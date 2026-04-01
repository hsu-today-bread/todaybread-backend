package com.todaybread.server.config.jwt;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 토큰을 위한 클래스입니다.
 * 스프링 공식 디코더를 사용하기 때문에, 토큰 발급을 담당하는 클래스입니다.
 * HmacSHA256 알고리즘을 사용합니다.
 */
@Component
public class JwtTokenService {

    @Getter
    private final SecretKeySpec secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * application.properties 혹은 env 파일을 참고해서 각 변수를 주입합니다.
     * @Value 어노테이션을 활용해서 값을 읽어옵니다.
     *
     * @param secret JWT 토큰 서명용 비밀 키
     * @param accessTokenExpiration JWT access token의 만료 시간
     * @param refreshTokenExpiration JWT refresh token의 만료 시간
     */
    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        if (secret == null || secret.isBlank() || secret.startsWith("my-super-secret")) {
            throw new IllegalStateException("JWT 시크릿이 설정되지 않았거나 기본값을 사용 중입니다. "
                    + "환경변수 JWT_SECRET을 반드시 설정하세요.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Access Token을 발급합니다. 유저 아이디, 이메일, 역할을 통해서 확인합니다.
     * 현재 시각을 토큰에 기록합니다.
     *
     * @param userId 유저 ID
     * @param email 유저 이메일
     * @param role 유저 역할
     * @return access token
     */
    public String generateAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(accessTokenExpiration)))
                .build();
        return signToken(claims);
    }

    /**
     * Refresh Token을 발급합니다. 유저 아이디를 통해서 발급 가능합니다.
     * 현재 시각을 토큰에 기록합니다.
     * 
     * @param userId 유저 ID
     * @return refresh token
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(refreshTokenExpiration)))
                .build();
        return signToken(claims);
    }

    /**
     * 토큰을 받아서 JWT 문자열로 바꿉니다. 헬퍼 메서드입니다.
     * 
     * @param claims 인증 요청용 토큰
     * @return JWT 문자열
     */
    private String signToken(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("JWT 토큰 서명 실패", e);
        }
    }
}
