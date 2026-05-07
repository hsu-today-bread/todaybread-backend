package com.todaybread.server.domain.user.scheduler;

import com.todaybread.server.domain.user.service.UserRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 비밀번호 재설정 토큰을 주기적으로 삭제하는 스케줄러입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "user.recovery.cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class PasswordResetTokenCleanupScheduler {

    private final UserRecoveryService userRecoveryService;

    @Scheduled(fixedDelayString = "${user.recovery.cleanup-interval-ms:3600000}")
    public void run() {
        int deletedCount = userRecoveryService.cleanupExpiredResetTokens();
        if (deletedCount > 0) {
            log.info("만료된 비밀번호 재설정 토큰 정리 완료: deletedCount={}", deletedCount);
        }
    }
}
