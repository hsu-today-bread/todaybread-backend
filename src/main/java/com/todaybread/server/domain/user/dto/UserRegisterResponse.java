package com.todaybread.server.domain.user.dto;

/**
 * 회원가입 완료 후, 응답 DTO
 * static 메서드로 바로 회원 가입 성공 시, 응답을 내려줍니다.
 *
 * @param status 회원 가입 성공 유무 코드
 * @param message 짧은 환영 인사
 */
public record UserRegisterResponse(boolean status, String message){

    public static UserRegisterResponse ok(){
        return new UserRegisterResponse(true, "회원가입 완료");
    }
}
