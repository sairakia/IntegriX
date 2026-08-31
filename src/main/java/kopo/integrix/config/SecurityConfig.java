package kopo.integrix.config;


/**
 * Spring Security의 핵심 설정 파일입니다. 공개 URL, 로그인 필요 URL, JWT 필터, CORS, CSRF, 세션 정책을 여기서 설정합니다.
 */
import kopo.integrix.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 프론트엔드 도메인에서 쿠키를 포함한 API 요청을 보낼 수 있도록 CORS 설정을 적용합니다.
                .cors(Customizer.withDefaults())
                // JWT 기반 REST API라 서버 세션의 CSRF 토큰을 사용하지 않고, SameSite/Secure 쿠키와 인증 필터로 보호합니다.
                .csrf(csrf -> csrf.disable())
                // 인증되지 않은 사용자를 익명 사용자로 처리하지 않고 명확히 401 응답을 내리기 위한 설정입니다.
                .anonymous(anonymous -> anonymous.disable())
                // 로그인 상태를 서버 세션에 저장하지 않고, 매 요청의 JWT로 인증 상태를 판단합니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인증이 필요한 URL에 토큰 없이 접근하면 기본 로그인 페이지 대신 401 상태코드를 반환합니다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // 브라우저의 CORS 사전 요청은 실제 API 호출 전에 통과되어야 하므로 전체 허용합니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 로그인 전에도 필요한 인증, 회원가입, 공개 분석, 업로드 파일 조회 API는 공개합니다.
                        .requestMatchers(
                                "/error",
                                "/uploads/**",
                                "/email/**",
                                "/user/login",
                                "/user/signup",
                                "/user/refresh",
                                "/user/logout",
                                "/user/find-id",
                                "/user/reset-password",
                                "/user/exists/**",
                                "/dashboard/summary",
                                "/api/dashboard/summary",
                                "/api/url/analyze",
                                "/api/text/analyze",
                                "/api/image/analyze",
                                "/api/report"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 위 공개 URL을 제외한 나머지 API는 JwtAuthenticationFilter에서 인증된 사용자만 접근할 수 있습니다.
                        .anyRequest().authenticated()
                )
                // Spring 기본 UsernamePasswordAuthenticationFilter보다 먼저 JWT를 확인해 SecurityContext를 채웁니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
