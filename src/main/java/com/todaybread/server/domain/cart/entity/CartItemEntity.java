package com.todaybread.server.domain.cart.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니에 담긴 개별 빵 항목을 나타내는 엔티티입니다.
 * 빵 ID와 수량 정보를 포함합니다.
 */
@Entity
@Table(name = "cart_item",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_cart_item_cart_id_bread_id",
           columnNames = {"cart_id", "bread_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "bread_id", nullable = false)
    private Long breadId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Builder
    private CartItemEntity(Long cartId, Long breadId, int quantity) {
        this.cartId = cartId;
        this.breadId = breadId;
        this.quantity = quantity;
    }

    /**
     * 수량을 증가시킵니다.
     *
     * @param amount 증가할 수량 (양수만 허용)
     */
    public void increaseQuantity(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다");
        }
        this.quantity += amount;
    }

    /**
     * 수량을 변경합니다.
     *
     * @param quantity 새로운 수량 (1 이상만 허용)
     */
    public void updateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
        this.quantity = quantity;
    }
}
