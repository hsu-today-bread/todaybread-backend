package com.todaybread.server.domain.user.controller;

import com.todaybread.server.domain.user.dto.UserRegisterRequest;
import com.todaybread.server.domain.user.dto.UserRegisterResponse;
import com.todaybread.server.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * user 도메인 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 회원가입을 담당하는 함수입니다. 요청시 DTO를 받습니다.
     *
     * @param request 회원 정보가 있는 DTO
     * @return 회원 가입 상태에 대한 응답
     */
    @PostMapping("/register")
    public UserRegisterResponse registerUser(@RequestBody @Valid UserRegisterRequest request) {
        userService.register(request);
        return UserRegisterResponse.ok();
    }

    /**
     * 회원 가입 시, 이메일 중복 확인을 체크합니다.
     *
     * @param value 이메일
     * @return 중복 여부 체크 후, true, false 반환
     */
    @GetMapping("/exist/email")
    public boolean checkEmail(@RequestParam("value") @NotBlank @Email String value) {
        return userService.checkEmail(value);
    }

    /**
     * 회원 가입 시, 전화 번호 중복을 체크합니다.
     *
     * @param value 전화번호
     * @return 중복 여부 체크 후, 존재하면 true, 존재하지 않으면 false 반환
     */
    @GetMapping("/exist/phone")
    public boolean checkPhone(@RequestParam("value") @NotBlank String value) {
        return userService.checkPhone(value);
    }
}

