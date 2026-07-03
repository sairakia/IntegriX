package kopo.integrix.service.impl;

import kopo.integrix.dto.CommonResponseDTO;
import kopo.integrix.dto.TokenResponseDTO;
import kopo.integrix.security.JwtTokenProvider;
import kopo.integrix.service.AuthTokenService;
import kopo.integrix.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    @Override
    public TokenResponseDTO issueTokenPair(String userId, Object user) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                refreshKey(refreshToken),
                userId,
                Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMillis())
        );

        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidityMillis() / 1000,
                jwtTokenProvider.getRefreshTokenValidityMillis() / 1000,
                user
        );
    }

    @Override
    public CommonResponseDTO refresh(String refreshToken, String refreshTokenCookie) {
        refreshToken = firstNotBlank(refreshToken, refreshTokenCookie);
        if (refreshToken == null || refreshToken.isBlank()) {
            return new CommonResponseDTO(false, "다시 로그인해주세요.", null);
        }

        String userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        if (userId == null) {
            return new CommonResponseDTO(false, "로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.", null);
        }

        String key = refreshKey(refreshToken);
        String savedUserId = redisTemplate.opsForValue().get(key);
        if (!userId.equals(savedUserId)) {
            return new CommonResponseDTO(false, "로그인 정보가 만료되었습니다. 다시 로그인해주세요.", null);
        }

        redisTemplate.delete(key);

        CommonResponseDTO currentUser = userService.getCurrentUser(userId);
        if (!currentUser.success()) {
            return currentUser;
        }

        return new CommonResponseDTO(true, "로그인 정보가 갱신되었습니다.", issueTokenPair(userId, currentUser.data()));
    }

    @Override
    public CommonResponseDTO logout(String accessTokenCookie, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(refreshKey(refreshToken));
        }

        String accessToken = resolveAccessToken(accessTokenCookie);
        if (accessToken != null) {
            long remainingMillis = jwtTokenProvider.getRemainingMillis(accessToken);
            if (remainingMillis > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + jwtTokenProvider.hashToken(accessToken),
                        "logout",
                        Duration.ofMillis(remainingMillis)
                );
            }
        }

        return new CommonResponseDTO(true, "로그아웃되었습니다.", null);
    }

    @Override
    public String getUserIdFromAccessTokenCookie(String accessTokenCookie) {
        String accessToken = resolveAccessToken(accessTokenCookie);
        if (accessToken == null || Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jwtTokenProvider.hashToken(accessToken)))) {
            return null;
        }

        return jwtTokenProvider.getUserIdFromAccessToken(accessToken);
    }

    private String refreshKey(String refreshToken) {
        return REFRESH_PREFIX + jwtTokenProvider.hashToken(refreshToken);
    }

    private String resolveAccessToken(String accessTokenCookie) {
        return accessTokenCookie != null && !accessTokenCookie.isBlank() ? accessTokenCookie : null;
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
