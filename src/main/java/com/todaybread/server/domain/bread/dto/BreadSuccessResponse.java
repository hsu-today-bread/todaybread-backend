package com.todaybread.server.domain.bread.dto;

/**
 * 빵 도메인 성공 응답 DTO (삭제, 재고 변경 등)
 * @param success 성공 여부
 */
public record BreadSuccessResponse(
        boolean success
) {
    public static BreadSuccessResponse ok() {
        return new BreadSuccessResponse(true);
    }
}
