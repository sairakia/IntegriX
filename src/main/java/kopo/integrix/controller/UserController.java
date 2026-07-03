package kopo.integrix.controller;


/**
 * 회원 기능을 담당하는 Controller입니다. 회원가입, 로그인, 로그아웃, 토큰 재발급, 내 정보 조회, 비밀번호 변경, 회원탈퇴 요청을 처리합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.ChangePasswordRequestDTO;
import kopo.integrix.dto.DeleteAccountRequestDTO;
import kopo.integrix.dto.FindIdRequestDTO;
import kopo.integrix.dto.LoginRequestDTO;
import kopo.integrix.dto.PasswordResetRequestDTO;
import kopo.integrix.dto.RefreshTokenRequestDTO;
import kopo.integrix.dto.SignupRequestDTO;
import kopo.integrix.dto.TokenResponseDTO;
import kopo.integrix.service.AuthTokenService;
import kopo.integrix.service.EmailAuthService;
import kopo.integrix.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String TYPE_SIGNUP = "SIGNUP";
    private static final String TYPE_FIND_ID = "FIND_ID";
    private static final String TYPE_RESET_PASSWORD = "RESET_PASSWORD";

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final EmailAuthService emailAuthService;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${auth.cookie.domain:}")
    private String cookieDomain;

    @PostMapping("/login")
    public CommonResponseDTO login(@RequestBody LoginRequestDTO pDTO, HttpServletResponse response) {
        // 아이디와 비밀번호 검증은 Service에서 처리하고, Controller는 HTTP 흐름만 담당합니다.
        CommonResponseDTO result = userService.login(pDTO);

        if (!result.success()) {
            return result;
        }

        // 로그인 성공 후 JWT 토큰 쌍을 만들고 HttpOnly 쿠키로 내려보냅니다.
        TokenResponseDTO tokenResponse = authTokenService.issueTokenPair(pDTO.userId(), result.data());
        addTokenCookies(response, tokenResponse);

        return new CommonResponseDTO(
                true,
                result.message(),
                tokenResponse
        );
    }

    @PostMapping("/refresh")
    public CommonResponseDTO refresh(@RequestBody(required = false) RefreshTokenRequestDTO request,
                                     @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                     HttpServletResponse response) {
        // Refresh Token은 body 또는 쿠키에서 받을 수 있고, 성공하면 새 토큰 쿠키로 교체합니다.
        CommonResponseDTO result = authTokenService.refresh(
                request != null ? request.refreshToken() : null,
                refreshTokenCookie
        );
        if (result.success() && result.data() instanceof TokenResponseDTO tokenResponse) {
            addTokenCookies(response, tokenResponse);
        }
        return result;
    }

    @PostMapping("/logout")
    public CommonResponseDTO logout(@CookieValue(value = ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
                                    @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                    @RequestBody(required = false) RefreshTokenRequestDTO request,
                                    HttpServletResponse response) {
        // 서버의 Redis 토큰 상태를 먼저 정리한 뒤 브라우저 쿠키를 만료시킵니다.
        CommonResponseDTO result = authTokenService.logout(
                accessTokenCookie,
                firstNotBlank(request != null ? request.refreshToken() : null, refreshTokenCookie)
        );
        clearTokenCookies(response);
        return result;
    }

    @GetMapping("/me")
    public CommonResponseDTO me(@AuthenticationPrincipal String userId) {
        // JwtAuthenticationFilter가 SecurityContext에 넣은 userId를 현재 로그인 사용자로 사용합니다.
        return userService.getCurrentUser(userId);
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponseDTO updateProfileImage(@AuthenticationPrincipal String userId,
                                                @RequestParam("profileImage") MultipartFile profileImage) {
        return userService.updateProfileImage(userId, profileImage);
    }

    @PostMapping("/change-password")
    public CommonResponseDTO changePassword(@AuthenticationPrincipal String userId,
                                            @RequestBody ChangePasswordRequestDTO request) {
        if (request == null) {
            return new CommonResponseDTO(false, "비밀번호 정보를 입력해 주세요.", null);
        }
        return userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }

    @PostMapping("/delete-account")
    public CommonResponseDTO deleteAccount(@AuthenticationPrincipal String userId,
                                           @RequestBody DeleteAccountRequestDTO request,
                                           @CookieValue(value = ACCESS_TOKEN_COOKIE, required = false) String accessTokenCookie,
                                           @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                           HttpServletResponse response) {
        if (request == null) {
            return new CommonResponseDTO(false, "비밀번호를 입력해 주세요.", null);
        }
        // 회원탈퇴가 성공한 경우 남아 있는 토큰과 쿠키도 함께 제거해 즉시 로그아웃 상태로 만듭니다.
        CommonResponseDTO result = userService.deleteAccount(userId, request.password());
        if (result.success()) {
            authTokenService.logout(accessTokenCookie, refreshTokenCookie);
            clearTokenCookies(response);
        }
        return result;
    }

    @PostMapping("/signup")
    public CommonResponseDTO signup(@RequestBody SignupRequestDTO pDTO, HttpServletResponse response) {
        // 회원가입은 먼저 이메일 인증 완료 여부를 확인한 뒤 실제 계정을 생성합니다.
        boolean emailVerified = emailAuthService.hasVerifiedEmail(pDTO.email(), TYPE_SIGNUP);
        CommonResponseDTO result = userService.signupAfterEmailVerified(pDTO, emailVerified, pDTO.email());

        if (!result.success()) {
            return result;
        }

        emailAuthService.consumeVerifiedEmail(pDTO.email(), TYPE_SIGNUP);

        // 가입 직후에도 바로 로그인 상태가 되도록 토큰을 발급하고 쿠키에 저장합니다.
        TokenResponseDTO tokenResponse = authTokenService.issueTokenPair(pDTO.userId(), result.data());
        addTokenCookies(response, tokenResponse);

        return new CommonResponseDTO(
                true,
                result.message(),
                tokenResponse
        );
    }

    @PostMapping("/find-id")
    public CommonResponseDTO findId(@RequestBody FindIdRequestDTO request) {
        boolean emailVerified = emailAuthService.hasVerifiedEmail(request.email(), TYPE_FIND_ID);
        CommonResponseDTO result = userService.findIdAfterEmailVerified(emailVerified, request.email(), TYPE_FIND_ID);

        if (result.success()) {
            emailAuthService.consumeVerifiedEmail(request.email(), TYPE_FIND_ID);
        }

        return result;
    }

    @PostMapping("/reset-password")
    public CommonResponseDTO resetPassword(@RequestBody PasswordResetRequestDTO request) {
        boolean emailVerified = emailAuthService.hasVerifiedEmail(request.email(), TYPE_RESET_PASSWORD);
        CommonResponseDTO result = userService.resetPasswordAfterEmailVerified(
                request,
                emailVerified,
                request.email(),
                TYPE_RESET_PASSWORD
        );

        if (result.success()) {
            emailAuthService.consumeVerifiedEmail(request.email(), TYPE_RESET_PASSWORD);
        }

        return result;
    }

    @GetMapping("/exists/userId")
    public boolean existsUserId(@RequestParam String userId) {
        return userService.existsUserId(userId);
    }

    @GetMapping("/exists/email")
    public boolean existsEmail(@RequestParam String email) {
        return userService.existsEmail(email);
    }

    private void addTokenCookies(HttpServletResponse response, TokenResponseDTO tokenResponse) {
        // Access Token과 Refresh Token은 둘 다 HttpOnly 쿠키로 내려 JavaScript 접근을 막습니다.
        addCookie(response, ACCESS_TOKEN_COOKIE, tokenResponse.accessToken(), tokenResponse.accessTokenExpiresIn());
        addCookie(response, REFRESH_TOKEN_COOKIE, tokenResponse.refreshToken(), tokenResponse.refreshTokenExpiresIn());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        // maxAge 0 쿠키를 다시 내려 브라우저에 저장된 토큰 쿠키를 삭제합니다.
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        // Secure, SameSite, Domain 값은 로컬/배포 환경이 다르므로 application.yml 또는 env로 제어합니다.
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
