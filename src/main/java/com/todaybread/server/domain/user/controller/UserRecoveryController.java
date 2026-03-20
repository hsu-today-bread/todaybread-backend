package com.todaybread.server.domain.user.controller;

import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.service.UserRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/recovery")
@RequiredArgsConstructor
@Validated
public class UserRecoveryController {

    private final UserRecoveryService userRecoveryService;

    /**
     * 전화번호 이메일 찾기를 담당하는 컨트롤러입니다.
     * @param value 전화번호
     * @return 응답 DTO
     */
    @PostMapping("/find-email")
    public UserFindEmailResponse findEmailByPhone(@RequestParam("value") @NotBlank String value) {
        return userRecoveryService.findEmailByPhone(value);
    }

    /**
     * 비밀번호 찾기를 담당합니다.
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @PostMapping("/verify-identity")
    public UserResetPasswordResponse verifyIdentity(@RequestBody @Valid UserResetPasswordRequest request) {
        return userRecoveryService.verifyIdentity(request);
    }

    @PostMapping("/reset-password")
    public UserNewPasswordResponse resetPassword(@RequestBody @Valid UserNewPasswordRequest request) {
        return userRecoveryService.resetPassword(request);
    }
}
