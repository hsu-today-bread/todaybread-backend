package com.todaybread.server.domain.keyword.repository;

import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 키워드 - 유저 관계 엔티티를 위한 리포지터리입니다.
 */
public interface UserKeywordRepository extends JpaRepository<UserKeywordEntity, Long> {

    /**
     * 특정 사용자의 키워드 연결 목록을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 사용자-키워드 연결 목록
     */
    List<UserKeywordEntity> findByUserId(Long userId);

    /**
     * 특정 키워드를 구독한 사용자 연결 목록을 조회합니다.
     * 향후 알림 기능에서 사용됩니다.
     *
     * @param keywordId 키워드 ID
     * @return 사용자-키워드 연결 목록
     */
    List<UserKeywordEntity> findByKeywordId(Long keywordId);

    /**
     * 사용자-키워드 중복 등록 여부를 확인합니다.
     *
     * @param userId 유저 ID
     * @param keywordId 키워드 ID
     * @return 중복 여부
     */
    boolean existsByUserIdAndKeywordId(Long userId, Long keywordId);

    /**
     * 특정 사용자의 키워드 등록 수를 비관적 락으로 조회합니다.
     * 동시 요청 시 개수 제한을 정확히 보장하기 위해 사용합니다.
     *
     * @param userId 유저 ID
     * @return 키워드 등록 수
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(uk) FROM UserKeywordEntity uk WHERE uk.userId = :userId")
    long countByUserIdWithLock(@Param("userId") Long userId);
}
