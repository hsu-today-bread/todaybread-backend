package com.todaybread.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaybread.server.config.jwt.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.exception.ErrorResponse;
import org.springframework.security.core.AuthenticationException;

/**
 * Spring Security 관련 빈을 등록하는 설정 클래스입니다.
 * 비밀번호 암호화에는 Argon2를 사용하고,
 * JWT 인증에는 Spring Security Resource Server와 Nimbus 디코더를 사용합니다.
 */
@Configuration
public class SecurityConfig {

    /**
     * 사용자 비밀번호 인코딩을 실시합니다.
     * @return 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    /**
     * JWT 서명 검증에 사용할 디코더를 등록합니다.
     * 로그인 시 발급한 토큰과 동일한 secret key를 사용해,
     * 요청으로 들어온 Bearer 토큰의 서명과 만료 여부를 검증합니다.
     *
     * @param jwtTokenService JWT 발급에 사용하는 secret key를 제공하는 서비스
     * @return HMAC secret key 기반의 Nimbus JWT 디코더
     */
    @Bean
    public JwtDecoder jwtDecoder(JwtTokenService jwtTokenService) {
        return NimbusJwtDecoder.withSecretKey(jwtTokenService.getSecretKey()).build();
    }

    /**
     * 애플리케이션의 HTTP 보안 정책을 정의합니다.
     * 세션을 사용하지 않는 stateless 구조로 설정하고,
     * 회원가입/로그인/헬스체크/Swagger 경로는 인증 없이 허용합니다.
     * 그 외의 요청은 JWT 기반 인증이 필요하도록 구성합니다.
     *
     * @param http Spring Security HTTP 설정 객체
     * @param jwtDecoder 요청으로 들어온 JWT를 검증할 디코더
     * @return 구성된 SecurityFilterChain
     * @throws Exception SecurityFilterChain 구성 중 예외가 발생할 경우
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/user/register",
                                "/api/user/login",
                                "/api/user/exist/**",
                                "/api/auth/reissue",
                                "/api/system/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                )
                .build();
    }

    /**
     * JWT 인증 실패 시 공통 에러 응답을 반환하는 엔트리 포인트를 등록합니다.
     * 유효하지 않거나 만료된 토큰에 대해 프로젝트 표준 ErrorResponse 형식으로 응답합니다.
     *
     * @return JWT 인증 실패를 처리하는 AuthenticationEntryPoint
     */
    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            ErrorCode errorCode = resolveErrorCode(authException);
            response.setStatus(errorCode.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorResponse errorResponse = ErrorResponse.errorFrom(errorCode);
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    /**
     * 인증 예외의 내용을 바탕으로 적절한 에러 코드를 결정합니다.
     * 현재는 예외 메시지에 expired가 포함되어 있으면 만료 토큰으로 간주하고,
     * 그 외의 경우는 잘못된 토큰으로 처리합니다.
     *
     * @param authException JWT 인증 과정에서 발생한 예외
     * @return 프로젝트 공통 에러 코드
     */
    private ErrorCode resolveErrorCode(AuthenticationException authException) {
        if (authException.getMessage() != null
                && authException.getMessage().contains("expired")) {
            return ErrorCode.AUTH_ACCESS_TOKEN_EXPIRED;
        }
        return ErrorCode.AUTH_ACCESS_TOKEN_INVALID;
    }
}
