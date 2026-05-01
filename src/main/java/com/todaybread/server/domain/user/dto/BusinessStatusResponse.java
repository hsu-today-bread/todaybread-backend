package com.todaybread.server.domain.user.dto;

import java.util.List;

/**
 * 국세청 사업자 상태 조회 API 응답 DTO입니다.
 *
 * @param status_code API 응답 상태 코드
 * @param data 사업자 상태 데이터 목록
 */
public record BusinessStatusResponse(String status_code, List<BusinessData> data) {

    /**
     * 사업자 상태 데이터입니다.
     *
     * @param b_no 사업자 등록 번호
     * @param b_stt_cd 사업자 상태 코드 (01: 계속사업자, 02: 휴업자, 03: 폐업자)
     */
    public record BusinessData(String b_no, String b_stt_cd) {}
}