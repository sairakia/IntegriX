package kopo.integrix.security;


/**
 * 요청마다 JWT를 검사하는 Security Filter입니다. accessToken 쿠키를 검증한 뒤 SecurityContext에 인증 정보를 저장합니다.
 */
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.integrix.service.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private final AuthTokenService authTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 이미 앞단에서 인증 정보가 만들어졌다면 중복 검증하지 않고 다음 필터로 넘깁니다.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = authTokenService.getUserIdFromAccessTokenCookie(getCookieValue(request, ACCESS_TOKEN_COOKIE));

            if (userId != null && !userId.isBlank()) {
                // 토큰 검증이 끝난 사용자 ID를 Spring Security가 이해하는 인증 객체로 변환합니다.
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Controller의 @AuthenticationPrincipal String userId에서 이 값을 꺼내 쓸 수 있게 저장합니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 인증 성공 여부와 관계없이 다음 필터로 넘기고, 접근 허용 여부는 SecurityConfig 인가 규칙이 판단합니다.
        filterChain.doFilter(request, response);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        // HttpOnly 쿠키는 JavaScript에서는 못 읽지만, 서버 요청에서는 request cookie로 읽을 수 있습니다.
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
