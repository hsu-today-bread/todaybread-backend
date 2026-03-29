package com.todaybread.server.domain.bread.entity;

import com.todaybread.server.domain.bread.dto.BreadUpdateRequest;
import com.todaybread.server.global.entity.BaseEntity;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stock을 정의하는 엔티티입니다.
 */
@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class BreadEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "original_price", nullable = false)
    private int  originalPrice;

    @Column(name = "sale_price", nullable = false)
    private int salePrice;

    @Column(name ="remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(name = "description", nullable = false,length=255)
    private String description;

    @Builder
    public BreadEntity(Long storeId, String name, String description,
                       int originalPrice, int salePrice, int remainingQuantity) {
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
    public void updateQuantity(int number) {
        int checked = this.remainingQuantity - number;
        if (checked < 0) {
            throw new CustomException(ErrorCode.BREAD_INSUFFICIENT_QUANTITY);
        }
        this.remainingQuantity = checked;
    }

    /**
     * 재고를 set 합니다.
     *
     * @param number 변경할 수
     */
    public void setQuantity(int number) {
        this.remainingQuantity = number;
    }

    public void updateInfo(String name, int originalPrice,
                                  int salePrice, int remainingQuantity,
                                  String description) {
        this.name = name;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.remainingQuantity = remainingQuantity;
        this.description = description;
    }
}
