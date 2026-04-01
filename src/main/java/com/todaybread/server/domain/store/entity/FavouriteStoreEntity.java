package com.todaybread.server.domain.store.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * favourite_store 테이블 엔티티를 정의합니다.
 */
@Entity
@Table(name = "favourite_store",
        uniqueConstraints = @UniqueConstraint(name = "uk_favourite_store_user_id_store_id",
                columnNames = {"user_id", "store_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavouriteStoreEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Builder
    private FavouriteStoreEntity(Long userId, Long storeId) {
        this.userId = userId;
        this.storeId = storeId;
    }
}
