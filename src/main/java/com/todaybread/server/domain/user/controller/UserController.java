package com.todaybread.server.domain.user.controller;

import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserRegisterResponse> registerUser(@RequestBody @Valid UserRegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok(UserRegisterResponse.ok());
    }

    /**
     * 로그인을 담당합니다.
     *
     * @param request 로그인 정보 DTO
     * @return 로그인 응답
     */
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> loginUser(@RequestBody @Valid UserLoginRequest request){
        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * 회원 가입 시, 이메일 중복 확인을 체크합니다.
     *
     * @param value 이메일
     * @return 중복 여부 체크 응답
     */
    @GetMapping("/exist/email")
    public ResponseEntity<UserCheckEmailResponse> checkEmail(@RequestParam("value") @NotBlank @Email String value) {
        boolean exists = userService.checkEmail(value);
        return ResponseEntity.ok(UserCheckEmailResponse.of(exists));
    }

    /**
     * 회원 가입 시, 닉네임 중복을 체크합니다.
     *
     * @param value 닉네임
     * @return 중복 여부 체크 응답
     */
    @GetMapping("/exist/nickname")
    public ResponseEntity<UserCheckNicknameResponse> checkNickname(@RequestParam("value") @NotBlank String value) {
        boolean exists = userService.checkNickname(value);
        return ResponseEntity.ok(UserCheckNicknameResponse.of(exists));
    }

    /**
     * 회원 가입 시, 전화 번호 중복을 체크합니다.
     *
     * @param value 전화번호
     * @return 중복 여부 체크 응답
     */
    @GetMapping("/exist/phone")
    public ResponseEntity<UserCheckPhoneResponse> checkPhone(@RequestParam("value") @NotBlank String value) {
        boolean exists = userService.checkPhone(value);
        return ResponseEntity.ok(UserCheckPhoneResponse.of(exists));
    }

    /**
     * 이름과 전화번호로 가입된 이메일을 찾습니다.
     *
     * @param request 이름과 전화번호 DTO
     * @return 이메일 조회 응답
     */
    @GetMapping("/email")
    public ResponseEntity<UserFindEmailResponse> findEmail(@ModelAttribute @Valid UserFindEmailRequest request) {
        String email = userService.findEmail(request);
        return ResponseEntity.ok(UserFindEmailResponse.ok(email));
    }

    /**
     * 이메일과 전화번호, 이름을 통해 비밀번호를 재설정합니다.
     *
     * @param request 비밀번호 재설정 요청 DTO
     * @return 비밀번호 재설정 응답
     */
    @PostMapping("/password/reset")
    public ResponseEntity<UserResetPasswordResponse> resetPassword(@RequestBody @Valid UserResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(UserResetPasswordResponse.ok());
    }
}
