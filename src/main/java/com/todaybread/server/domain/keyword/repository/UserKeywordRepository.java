package com.todaybread.server.domain.keyword.repository;

import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 키워드 - 유저 관계 엔티티를 위한 리포지터리입니다.
 * findByUserId: 특정 사용자의 키워드 연결 목록 조회.
 * findByKeywordId: 특정 키워드를 구독한 사용자 연결 목록 조회.
 * existsByUserId: 특정 사용자의 키워드 연결 존재 여부 확인.
 */
@Repository
public interface UserKeywordRepository extends JpaRepository<UserKeywordEntity, Long> {
    List<UserKeywordEntity> findByUserId(Long userId);
    List<UserKeywordEntity> findByKeywordId(Long keywordId);
    boolean existsByUserId(Long userId);
}
