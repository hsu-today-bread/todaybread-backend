package com.todaybread.server.domain.cart.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저의 장바구니를 나타내는 엔티티입니다.
 * 하나의 유저는 하나의 장바구니를 가지며, 하나의 매장 빵만 담을 수 있습니다.
 */
@Entity
@Table(name = "cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "store_id")
    private Long storeId;

    @Builder
    private CartEntity(Long userId, Long storeId) {
        this.userId = userId;
        this.storeId = storeId;
    }

    /**
     * 장바구니의 매장 ID를 변경합니다.
     * 장바구니를 비울 때 null로 설정하여 매장 제약을 해제합니다.
     *
     * @param storeId 매장 ID (null 가능)
     */
    public void updateStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
