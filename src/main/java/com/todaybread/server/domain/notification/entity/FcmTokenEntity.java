package com.todaybread.server.domain.notification.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FCM 토큰 엔티티입니다.
 * 유저당 최대 1개의 활성 토큰을 관리합니다.
 */
@Entity
@Table(name = "fcm_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private Platform platform;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Builder
    private FcmTokenEntity(Long userId, String token, Platform platform, LocalDateTime lastSeenAt) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.active = true;
        this.lastSeenAt = lastSeenAt;
    }

    /**
     * 토큰을 갱신합니다.
     * 기존 토큰을 새 토큰으로 교체하고 활성 상태로 설정합니다.
     *
     * @param token 새 FCM 토큰
     * @param platform 플랫폼
     * @param now 현재 시각
     */
    public void updateToken(String token, Platform platform, LocalDateTime now) {
        this.token = token;
        this.platform = platform;
        this.active = true;
        this.lastSeenAt = now;
    }

    /**
     * 토큰을 비활성화합니다.
     * 로그아웃 또는 알림 끄기 시 호출됩니다.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 비활성화된 토큰을 다시 활성화합니다.
     * 새 토큰과 플랫폼 정보로 갱신하며 활성 상태로 전환합니다.
     *
     * @param token 새 FCM 토큰
     * @param platform 플랫폼
     * @param now 현재 시각
     */
    public void activate(String token, Platform platform, LocalDateTime now) {
        this.token = token;
        this.platform = platform;
        this.active = true;
        this.lastSeenAt = now;
    }
}
