package com.todaybread.server.domain.notification.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * notification_log 테이블 엔티티를 정의합니다.
 * 알림 발송 이력을 저장하며, (user_id, type, event_key) 유니크 제약으로 중복 발송을 방지합니다.
 */
@Entity
@Table(name = "notification_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_log_dedup",
                columnNames = {"user_id", "type", "event_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Builder
    private NotificationLogEntity(Long userId, NotificationType type, String targetId,
                                  String eventKey, String title, String body) {
        this.userId = userId;
        this.type = type;
        this.targetId = targetId;
        this.eventKey = eventKey;
        this.title = title;
        this.body = body;
    }
}
