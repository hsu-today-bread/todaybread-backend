package com.todaybread.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * EC2 프로필에서 S3 클라이언트를 생성하는 설정입니다.
 * AWS SDK default credentials provider chain을 사용하므로 EC2 IAM Role을 우선 사용합니다.
 */
@Configuration
@Profile("ec2")
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
