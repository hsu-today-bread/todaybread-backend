package com.todaybread.server.domain.user.dto;

/**
 * 전화번호로 이메일 확인 시, 응답 DTO입니다.
 * @param maskedEmail
 */
public record UserFindEmailResponse (
        String maskedEmail
){}
