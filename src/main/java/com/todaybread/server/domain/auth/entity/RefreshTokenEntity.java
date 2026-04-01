package com.todaybread.server.domain.auth.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * refresh 토큰을 위한 엔티티 객체입니다.
 * 관계를 설정하지 않고 그냥 유저 키만 넣습니다.
 * 유저 한명당 한개의 리프레쉬 토큰을 가지기 떄문입니다.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 빌더 메서드입니다.
     *
     * @param userId 유저 ID
     * @param token refresh 토큰
     * @param expiresAt 만료일
     */
    @Builder
    private RefreshTokenEntity(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    /**
     * 리프레쉬 토큰을 갱신합니다.
     *
     * @param token     새 토큰 값
     * @param expiresAt 새 만료일
     */
    public void renew(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
