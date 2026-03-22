package com.todaybread.server.domain.store.repository;

import com.todaybread.server.domain.store.entity.StoreEntity;
import jakarta.annotation.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * store 리포지터리입니다.
 */
@Resource
public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
    boolean existsByUserIdAndIsActiveTrue(Long userId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<StoreEntity> findByUserIdAndIsActiveTrue(Long userId);
}
