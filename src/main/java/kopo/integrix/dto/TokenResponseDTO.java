package kopo.integrix.dto;


/**
 * 토큰 발급 응답 DTO입니다. 로그인 또는 재발급 결과로 내려줄 Access Token과 Refresh Token 값을 표현합니다.
 */
public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        Object user
) {
}
