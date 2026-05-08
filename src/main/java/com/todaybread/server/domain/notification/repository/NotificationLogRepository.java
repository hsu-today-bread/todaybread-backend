package com.todaybread.server.domain.notification.repository;

import com.todaybread.server.domain.notification.entity.NotificationLogEntity;
import com.todaybread.server.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 알림 발송 이력 리포지터리입니다.
 * (user_id, type, event_key) 조합으로 중복 발송 여부를 확인합니다.
 */
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {

    /**
     * 특정 유저에게 동일 타입·이벤트키 조합의 알림이 이미 발송되었는지 확인합니다.
     *
     * @param userId   유저 ID
     * @param type     알림 유형
     * @param eventKey 이벤트 키
     * @return 이미 존재하면 true
     */
    boolean existsByUserIdAndTypeAndEventKey(Long userId, NotificationType type, String eventKey);
}
