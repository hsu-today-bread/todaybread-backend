package com.todaybread.server.domain.notification.service;

import com.todaybread.server.domain.notification.entity.FcmTokenEntity;
import com.todaybread.server.domain.notification.entity.Platform;
import com.todaybread.server.domain.notification.repository.FcmTokenRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FcmTokenService Property Tests (Properties 1, 2, 3)
 *
 * Property 1: 유저당 토큰 row 유일성 - repeated registerToken calls → exactly 1 row per user
 * Property 2: 토큰 충돌 해소 - token registered by user A, then user B registers same token → A's row deleted
 * Property 3: 활성화/비활성화 라운드트립 - register → deactivate → register → active=true with new token
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.4**
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FcmTokenServicePropertyTest {

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private FcmTokenService fcmTokenService;

    // ──────────────────────────────────────────────────────────────────────
    // Example-Based Unit Tests - Property 1: 유저당 토큰 row 유일성
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Property 1: 유저당 토큰 row 유일성")
    @Tag("Feature: fcm-push-notification, Property 1: 유저당 토큰 row 유일성")
    class Property1TokenUniquenessPerUser {

        /**
         * **Validates: Requirements 1.1, 1.3**
         * 첫 등록 시 정확히 1개의 row가 생성된다.
         */
        @Test
        @DisplayName("첫 등록 시 유저에 대해 정확히 1개의 row가 생성된다")
        void firstRegistration_createsExactlyOneRow() {
            fcmTokenService.registerToken(1L, "token-abc", Platform.ANDROID);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
            assertThat(allTokens.get(0).getUserId()).isEqualTo(1L);
            assertThat(allTokens.get(0).getToken()).isEqualTo("token-abc");
            assertThat(allTokens.get(0).getPlatform()).isEqualTo(Platform.ANDROID);
            assertThat(allTokens.get(0).getActive()).isTrue();
        }

        /**
         * **Validates: Requirements 1.2, 1.4**
         * 동일 유저가 다른 토큰으로 재등록하면 row 수는 1개를 유지하고 토큰이 갱신된다.
         */
        @Test
        @DisplayName("동일 유저가 다른 토큰으로 재등록하면 row 수는 1개를 유지한다")
        void sameUser_differentToken_updatesExistingRow() {
            fcmTokenService.registerToken(1L, "token-first", Platform.ANDROID);
            fcmTokenService.registerToken(1L, "token-second", Platform.IOS);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
            assertThat(allTokens.get(0).getToken()).isEqualTo("token-second");
            assertThat(allTokens.get(0).getPlatform()).isEqualTo(Platform.IOS);
            assertThat(allTokens.get(0).getActive()).isTrue();
        }

        /**
         * **Validates: Requirements 1.2**
         * 동일 유저가 같은 토큰으로 반복 등록해도 row 수는 1개를 유지한다.
         */
        @Test
        @DisplayName("동일 유저가 같은 토큰으로 반복 등록해도 row 수는 1개를 유지한다")
        void sameUser_sameToken_repeatedRegistration_maintainsOneRow() {
            fcmTokenService.registerToken(1L, "token-same", Platform.ANDROID);
            fcmTokenService.registerToken(1L, "token-same", Platform.ANDROID);
            fcmTokenService.registerToken(1L, "token-same", Platform.ANDROID);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
            assertThat(allTokens.get(0).getToken()).isEqualTo("token-same");
        }

        /**
         * **Validates: Requirements 1.2, 1.4**
         * 여러 번 토큰을 변경해도 항상 마지막 토큰이 유지된다.
         */
        @Test
        @DisplayName("여러 번 토큰을 변경해도 항상 마지막 토큰이 유지된다")
        void multipleTokenChanges_alwaysKeepsLatestToken() {
            fcmTokenService.registerToken(1L, "token-1", Platform.ANDROID);
            fcmTokenService.registerToken(1L, "token-2", Platform.IOS);
            fcmTokenService.registerToken(1L, "token-3", Platform.ANDROID);
            fcmTokenService.registerToken(1L, "token-4", Platform.IOS);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
            assertThat(allTokens.get(0).getToken()).isEqualTo("token-4");
            assertThat(allTokens.get(0).getPlatform()).isEqualTo(Platform.IOS);
            assertThat(allTokens.get(0).getActive()).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Example-Based Unit Tests - Property 2: 토큰 충돌 해소
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Property 2: 토큰 충돌 해소")
    @Tag("Feature: fcm-push-notification, Property 2: 토큰 충돌 해소")
    class Property2TokenConflictResolution {

        /**
         * **Validates: Requirements 1.5**
         * 유저 A가 등록한 토큰을 유저 B가 등록하면 A의 row가 삭제된다.
         */
        @Test
        @DisplayName("유저 A의 토큰을 유저 B가 등록하면 A의 row가 삭제된다")
        void tokenConflict_deletesOriginalUserRow() {
            fcmTokenService.registerToken(1L, "shared-token", Platform.ANDROID);
            fcmTokenService.registerToken(2L, "shared-token", Platform.IOS);

            // 유저 A의 row는 삭제됨
            Optional<FcmTokenEntity> userAToken = fcmTokenRepository.findByUserId(1L);
            assertThat(userAToken).isEmpty();

            // 유저 B가 해당 토큰을 소유
            Optional<FcmTokenEntity> userBToken = fcmTokenRepository.findByUserId(2L);
            assertThat(userBToken).isPresent();
            assertThat(userBToken.get().getToken()).isEqualTo("shared-token");
            assertThat(userBToken.get().getPlatform()).isEqualTo(Platform.IOS);
        }

        /**
         * **Validates: Requirements 1.5**
         * 유저 B가 이미 다른 토큰을 가진 상태에서 유저 A의 토큰으로 등록하면
         * A의 row가 삭제되고 B의 row가 갱신된다.
         */
        @Test
        @DisplayName("유저 B가 기존 토큰이 있는 상태에서 유저 A의 토큰으로 등록하면 A의 row 삭제 후 B의 row 갱신")
        void tokenConflict_withExistingUserBRow_deletesAAndUpdatesB() {
            fcmTokenService.registerToken(1L, "token-A", Platform.ANDROID);
            fcmTokenService.registerToken(2L, "token-B", Platform.IOS);

            // 유저 B가 유저 A의 토큰으로 등록
            fcmTokenService.registerToken(2L, "token-A", Platform.ANDROID);

            // 유저 A의 row는 삭제됨
            Optional<FcmTokenEntity> userAToken = fcmTokenRepository.findByUserId(1L);
            assertThat(userAToken).isEmpty();

            // 유저 B의 row는 token-A로 갱신됨
            Optional<FcmTokenEntity> userBToken = fcmTokenRepository.findByUserId(2L);
            assertThat(userBToken).isPresent();
            assertThat(userBToken.get().getToken()).isEqualTo("token-A");

            // 전체 row 수는 1개
            assertThat(fcmTokenRepository.findAll()).hasSize(1);
        }

        /**
         * **Validates: Requirements 1.5**
         * 충돌 해소 후 유저 B의 row는 정확히 1개만 존재한다.
         */
        @Test
        @DisplayName("충돌 해소 후 유저 B의 row는 정확히 1개만 존재한다")
        void afterConflictResolution_userBHasExactlyOneRow() {
            fcmTokenService.registerToken(1L, "conflict-token", Platform.ANDROID);
            fcmTokenService.registerToken(2L, "conflict-token", Platform.IOS);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
            assertThat(allTokens.get(0).getUserId()).isEqualTo(2L);
            assertThat(allTokens.get(0).getToken()).isEqualTo("conflict-token");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Example-Based Unit Tests - Property 3: 활성화/비활성화 라운드트립
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Property 3: 활성화/비활성화 라운드트립")
    @Tag("Feature: fcm-push-notification, Property 3: 활성화/비활성화 라운드트립")
    class Property3ActivationRoundTrip {

        /**
         * **Validates: Requirements 2.1, 2.4**
         * register → deactivate → register 시퀀스 후 active=true이고 새 토큰이 저장된다.
         */
        @Test
        @DisplayName("register → deactivate → register 후 active=true이고 새 토큰이 저장된다")
        void registerDeactivateRegister_resultsInActiveWithNewToken() {
            fcmTokenService.registerToken(1L, "token-old", Platform.ANDROID);
            fcmTokenService.deactivateToken(1L);

            // 비활성화 확인
            Optional<FcmTokenEntity> deactivated = fcmTokenRepository.findByUserId(1L);
            assertThat(deactivated).isPresent();
            assertThat(deactivated.get().getActive()).isFalse();

            // 새 토큰으로 재등록
            fcmTokenService.registerToken(1L, "token-new", Platform.IOS);

            Optional<FcmTokenEntity> reactivated = fcmTokenRepository.findByUserId(1L);
            assertThat(reactivated).isPresent();
            assertThat(reactivated.get().getActive()).isTrue();
            assertThat(reactivated.get().getToken()).isEqualTo("token-new");
            assertThat(reactivated.get().getPlatform()).isEqualTo(Platform.IOS);
        }

        /**
         * **Validates: Requirements 2.1, 2.4**
         * 라운드트립 후에도 row 수는 정확히 1개를 유지한다.
         */
        @Test
        @DisplayName("라운드트립 후에도 row 수는 정확히 1개를 유지한다")
        void roundTrip_maintainsExactlyOneRow() {
            fcmTokenService.registerToken(1L, "token-1", Platform.ANDROID);
            fcmTokenService.deactivateToken(1L);
            fcmTokenService.registerToken(1L, "token-2", Platform.IOS);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).hasSize(1);
        }

        /**
         * **Validates: Requirements 2.1**
         * 존재하지 않는 유저의 deactivate는 에러 없이 완료된다.
         */
        @Test
        @DisplayName("존재하지 않는 유저의 deactivate는 에러 없이 완료된다")
        void deactivateNonExistentUser_completesWithoutError() {
            // 아무 row도 없는 상태에서 deactivate 호출
            fcmTokenService.deactivateToken(999L);

            List<FcmTokenEntity> allTokens = fcmTokenRepository.findAll();
            assertThat(allTokens).isEmpty();
        }

        /**
         * **Validates: Requirements 2.4**
         * 여러 번 비활성화/재활성화를 반복해도 항상 마지막 상태가 유지된다.
         */
        @Test
        @DisplayName("여러 번 비활성화/재활성화를 반복해도 항상 마지막 상태가 유지된다")
        void multipleRoundTrips_alwaysReflectsLatestState() {
            fcmTokenService.registerToken(1L, "token-a", Platform.ANDROID);
            fcmTokenService.deactivateToken(1L);
            fcmTokenService.registerToken(1L, "token-b", Platform.IOS);
            fcmTokenService.deactivateToken(1L);
            fcmTokenService.registerToken(1L, "token-c", Platform.ANDROID);

            Optional<FcmTokenEntity> result = fcmTokenRepository.findByUserId(1L);
            assertThat(result).isPresent();
            assertThat(result.get().getActive()).isTrue();
            assertThat(result.get().getToken()).isEqualTo("token-c");
            assertThat(result.get().getPlatform()).isEqualTo(Platform.ANDROID);

            // row 수는 항상 1개
            assertThat(fcmTokenRepository.findAll()).hasSize(1);
        }
    }
}
