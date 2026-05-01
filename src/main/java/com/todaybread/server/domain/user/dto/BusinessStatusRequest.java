package com.todaybread.server.domain.user.dto;

import java.util.List;

/**
 * 국세청 사업자 상태 조회 API 요청 DTO입니다.
 *
 * @param b_no 사업자 등록 번호 목록
 */
public record BusinessStatusRequest(List<String> b_no) {

    /**
     * 단일 사업자 등록 번호로 요청 객체를 생성합니다.
     *
     * @param bossNumber 사업자 등록 번호
     * @return BusinessStatusRequest 객체
     */
    public static BusinessStatusRequest of(String bossNumber) {
        return new BusinessStatusRequest(List.of(bossNumber));
    }
}