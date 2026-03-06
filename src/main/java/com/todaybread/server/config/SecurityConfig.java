package com.todaybread.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 암호화 관련 설정을 담당하는 클래스입니다.
 */
@Configuration
public class SecurityConfig {

    /**
     * 내용: 비밀번호 암호화를 위한 인코더 빈을 등록합니다.
     *
     * @return BCryptPasswordEncoder 객체
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}