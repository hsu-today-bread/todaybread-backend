package com.todaybread.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 스웨거 설정을 위한 config 클래스입니다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 스웨거 문서 설정을 생성합니다.
     * JWT Bearer 인증 스키마를 포함합니다.
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("TodayBread API")
                        .version("v1.0")
                        .description("TodayBread 백엔드 API 문서"));
    }
}
