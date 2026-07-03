package kopo.integrix.service;


/**
 * 인증 토큰 기능의 Service 계약입니다. 로그인 후 토큰 발급, Refresh Token 재발급, 로그아웃 처리를 정의합니다.
 */
import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.TokenResponseDTO;

public interface AuthTokenService {

    TokenResponseDTO issueTokenPair(String userId, Object user);

    CommonResponseDTO refresh(String refreshToken, String refreshTokenCookie);

    CommonResponseDTO logout(String accessTokenCookie, String refreshToken);

    String getUserIdFromAccessTokenCookie(String accessTokenCookie);
}
