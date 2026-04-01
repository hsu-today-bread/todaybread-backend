package com.todaybread.server.domain.keyword.repository;

import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Keyword를 위한 리포지터리입니다.
 * 키워드 검색을 위한 메서드를 제공합니다.
 * findByNormalisedText: 정규화된 텍스트로 키워드 조회. Spring Data JPA 메서드 네이밍 규칙에 따라 자동 쿼리 생성.
 * existsByNormalisedText: 정규화된 텍스트로 존재 여부 확인. count 쿼리 대신 exists 쿼리로 효율적 처리.
 */
public interface KeywordRepository extends JpaRepository<KeywordEntity, Long> {
    Optional<KeywordEntity> findByNormalisedText(String normalisedText);
    boolean existsByNormalisedText(String normalisedText);
}
