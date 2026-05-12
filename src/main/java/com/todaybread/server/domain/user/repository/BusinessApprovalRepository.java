package com.todaybread.server.domain.user.repository;

import com.todaybread.server.domain.user.entity.BusinessApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사장님 사업자 검증 승인 리포지터리입니다.
 */
public interface BusinessApprovalRepository extends JpaRepository<BusinessApprovalEntity, Long> {

    boolean existsByBusinessNumberHash(String businessNumberHash);
}
