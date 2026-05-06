package com.todaybread.server.domain.keyword.repository;

import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Keyword를 위한 리포지터리입니다.
 * 키워드 검색을 위한 메서드를 제공합니다.
 */
public interface KeywordRepository extends JpaRepository<KeywordEntity, Long> {

    /**
     * 정규화된 텍스트로 키워드를 조회합니다.
     *
     * @param normalisedText 정규화된 키워드 텍스트
     * @return 키워드 엔티티 (없으면 빈 Optional)
     */
    Optional<KeywordEntity> findByNormalisedText(String normalisedText);
}
