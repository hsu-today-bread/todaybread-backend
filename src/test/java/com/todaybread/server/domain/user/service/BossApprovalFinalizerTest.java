package com.todaybread.server.domain.user.service;

import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.service.AuthService;
import com.todaybread.server.domain.user.client.dto.NtsBusinessValidationResult;
import com.todaybread.server.domain.user.dto.UserBossResponse;
import com.todaybread.server.domain.user.entity.BusinessApprovalEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.BusinessApprovalRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BossApprovalFinalizerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessApprovalRepository businessApprovalRepository;

    @Mock
    private BusinessNumberHasher businessNumberHasher;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private Clock clock;

    @InjectMocks
    private BossApprovalFinalizer finalizer;

    @Test
    void finalizeApproval_savesApprovalAndIssuesBossTokens() {
        UserEntity user = TestFixtures.user(1L, false);
        NtsBusinessValidationResult validationResult =
                new NtsBusinessValidationResult("01", "계속사업자");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessNumberHasher.hash("1234567890")).willReturn("hash");
        given(businessNumberHasher.last4("1234567890")).willReturn("7890");
        given(businessApprovalRepository.existsByBusinessNumberHash("hash")).willReturn(false);
        given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
        given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());
        given(jwtTokenService.generateAccessToken(1L, user.getEmail(), "BOSS")).willReturn("access");
        given(jwtTokenService.generateRefreshToken(1L)).willReturn("refresh");

        UserBossResponse response = finalizer.finalizeApproval(
                1L, "1234567890", "20200101", validationResult);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(user.getIsBoss()).isTrue();
        verify(authService).saveRefreshToken(1L, "refresh");

        ArgumentCaptor<BusinessApprovalEntity> captor =
                ArgumentCaptor.forClass(BusinessApprovalEntity.class);
        verify(businessApprovalRepository).saveAndFlush(captor.capture());
        BusinessApprovalEntity approval = captor.getValue();
        assertThat(approval.getBusinessNumberHash()).isEqualTo("hash");
        assertThat(approval.getBusinessNumberLast4()).isEqualTo("7890");
        assertThat(approval.getBusinessStartDate()).isEqualTo("20200101");
        assertThat(approval.getBusinessStatusCode()).isEqualTo("01");
        assertThat(approval.getBusinessStatusName()).isEqualTo("계속사업자");
    }

    @Test
    void finalizeApproval_rejectsDuplicateBusinessNumber() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessNumberHasher.hash("1234567890")).willReturn("hash");
        given(businessApprovalRepository.existsByBusinessNumberHash("hash")).willReturn(true);

        assertThatThrownBy(() -> finalizer.finalizeApproval(
                1L, "1234567890", "20200101",
                new NtsBusinessValidationResult("01", "계속사업자")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_NUMBER_ALREADY_REGISTERED);
    }

    @Test
    void finalizeApproval_mapsUniqueConstraintRaceToDuplicateBusinessNumber() {
        UserEntity user = TestFixtures.user(1L, false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessNumberHasher.hash("1234567890")).willReturn("hash");
        given(businessNumberHasher.last4("1234567890")).willReturn("7890");
        given(businessApprovalRepository.existsByBusinessNumberHash("hash")).willReturn(false);
        given(clock.instant()).willReturn(TestFixtures.FIXED_CLOCK.instant());
        given(clock.getZone()).willReturn(TestFixtures.FIXED_CLOCK.getZone());
        given(businessApprovalRepository.saveAndFlush(any(BusinessApprovalEntity.class)))
                .willThrow(new DataIntegrityViolationException("uk_business_approval_number_hash"));

        assertThatThrownBy(() -> finalizer.finalizeApproval(
                1L, "1234567890", "20200101",
                new NtsBusinessValidationResult("01", "계속사업자")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_NUMBER_ALREADY_REGISTERED);
    }
}
