package kopo.integrix.dto;


/**
 * Refresh Token 재발급 요청 DTO입니다. 토큰 재발급 과정에서 필요한 refresh token 값을 표현합니다.
 */
public record RefreshTokenRequestDTO(
        String refreshToken
) {
}
