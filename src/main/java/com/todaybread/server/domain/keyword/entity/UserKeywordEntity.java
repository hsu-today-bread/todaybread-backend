package com.todaybread.server.domain.keyword.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 - 키워드 간의 관계 테이블입니다.
 */
@Entity
@Table(name = "user_keyword",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_keyword",
                columnNames = {"user_id", "keyword_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserKeywordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "display_text",nullable = false)
    private String displayText;

    @Builder
    private UserKeywordEntity(Long userId, Long keywordId, String displayText) {
        this.userId = userId;
        this.keywordId = keywordId;
        this.displayText = displayText;
    }
}
