package com.todaybread.server.domain.order.dto;

/**
 * 유저별 구매 횟수 집계 결과를 위한 JPA Projection 인터페이스입니다.
 */
public interface PurchaseCountProjection {
    Long getUserId();
    Long getPurchaseCount();
}
