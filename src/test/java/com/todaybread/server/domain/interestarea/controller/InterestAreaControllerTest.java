package com.todaybread.server.domain.interestarea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaybread.server.config.SecurityConfig;
import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.interestarea.dto.InterestAreaDeleteResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaRequest;
import com.todaybread.server.domain.interestarea.dto.InterestAreaResponse;
import com.todaybread.server.domain.interestarea.dto.InterestAreaWrapperResponse;
import com.todaybread.server.domain.interestarea.service.InterestAreaService;
import com.todaybread.server.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InterestAreaController @WebMvcTest 슬라이스 테스트입니다.
 * 인증/비인증 요청, 유효성 검증 실패, 정상 응답을 검증합니다.
 *
 * Validates: Requirements 2.4, 1.3, 1.4, 1.5, 1.6
 */
@WebMvcTest(controllers = InterestAreaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InterestAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterestAreaService interestAreaService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    // --- GET /api/interest-area ---

    @Test
    @DisplayName("GET /api/interest-area - 인증된 요청은 200과 래퍼 응답을 반환한다")
    void getInterestArea_authenticated_returns200WithWrapperResponse() throws Exception {
        // given
        InterestAreaResponse areaResponse = new InterestAreaResponse(
                1L, "강남역", "서울시 강남구 강남대로 396",
                new BigDecimal("37.4979500"), new BigDecimal("127.0276000"), 3.0);
        InterestAreaWrapperResponse wrapperResponse = new InterestAreaWrapperResponse(areaResponse);

        given(interestAreaService.getInterestArea(eq(1L))).willReturn(wrapperResponse);

        // when & then
        mockMvc.perform(get("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestArea.id").value(1))
                .andExpect(jsonPath("$.interestArea.name").value("강남역"))
                .andExpect(jsonPath("$.interestArea.address").value("서울시 강남구 강남대로 396"))
                .andExpect(jsonPath("$.interestArea.latitude").value(37.4979500))
                .andExpect(jsonPath("$.interestArea.longitude").value(127.0276000))
                .andExpect(jsonPath("$.interestArea.radiusKm").value(3.0));
    }

    @Test
    @DisplayName("GET /api/interest-area - 비인증 요청은 401을 반환한다")
    void getInterestArea_unauthenticated_returns401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/interest-area"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/interest-area ---

    @Test
    @DisplayName("POST /api/interest-area - 유효한 요청은 200과 응답을 반환한다")
    void createInterestArea_validRequest_returns200() throws Exception {
        // given
        InterestAreaRequest request = new InterestAreaRequest(
                "강남역", "서울시 강남구 강남대로 396",
                new BigDecimal("37.4979500"), new BigDecimal("127.0276000"));

        InterestAreaResponse response = new InterestAreaResponse(
                1L, "강남역", "서울시 강남구 강남대로 396",
                new BigDecimal("37.4979500"), new BigDecimal("127.0276000"), 3.0);

        given(interestAreaService.createInterestArea(eq(1L), any(InterestAreaRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("강남역"))
                .andExpect(jsonPath("$.radiusKm").value(3.0));
    }

    @Test
    @DisplayName("POST /api/interest-area - 위도 범위 초과(91)는 400을 반환한다")
    void createInterestArea_invalidLatitude_returns400() throws Exception {
        // given - latitude 91 (범위 초과)
        InterestAreaRequest request = new InterestAreaRequest(
                "강남역", "서울시 강남구 강남대로 396",
                new BigDecimal("91.0"), new BigDecimal("127.0276000"));

        // when & then
        mockMvc.perform(post("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("POST /api/interest-area - 경도 범위 초과(181)는 400을 반환한다")
    void createInterestArea_invalidLongitude_returns400() throws Exception {
        // given - longitude 181 (범위 초과)
        InterestAreaRequest request = new InterestAreaRequest(
                "강남역", "서울시 강남구 강남대로 396",
                new BigDecimal("37.4979500"), new BigDecimal("181.0"));

        // when & then
        mockMvc.perform(post("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("POST /api/interest-area - 빈 이름은 400을 반환한다")
    void createInterestArea_blankName_returns400() throws Exception {
        // given - name이 빈 문자열
        String body = """
                {
                    "name": "",
                    "address": "서울시 강남구 강남대로 396",
                    "latitude": 37.4979500,
                    "longitude": 127.0276000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @DisplayName("POST /api/interest-area - 빈 주소는 400을 반환한다")
    void createInterestArea_blankAddress_returns400() throws Exception {
        // given - address가 빈 문자열
        String body = """
                {
                    "name": "강남역",
                    "address": "",
                    "latitude": 37.4979500,
                    "longitude": 127.0276000
                }
                """;

        // when & then
        mockMvc.perform(post("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    // --- PUT /api/interest-area ---

    @Test
    @DisplayName("PUT /api/interest-area - 유효한 요청은 200을 반환한다")
    void updateInterestArea_validRequest_returns200() throws Exception {
        // given
        InterestAreaRequest request = new InterestAreaRequest(
                "홍대입구", "서울시 마포구 양화로 160",
                new BigDecimal("37.5571000"), new BigDecimal("126.9236000"));

        InterestAreaResponse response = new InterestAreaResponse(
                1L, "홍대입구", "서울시 마포구 양화로 160",
                new BigDecimal("37.5571000"), new BigDecimal("126.9236000"), 3.0);

        given(interestAreaService.updateInterestArea(eq(1L), any(InterestAreaRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("홍대입구"))
                .andExpect(jsonPath("$.radiusKm").value(3.0));
    }

    // --- DELETE /api/interest-area ---

    @Test
    @DisplayName("DELETE /api/interest-area - 인증된 요청은 200을 반환한다")
    void deleteInterestArea_authenticated_returns200() throws Exception {
        // given
        InterestAreaDeleteResponse response = new InterestAreaDeleteResponse(true, false);
        given(interestAreaService.deleteInterestArea(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/interest-area")
                        .with(jwt().jwt(builder -> builder.subject("1").claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keywordNotificationDisabled").value(false));
    }
}
