package com.todaybread.server.domain.bread.entity;

import com.todaybread.server.global.entity.BaseEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빵 메뉴를 정의하는 엔티티입니다.
 */
@Entity
@Table(name = "bread")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreadEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "original_price", nullable = false)
    private int originalPrice;

    @Column(name = "sale_price", nullable = false)
    private int salePrice;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Builder
    private BreadEntity(Long storeId, String name, String description,
                       int originalPrice, int salePrice, int remainingQuantity) {
       validateFields(originalPrice, salePrice, remainingQuantity);
       this.storeId = storeId;
       this.name = name;
       this.description = description;
       this.originalPrice = originalPrice;
       this.salePrice = salePrice;
       this.remainingQuantity = remainingQuantity;
    }

    /**
     * 재고 변화 시, 사용됩니다.
     * 재고가 0이하로 내려가면 오류를 던집니다.
     *
     * @param number 변경된 재고 수
     */
    public void decreaseQuantity(int number) {
        int checked = this.remainingQuantity - number;
        if (checked < 0) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
        this.remainingQuantity = checked;
    }

    /**
     * 재고를 증가시킵니다.
     * 주문 취소 시 재고 복원에 사용됩니다.
     *
     * @param number 증가할 수량
     */
    public void increaseQuantity(int number) {
        this.remainingQuantity += number;
    }

    /**
     * 재고를 set 합니다.
     *
     * @param number 변경할 수
     */
    public void changeQuantity(int number) {
        if (number < 0) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
        this.remainingQuantity = number;
    }

    /**
     * 빵 정보를 업데이트합니다.
     *
     * @param name              이름
     * @param originalPrice     원가
     * @param salePrice         할인가
     * @param remainingQuantity 재고
     * @param description       설명
     */
    public void updateInfo(String name, int originalPrice,
                                  int salePrice, int remainingQuantity,
                                  String description) {
        validateFields(originalPrice, salePrice, remainingQuantity);
        this.name = name;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.remainingQuantity = remainingQuantity;
        this.description = description;
    }

    /**
     * 가격 및 재고 검증을 수행합니다.
     * 생성자와 updateInfo에서 공통으로 사용합니다.
     */
    private void validateFields(int originalPrice, int salePrice, int remainingQuantity) {
        if (originalPrice < 0 || salePrice < 0) {
            throw new CustomException(ErrorCode.BREAD_INVALID_PRICE);
        }
        if (remainingQuantity < 0) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
    }
}
