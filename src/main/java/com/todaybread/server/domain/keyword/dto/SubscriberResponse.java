package com.todaybread.server.domain.keyword.dto;

import com.todaybread.server.domain.user.entity.UserEntity;

/**
 * 키워드 구독 유저 응답 DTO입니다.
 * @param userId 유저 ID
 * @param email 이메일
 * @param nickname 닉네임
 */
public record SubscriberResponse(Long userId, String email, String nickname) {
    public static SubscriberResponse from(UserEntity entity) {
        return new SubscriberResponse(entity.getId(), entity.getEmail(), entity.getNickname());
    }
}
