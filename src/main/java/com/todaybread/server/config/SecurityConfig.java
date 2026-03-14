package com.todaybread.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해쉬화 및 JWT 인증과 관련된 작업을 실시합니다.
 * 비밀번호 해쉬화는 Argon2 알고리즘을 사용합니다.
 * JWT 인증은 Spring boot security와 nimbus를 사용합니다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
