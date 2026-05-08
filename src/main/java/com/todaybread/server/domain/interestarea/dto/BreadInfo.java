package com.todaybread.server.domain.interestarea.dto;

/**
 * 빵 정보 DTO
 *
 * @param breadId 빵 ID
 * @param breadName 빵 이름
 */
public record BreadInfo(
        Long breadId,
        String breadName
) {
}
