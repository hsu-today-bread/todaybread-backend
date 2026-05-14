package com.todaybread.server.domain.interestarea.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.interestarea.dto.*;
import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import com.todaybread.server.domain.interestarea.repository.InterestAreaRepository;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 관심지역 삭제 시 키워드 보존 및 알림 복원 통합 검증 시나리오 테스트.
 *
 * 시나리오:
 * 1. 유저가 관심지역과 키워드를 보유한 상태
 * 2. 관심지역 삭제 → 키워드(UserKeyword) 레코드 유지 확인
 * 3. NotificationTargetCalculator가 해당 유저를 제외 (관심지역 없음)
 * 4. 유저가 새 관심지역을 재등록
 * 5. NotificationTargetCalculator가 해당 유저를 포함 (관심지역 존재, 3km 이내, 키워드 매칭)
 *
 * Validates: Requirements 4.3, 4.4, 8.1, 8.2, 8.3
 */
@ExtendWith(MockitoExtension.class)
class InterestAreaDeleteKeywordPreservationAndNotificationRestorationTest {

    @Mock
    private InterestAreaRepository interestAreaRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @InjectMocks
    private InterestAreaService interestAreaService;

    @InjectMocks
    private NotificationTargetCalculator calculator;

    // 고정 시간: 2024-01-15 (월요일) 10:00 KST
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2024, 1, 15, 10, 0).toInstant(ZoneOffset.of("+09:00")),
            ZoneId.of("Asia/Seoul")
    );

    private static final Long USER_ID = 10L;
    private static final Long STORE_ID = 1L;

    // --- Helper methods ---

    private static InterestAreaEntity interestAreaEntity(Long id, Long userId,
                                                         BigDecimal latitude, BigDecimal longitude) {
        InterestAreaEntity entity = InterestAreaEntity.builder()
                .userId(userId)
                .name("강남역")
                .address("서울특별시 강남구 강남대로 396")
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(3.0)
                .build();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static BreadEntity breadEntity(Long id, Long storeId, String name,
                                           int remainingQuantity, boolean isDeleted) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name(name)
                .description("설명")
                .originalPrice(5000)
                .salePrice(3000)
                .remainingQuantity(remainingQuantity)
                .build();
        ReflectionTestUtils.setField(bread, "id", id);
        if (isDeleted) {
            bread.softDelete(LocalDateTime.now());
        }
        return bread;
    }

    private static StoreEntity storeEntity(Long id, String name,
                                           BigDecimal latitude, BigDecimal longitude, boolean isActive) {
        StoreEntity store = StoreEntity.builder()
                .userId(100L)
                .name(name)
                .phoneNumber("010-1234-5678")
                .description("매장 설명")
                .addressLine1("서울시 강남구")
                .addressLine2("1층")
                .latitude(latitude)
                .longitude(longitude)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        ReflectionTestUtils.setField(store, "isActive", isActive);
        return store;
    }

    private static StoreBusinessHoursEntity businessHours(Long storeId, int dayOfWeek,
                                                          LocalTime startTime, LocalTime endTime,
                                                          LocalTime lastOrderTime) {
        return StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(dayOfWeek)
                .isClosed(false)
                .startTime(startTime)
                .endTime(endTime)
                .lastOrderTime(lastOrderTime)
                .build();
    }

    private static KeywordEntity keywordEntity(Long id, String normalisedText) {
        KeywordEntity keyword = KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build();
        ReflectionTestUtils.setField(keyword, "id", id);
        return keyword;
    }

    private static UserKeywordEntity userKeywordEntity(Long id, Long userId, Long keywordId) {
        UserKeywordEntity uk = UserKeywordEntity.builder()
                .userId(userId)
                .keywordId(keywordId)
                .displayText("키워드")
                .build();
        ReflectionTestUtils.setField(uk, "id", id);
        return uk;
    }

    // --- Scenario Test ---

    @Test
    @DisplayName("관심지역 삭제 → 키워드 보존 → 알림 제외 → 관심지역 재등록 → 알림 포함 시나리오")
    void deleteInterestArea_preservesKeywords_excludesFromNotification_thenReRegister_includesInNotification() {
        // === 사전 조건: 유저가 관심지역과 키워드를 보유 ===
        // 매장 좌표: 강남역 (37.4979, 127.0276)
        StoreEntity store = storeEntity(STORE_ID, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);

        // 빵: "소금빵", 재고 5개, 삭제되지 않음
        BreadEntity bread = breadEntity(1L, STORE_ID, "소금빵", 5, false);

        // 영업 중 설정 (월요일=1, 09:00~21:00, 라스트오더 20:00)
        StoreBusinessHoursEntity hours = businessHours(STORE_ID, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));

        // 키워드 "소금빵" 존재
        KeywordEntity keyword = keywordEntity(1L, "소금빵");

        // 유저의 UserKeyword 레코드
        UserKeywordEntity userKeyword = userKeywordEntity(1L, USER_ID, 1L);
        List<UserKeywordEntity> userKeywords = List.of(userKeyword);

        // 유저의 기존 관심지역: 매장에서 약 1km 이내 (37.505, 127.027)
        InterestAreaEntity originalInterestArea = interestAreaEntity(1L, USER_ID,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));

        // === Step 1: 관심지역 삭제 ===
        given(interestAreaRepository.findByUserId(USER_ID)).willReturn(Optional.of(originalInterestArea));
        given(userKeywordRepository.existsByUserId(USER_ID)).willReturn(true);

        InterestAreaDeleteResponse deleteResponse = interestAreaService.deleteInterestArea(USER_ID);

        // 삭제 성공 확인
        assertThat(deleteResponse.success()).isTrue();
        // 키워드가 존재하므로 keywordNotificationDisabled=true
        assertThat(deleteResponse.keywordNotificationDisabled()).isTrue();
        // 관심지역 삭제 호출 확인
        verify(interestAreaRepository).delete(originalInterestArea);

        // === Step 2: 키워드 보존 확인 (UserKeyword 레코드 유지) ===
        // 삭제 후에도 UserKeyword 레코드가 여전히 존재함을 확인
        given(userKeywordRepository.findByUserId(USER_ID)).willReturn(userKeywords);

        List<UserKeywordEntity> preservedKeywords = userKeywordRepository.findByUserId(USER_ID);
        assertThat(preservedKeywords).hasSize(1);
        assertThat(preservedKeywords.get(0).getUserId()).isEqualTo(USER_ID);
        assertThat(preservedKeywords.get(0).getKeywordId()).isEqualTo(1L);

        // === Step 3: NotificationTargetCalculator가 유저를 제외 (관심지역 없음) ===
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(STORE_ID))
                .willReturn(List.of(hours));
        given(keywordRepository.findAll()).willReturn(List.of(keyword));
        given(userKeywordRepository.findByKeywordIdInForUserNotificationTargets(any())).willReturn(userKeywords);
        // 관심지역 삭제된 상태 → 일괄 조회 결과가 비어 있음
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of());

        NotificationTargetResult resultAfterDelete = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // 관심지역이 없으므로 알림 대상에서 제외
        assertThat(resultAfterDelete.targets()).isEmpty();

        // === Step 4: 유저가 새 관심지역을 재등록 ===
        // 새 관심지역: 매장에서 약 2km 이내 (37.51, 127.03)
        InterestAreaEntity newInterestArea = interestAreaEntity(2L, USER_ID,
                BigDecimal.valueOf(37.51), BigDecimal.valueOf(127.03));
        InterestAreaRequest reRegisterRequest = new InterestAreaRequest(
                "역삼역",
                "서울특별시 강남구 역삼로 180",
                BigDecimal.valueOf(37.51),
                BigDecimal.valueOf(127.03)
        );
        given(interestAreaRepository.save(any(InterestAreaEntity.class))).willReturn(newInterestArea);

        InterestAreaResponse createResponse = interestAreaService.createInterestArea(USER_ID, reRegisterRequest);

        // 재등록 성공 확인
        assertThat(createResponse.id()).isEqualTo(2L);
        assertThat(createResponse.radiusKm()).isEqualTo(3.0);

        // === Step 5: NotificationTargetCalculator가 유저를 포함 (관심지역 존재, 3km 이내, 키워드 매칭) ===
        given(interestAreaRepository.findByUserIdIn(any())).willReturn(List.of(newInterestArea));

        NotificationTargetResult resultAfterReRegister = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // 관심지역 재등록 후 알림 대상에 포함
        assertThat(resultAfterReRegister.targets()).hasSize(1);
        NotificationTarget target = resultAfterReRegister.targets().get(0);
        assertThat(target.userId()).isEqualTo(USER_ID);
        assertThat(target.matchedKeywords()).containsExactly("소금빵");
        assertThat(target.storeInfo().storeId()).isEqualTo(STORE_ID);
        assertThat(target.storeInfo().storeName()).isEqualTo("강남베이커리");
        assertThat(target.breadInfo().breadId()).isEqualTo(1L);
        assertThat(target.breadInfo().breadName()).isEqualTo("소금빵");
        assertThat(target.minutesUntilLastOrder()).isEqualTo(600L); // 10:00 → 20:00 = 600분
    }

    @Test
    @DisplayName("관심지역 삭제 후 재등록 시 키워드가 0개이면 알림 대상에 포함되지 않음")
    void deleteAndReRegister_withNoKeywords_userNotIncludedInNotification() {
        // === 사전 조건: 유저가 관심지역만 보유, 키워드 없음 ===
        StoreEntity store = storeEntity(STORE_ID, "강남베이커리",
                BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), true);
        BreadEntity bread = breadEntity(1L, STORE_ID, "소금빵", 5, false);
        StoreBusinessHoursEntity hours = businessHours(STORE_ID, 1,
                LocalTime.of(9, 0), LocalTime.of(21, 0), LocalTime.of(20, 0));
        KeywordEntity keyword = keywordEntity(1L, "소금빵");

        InterestAreaEntity originalInterestArea = interestAreaEntity(1L, USER_ID,
                BigDecimal.valueOf(37.505), BigDecimal.valueOf(127.027));

        // === Step 1: 관심지역 삭제 (키워드 없음) ===
        given(interestAreaRepository.findByUserId(USER_ID)).willReturn(Optional.of(originalInterestArea));
        given(userKeywordRepository.existsByUserId(USER_ID)).willReturn(false);

        InterestAreaDeleteResponse deleteResponse = interestAreaService.deleteInterestArea(USER_ID);

        assertThat(deleteResponse.success()).isTrue();
        // 키워드가 없으므로 keywordNotificationDisabled=false
        assertThat(deleteResponse.keywordNotificationDisabled()).isFalse();

        // === Step 2: 새 관심지역 재등록 ===
        InterestAreaEntity newInterestArea = interestAreaEntity(2L, USER_ID,
                BigDecimal.valueOf(37.51), BigDecimal.valueOf(127.03));
        given(interestAreaRepository.save(any(InterestAreaEntity.class))).willReturn(newInterestArea);

        InterestAreaRequest reRegisterRequest = new InterestAreaRequest(
                "역삼역",
                "서울특별시 강남구 역삼로 180",
                BigDecimal.valueOf(37.51),
                BigDecimal.valueOf(127.03)
        );
        InterestAreaResponse createResponse = interestAreaService.createInterestArea(USER_ID, reRegisterRequest);
        assertThat(createResponse.id()).isEqualTo(2L);

        // === Step 3: 알림 대상 계산 - 키워드가 없으므로 유저 미포함 ===
        given(storeBusinessHoursRepository.findByStoreIdOrderByDayOfWeekAsc(STORE_ID))
                .willReturn(List.of(hours));
        given(keywordRepository.findAll()).willReturn(List.of(keyword));
        // 키워드 "소금빵"을 구독하는 유저가 없음
        given(userKeywordRepository.findByKeywordIdInForUserNotificationTargets(any())).willReturn(List.of());

        NotificationTargetResult result = calculator.calculateTargets(bread, store, FIXED_CLOCK);

        // 키워드가 없으므로 알림 대상에 포함되지 않음
        assertThat(result.targets()).isEmpty();
    }
}
