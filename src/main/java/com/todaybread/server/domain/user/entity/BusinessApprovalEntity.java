package com.todaybread.server.domain.user.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사장님 등록 시 검증 완료된 사업자 정보를 저장합니다.
 */
@Entity
@Table(name = "business_approval", uniqueConstraints = {
        @UniqueConstraint(name = "uk_business_approval_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_business_approval_number_hash", columnNames = "business_number_hash")
}, indexes = {
        @Index(name = "idx_business_approval_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessApprovalEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "business_number_hash", nullable = false, length = 64)
    private String businessNumberHash;

    @Column(name = "business_number_last4", nullable = false, length = 4)
    private String businessNumberLast4;

    @Column(name = "business_start_date", nullable = false, length = 8)
    private String businessStartDate;

    @Column(name = "business_status_code", nullable = false, length = 2)
    private String businessStatusCode;

    @Column(name = "business_status_name", nullable = false, length = 30)
    private String businessStatusName;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Builder
    private BusinessApprovalEntity(Long userId, String businessNumberHash, String businessNumberLast4,
                                   String businessStartDate, String businessStatusCode,
                                   String businessStatusName, LocalDateTime verifiedAt) {
        this.userId = userId;
        this.businessNumberHash = businessNumberHash;
        this.businessNumberLast4 = businessNumberLast4;
        this.businessStartDate = businessStartDate;
        this.businessStatusCode = businessStatusCode;
        this.businessStatusName = businessStatusName;
        this.verifiedAt = verifiedAt;
    }
}
