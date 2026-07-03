package kopo.integrix.service.impl;


/**
 * 사용자 관련 비즈니스 로직입니다. 회원가입, 로그인 검증, 사용자 조회, 프로필 이미지 저장, 비밀번호 변경, 회원탈퇴를 처리합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.LoginRequestDTO;
import kopo.integrix.dto.PasswordResetRequestDTO;
import kopo.integrix.dto.SignupRequestDTO;
import kopo.integrix.dto.UserResponseDTO;
import kopo.integrix.entity.UserInfoEntity;
import kopo.integrix.repository.UserInfoRepository;
import kopo.integrix.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String TYPE_FIND_ID = "FIND_ID";
    private static final String TYPE_RESET_PASSWORD = "RESET_PASSWORD";
    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${upload.profile-dir}")
    private String profileUploadDir;

    @Value("${upload.profile-url-path}")
    private String profileUrlPath;

    @Override
    public CommonResponseDTO login(LoginRequestDTO pDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(pDTO.userId(), pDTO.password())
            );

            UserInfoEntity user = userInfoRepository.findByUserId(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

            return new CommonResponseDTO(true, "로그인되었습니다.", toUserResponse(user));
        } catch (DisabledException e) {
            return new CommonResponseDTO(false, "비활성화된 계정입니다.", null);
        } catch (AuthenticationException e) {
            return new CommonResponseDTO(false, "아이디 또는 비밀번호가 올바르지 않습니다.", null);
        }
    }

    @Override
    @Transactional
    public CommonResponseDTO signup(SignupRequestDTO pDTO) {
        // 탈퇴하지 않은 활성 계정 기준으로 아이디와 이메일 중복을 검사합니다.
        if (userInfoRepository.existsByUserIdAndStatus(pDTO.userId(), STATUS_ACTIVE)) {
            return new CommonResponseDTO(false, "이미 사용 중인 아이디입니다.", null);
        }

        if (userInfoRepository.existsByEmailAndStatus(pDTO.email(), STATUS_ACTIVE)) {
            return new CommonResponseDTO(false, "이미 사용 중인 이메일입니다.", null);
        }

        // 과거 soft-delete 데이터가 남아 있으면 재가입을 막지 않도록 정리합니다.
        Optional<UserInfoEntity> inactiveUser = userInfoRepository.findByUserId(pDTO.userId())
                .filter(user -> !STATUS_ACTIVE.equals(user.getStatus()));

        if (inactiveUser.isPresent()) {
            Optional<UserInfoEntity> emailOwner = userInfoRepository.findByEmail(pDTO.email());
            if (emailOwner.isPresent() && !emailOwner.get().getUserId().equals(pDTO.userId())) {
                return new CommonResponseDTO(false, "탈퇴한 이메일은 기존 아이디로만 다시 가입할 수 있습니다.", null);
            }

            UserInfoEntity user = inactiveUser.get();
            user.reactivate(pDTO.email(), passwordEncoder.encode(pDTO.password()), pDTO.name());
            userInfoRepository.save(user);
            return new CommonResponseDTO(true, "회원가입이 완료되었습니다.", toUserResponse(user));
        }

        Optional<UserInfoEntity> inactiveEmailOwner = userInfoRepository.findByEmail(pDTO.email())
                .filter(user -> !STATUS_ACTIVE.equals(user.getStatus()));
        if (inactiveEmailOwner.isPresent()) {
            return new CommonResponseDTO(false, "탈퇴한 이메일은 기존 아이디로만 다시 가입할 수 있습니다.", null);
        }

        // 비밀번호는 저장 전에 BCrypt로 단방향 암호화합니다.
        UserInfoEntity pEntity = UserInfoEntity.createUser(
                pDTO.userId(),
                pDTO.email(),
                passwordEncoder.encode(pDTO.password()),
                pDTO.name()
        );

        userInfoRepository.save(pEntity);
        return new CommonResponseDTO(true, "회원가입이 완료되었습니다.", toUserResponse(pEntity));
    }

    @Override
    public CommonResponseDTO getCurrentUser(String userId) {
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "로그인이 필요합니다.", null);
        }

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserId(userId);
        if (rEntity.isEmpty()) {
            return new CommonResponseDTO(false, "사용자 정보를 찾을 수 없습니다.", null);
        }

        UserInfoEntity user = rEntity.get();
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            return new CommonResponseDTO(false, "비활성화된 계정입니다.", null);
        }

        return new CommonResponseDTO(true, "로그인 상태입니다.", toUserResponse(user));
    }

    @Override
    public CommonResponseDTO updateProfileImage(String userId, MultipartFile profileImage) {
        // 프로필 이미지는 로그인 사용자만 변경할 수 있으므로 인증된 userId를 먼저 확인합니다.
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "로그인이 필요합니다.", null);
        }

        // 업로드 파일 크기와 MIME 타입을 제한해 잘못된 파일 저장을 막습니다.
        if (profileImage == null || profileImage.isEmpty()) {
            return new CommonResponseDTO(false, "업로드할 프로필 이미지를 선택해 주세요.", null);
        }

        if (profileImage.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            return new CommonResponseDTO(false, "프로필 이미지는 5MB 이하만 업로드할 수 있습니다.", null);
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return new CommonResponseDTO(false, "JPG, PNG, GIF, WEBP 이미지만 업로드할 수 있습니다.", null);
        }

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserId(userId);
        if (rEntity.isEmpty()) {
            return new CommonResponseDTO(false, "사용자 정보를 찾을 수 없습니다.", null);
        }

        UserInfoEntity user = rEntity.get();
        Path uploadDir = Path.of(profileUploadDir).toAbsolutePath().normalize();

        try {
            // 배포 서버의 외부 업로드 디렉터리가 없으면 생성합니다.
            Files.createDirectories(uploadDir);

            String extension = extensionFromContentType(contentType);
            String fileName = userId + "_" + UUID.randomUUID() + "." + extension;
            Path targetPath = uploadDir.resolve(fileName).normalize();

            // 파일명 조작으로 업로드 디렉터리 밖에 저장되는 path traversal을 방지합니다.
            if (!targetPath.startsWith(uploadDir)) {
                return new CommonResponseDTO(false, "잘못된 파일 경로입니다.", null);
            }

            Files.copy(profileImage.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            // 새 이미지 저장 후 이전 프로필 이미지를 삭제해 서버 디스크에 불필요한 파일이 쌓이지 않게 합니다.
            deletePreviousProfileImage(user.getProfileImageUrl(), uploadDir);

            String imageUrl = normalizeUrlPath(profileUrlPath) + "/" + fileName;
            user.changeProfileImageUrl(imageUrl);
            userInfoRepository.save(user);

            return new CommonResponseDTO(true, "프로필 이미지가 변경되었습니다.", toUserResponse(user));
        } catch (IOException e) {
            return new CommonResponseDTO(false, "프로필 이미지 저장 중 오류가 발생했습니다.", null);
        }
    }

    @Override
    public CommonResponseDTO changePassword(String userId, String currentPassword, String newPassword) {
        // 비밀번호 변경은 현재 비밀번호 확인 후 새 비밀번호를 저장하는 민감 작업입니다.
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "로그인이 필요합니다.", null);
        }

        if (isBlank(currentPassword) || isBlank(newPassword)) {
            return new CommonResponseDTO(false, "현재 비밀번호와 새 비밀번호를 입력해 주세요.", null);
        }

        if (newPassword.length() < 6) {
            return new CommonResponseDTO(false, "비밀번호는 최소 6자 이상이어야 합니다.", null);
        }

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserId(userId);
        if (rEntity.isEmpty()) {
            return new CommonResponseDTO(false, "사용자 정보를 찾을 수 없습니다.", null);
        }

        // DB의 BCrypt 해시와 입력한 현재 비밀번호를 비교합니다.
        UserInfoEntity user = rEntity.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return new CommonResponseDTO(false, "현재 비밀번호가 일치하지 않습니다.", null);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);
        return new CommonResponseDTO(true, "비밀번호가 변경되었습니다.", null);
    }

    @Override
    @Transactional
    public CommonResponseDTO deleteAccount(String userId, String password) {
        // 회원탈퇴는 되돌리기 어려우므로 비밀번호를 다시 확인합니다.
        if (isBlank(userId)) {
            return new CommonResponseDTO(false, "로그인이 필요합니다.", null);
        }

        if (isBlank(password)) {
            return new CommonResponseDTO(false, "비밀번호를 입력해 주세요.", null);
        }

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserId(userId);
        if (rEntity.isEmpty()) {
            return new CommonResponseDTO(false, "사용자 정보를 찾을 수 없습니다.", null);
        }

        UserInfoEntity user = rEntity.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return new CommonResponseDTO(false, "비밀번호가 일치하지 않습니다.", null);
        }

        // 계정 row를 실제 삭제하기 전에 서버에 저장된 프로필 이미지 파일도 정리합니다.
        deleteProfileImageQuietly(user);
        user.changeProfileImageUrl(null);
        user.deactivate();
        userInfoRepository.save(user);
        return new CommonResponseDTO(true, "회원 탈퇴가 완료되었습니다.", null);
    }

    @Override
    public CommonResponseDTO signupAfterEmailVerified(SignupRequestDTO pDTO, Boolean emailVerified, String authEmail) {
        // 이메일 인증이 끝난 주소와 회원가입 요청 이메일이 같을 때만 가입을 진행합니다.
        if (!Boolean.TRUE.equals(emailVerified)) {
            return new CommonResponseDTO(false, "이메일 인증이 필요합니다.", null);
        }

        if (authEmail == null || !authEmail.equals(pDTO.email())) {
            return new CommonResponseDTO(false, "인증한 이메일과 입력한 이메일이 일치하지 않습니다.", null);
        }

        return signup(pDTO);
    }

    @Override
    public CommonResponseDTO findIdAfterEmailVerified(Boolean emailVerified, String authEmail, String authType) {
        if (!Boolean.TRUE.equals(emailVerified)) {
            return new CommonResponseDTO(false, "이메일 인증이 필요합니다.", null);
        }

        if (!TYPE_FIND_ID.equals(authType)) {
            return new CommonResponseDTO(false, "아이디 찾기용 이메일 인증이 필요합니다.", null);
        }

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByEmail(authEmail);
        if (rEntity.isEmpty() || !STATUS_ACTIVE.equals(rEntity.get().getStatus())) {
            return new CommonResponseDTO(false, "해당 이메일로 가입된 계정이 없습니다.", null);
        }

        return new CommonResponseDTO(true, "아이디를 찾았습니다.", rEntity.get().getUserId());
    }

    @Override
    public CommonResponseDTO resetPasswordAfterEmailVerified(
            PasswordResetRequestDTO request,
            Boolean emailVerified,
            String authEmail,
            String authType
    ) {
        if (!Boolean.TRUE.equals(emailVerified)) {
            return new CommonResponseDTO(false, "이메일 인증이 필요합니다.", null);
        }

        if (!TYPE_RESET_PASSWORD.equals(authType)) {
            return new CommonResponseDTO(false, "비밀번호 재설정용 이메일 인증이 필요합니다.", null);
        }

        if (authEmail == null || !authEmail.equals(request.email())) {
            return new CommonResponseDTO(false, "인증한 이메일과 입력한 이메일이 일치하지 않습니다.", null);
        }

        try {
            updatePassword(request.userId(), request.email(), request.password());
            return new CommonResponseDTO(true, "비밀번호가 변경되었습니다.", null);
        } catch (Exception e) {
            return new CommonResponseDTO(false, e.getMessage(), null);
        }
    }

    @Override
    public boolean existsUserId(String userId) {
        return userInfoRepository.existsByUserIdAndStatus(userId, STATUS_ACTIVE);
    }

    @Override
    public boolean existsEmail(String email) {
        return userInfoRepository.existsByEmailAndStatus(email, STATUS_ACTIVE);
    }

    @Override
    public Optional<UserInfoEntity> findByEmail(String email) {
        return userInfoRepository.findByEmail(email);
    }

    @Override
    public void updatePassword(String userId, String email, String password) {
        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserId(userId);
        if (rEntity.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        UserInfoEntity user = rEntity.get();
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new RuntimeException("비활성화된 계정입니다.");
        }

        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("아이디와 이메일이 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(password));
        userInfoRepository.save(user);
    }

    @Override
    public Optional<UserInfoEntity> findByUserId(String userId) {
        return userInfoRepository.findByUserId(userId);
    }

    private UserResponseDTO toUserResponse(UserInfoEntity user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void deleteInactiveDuplicateIfExists(String userId, String email) {
        // 이전 버전에서 비활성 상태로 남아 있던 동일 계정을 정리해 재가입을 허용합니다.
        userInfoRepository.findByUserId(userId)
                .filter(user -> !STATUS_ACTIVE.equals(user.getStatus()));

        userInfoRepository.findByEmail(email)
                .filter(user -> !STATUS_ACTIVE.equals(user.getStatus()))
                .filter(user -> !user.getUserId().equals(userId));

        userInfoRepository.flush();
    }

    private void deleteProfileImageQuietly(UserInfoEntity user) {
        try {
            deletePreviousProfileImage(user.getProfileImageUrl(), Path.of(profileUploadDir).toAbsolutePath().normalize());
        } catch (IOException ignored) {
            // Account deletion should not be blocked by an already missing profile image file.
        }
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "img";
        };
    }

    private String normalizeUrlPath(String value) {
        String path = value == null || value.isBlank() ? "/uploads/profile" : value.trim();
        if (path.endsWith("/**")) {
            path = path.substring(0, path.length() - 3);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private void deletePreviousProfileImage(String previousImageUrl, Path uploadDir) throws IOException {
        String publicPath = normalizeUrlPath(profileUrlPath);
        if (previousImageUrl == null || !previousImageUrl.startsWith(publicPath + "/")) {
            return;
        }

        String previousFileName = previousImageUrl.substring((publicPath + "/").length());
        Path previousPath = uploadDir.resolve(previousFileName).normalize();
        if (previousPath.startsWith(uploadDir)) {
            Files.deleteIfExists(previousPath);
        }
    }
}
