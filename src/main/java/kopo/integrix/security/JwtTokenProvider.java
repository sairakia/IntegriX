package kopo.integrix.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity-millis}")
    @Getter
    private long accessTokenValidityMillis;

    @Value("${jwt.refresh-token-validity-millis}")
    @Getter
    private long refreshTokenValidityMillis;

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;

    @PostConstruct
    void init() {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(JWT_ALGORITHM)
                .build();
    }

    public String createAccessToken(String userId) {
        return createToken(userId, ACCESS, accessTokenValidityMillis);
    }

    public String createRefreshToken(String userId) {
        return createToken(userId, REFRESH, refreshTokenValidityMillis);
    }

    public String getUserIdFromAccessToken(String token) {
        return getUserId(token, ACCESS);
    }

    public String getUserIdFromRefreshToken(String token) {
        return getUserId(token, REFRESH);
    }

    public long getRemainingMillis(String token) {
        Jwt jwt = decode(token);
        if (jwt == null || jwt.getExpiresAt() == null) {
            return 0;
        }

        return Math.max(0, jwt.getExpiresAt().toEpochMilli() - Instant.now().toEpochMilli());
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return encode(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시에 실패했습니다.", e);
        }
    }

    private String createToken(String userId, String type, long validityMillis) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(JWT_ALGORITHM).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("type", type)
                .issuedAt(now)
                .expiresAt(now.plusMillis(validityMillis))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String getUserId(String token, String expectedType) {
        Jwt jwt = decode(token);
        if (jwt == null || !expectedType.equals(jwt.getClaimAsString("type"))) {
            return null;
        }

        String userId = jwt.getSubject();
        return userId != null && !userId.isBlank() ? userId : null;
    }

    private Jwt decode(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
