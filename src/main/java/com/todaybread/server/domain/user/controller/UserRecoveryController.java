package com.todaybread.server.domain.user.controller;

import com.todaybread.server.domain.user.dto.*;
import com.todaybread.server.domain.user.service.UserRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserRecoveryController {

    private final UserRecoveryService userRecoveryService;

    /**
     * 전화번호 이메일 찾기를 담당하는 컨트롤러입니다.
     * @param phone 전화번호
     * @return 응답 DTO
     */
    @GetMapping("/find-email")
    public UserFindEmailResponse findEmailByPhone(@RequestParam("phone") @NotBlank String phone) {
        return userRecoveryService.findEmailByPhone(phone);
    }

    /**
     * 본인 확인을 담당합니다.
     * @param phone 전화번호
     * @param email 이메일
     * @return 응답 DTO
     */
    @GetMapping("/verify-identity")
    public VerifyIdentityResponse verifyIdentity(@RequestParam("phone") @NotBlank String phone,
            @RequestParam("email") @NotBlank String email) {
        return userRecoveryService.verifyIdentity(phone, email);
    }

    /**
     * 비밀번호 재설정을 담당합니다.
     * @param request 요청 DTO
     * @return 응답 DTO
     */
    @PostMapping("/reset-password")
    public ResetPasswordResponse resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return userRecoveryService.resetPassword(request);
    }
}
