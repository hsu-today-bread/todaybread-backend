package com.todaybread.server.domain.store.controller;

import com.todaybread.server.config.jwt.JwtRoleHelper;
import com.todaybread.server.domain.store.dto.StoreImageResponse;
import com.todaybread.server.domain.store.service.StoreImageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 가게 이미지 업로드를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BOSS')")
public class StoreImageController {

    private final StoreImageService storeImageService;

    /**
     * 가게 이미지를 일괄 업로드합니다 (Replace All 패턴).
     * 매 호출 시 기존 이미지를 모두 삭제하고 새로 전달받은 이미지를 저장합니다.
     *
     * @param jwt    JWT 토큰
     * @param images 업로드할 이미지 파일 목록
     * @return 저장된 이미지 목록
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<StoreImageResponse> uploadImages(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("images") List<MultipartFile> images) {
        Long userId = JwtRoleHelper.getUserId(jwt);
        return storeImageService.replaceImages(userId, images);
    }
}
