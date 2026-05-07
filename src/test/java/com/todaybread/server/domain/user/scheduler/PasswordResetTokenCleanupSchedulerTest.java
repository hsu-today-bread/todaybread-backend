package com.todaybread.server.domain.user.scheduler;

import com.todaybread.server.domain.user.service.UserRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenCleanupSchedulerTest {

    @Mock
    private UserRecoveryService userRecoveryService;

    private PasswordResetTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PasswordResetTokenCleanupScheduler(userRecoveryService);
    }

    @Test
    void run_callsCleanupExpiredResetTokens() {
        scheduler.run();

        verify(userRecoveryService, times(1)).cleanupExpiredResetTokens();
    }
}
