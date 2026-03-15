package com.todaybread.server.domain.keyword.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * keyword를 정의하는 엔티티입니다.
 */
@Entity
@Table(name = "keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "normalised_text", nullable = false, unique = true, length = 255)
    private String normalisedText;

    @Builder
    private KeywordEntity(String normalisedText) {
        this.normalisedText = normalisedText;
    }
}
