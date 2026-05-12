package com.todaybread.server.domain.user.client;

import com.todaybread.server.domain.user.client.dto.NtsBusinessValidationResult;
import com.todaybread.server.domain.user.config.NtsBusinessProperties;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NtsBusinessClientTest {

    private MockWebServer mockWebServer;
    private NtsBusinessClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/api/nts-businessman/v1").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        NtsBusinessProperties properties =
                new NtsBusinessProperties("test-key", baseUrl, 1_000, 1_000);
        client = new NtsBusinessClient(properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void validate_returnsResultWhenBusinessIsValidAndActive() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status_code": "OK",
                          "request_cnt": 1,
                          "match_cnt": 1,
                          "data": [
                            {
                              "b_no": "1234567890",
                              "valid": "01",
                              "valid_msg": "확인되었습니다.",
                              "status": {
                                "b_no": "1234567890",
                                "b_stt": "계속사업자",
                                "b_stt_cd": "01"
                              }
                            }
                          ]
                        }
                        """));

        NtsBusinessValidationResult result =
                client.validate("1234567890", "20200101", "홍길동");

        assertThat(result.businessStatusCode()).isEqualTo("01");
        assertThat(result.businessStatusName()).isEqualTo("계속사업자");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/nts-businessman/v1/validate?serviceKey=test-key");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8()).contains(
                "\"b_no\":\"1234567890\"",
                "\"start_dt\":\"20200101\"",
                "\"p_nm\":\"홍길동\""
        );
    }

    @Test
    void validate_rejectsInvalidBusinessInformation() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status_code": "OK",
                          "request_cnt": 1,
                          "match_cnt": 0,
                          "data": [
                            {
                              "b_no": "1234567890",
                              "valid": "02",
                              "valid_msg": "확인할 수 없습니다."
                            }
                          ]
                        }
                        """));

        assertThatThrownBy(() -> client.validate("1234567890", "20200101", "홍길동"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_VERIFICATION_FAILED);
    }

    @Test
    void validate_rejectsInactiveBusiness() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "status_code": "OK",
                          "request_cnt": 1,
                          "match_cnt": 1,
                          "data": [
                            {
                              "b_no": "1234567890",
                              "valid": "01",
                              "status": {
                                "b_stt": "폐업자",
                                "b_stt_cd": "03"
                              }
                            }
                          ]
                        }
                        """));

        assertThatThrownBy(() -> client.validate("1234567890", "20200101", "홍길동"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_NOT_ACTIVE_BUSINESS);
    }

    @Test
    void validate_mapsProviderErrorToUnavailable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> client.validate("1234567890", "20200101", "홍길동"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BOSS_VERIFICATION_UNAVAILABLE);
    }
}
