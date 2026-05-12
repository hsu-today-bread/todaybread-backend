package com.todaybread.server.domain.user.client;

import com.todaybread.server.domain.user.client.dto.NtsBusinessStatus;
import com.todaybread.server.domain.user.client.dto.NtsBusinessValidateRequest;
import com.todaybread.server.domain.user.client.dto.NtsBusinessValidateResponse;
import com.todaybread.server.domain.user.client.dto.NtsBusinessValidationResult;
import com.todaybread.server.domain.user.config.NtsBusinessProperties;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;

/**
 * 국세청 사업자등록정보 진위확인 API 클라이언트입니다.
 */
@Slf4j
@Component
public class NtsBusinessClient {

    private static final String VALID_MATCHED = "01";
    private static final String ACTIVE_BUSINESS = "01";

    private final NtsBusinessProperties properties;
    private final RestClient restClient;

    public NtsBusinessClient(NtsBusinessProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutOrDefault());
        requestFactory.setReadTimeout(properties.readTimeoutOrDefault());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    log.warn("국세청 사업자등록정보 API 오류: status={}", response.getStatusCode().value());
                    throw new CustomException(ErrorCode.USER_BOSS_VERIFICATION_UNAVAILABLE);
                })
                .build();
    }

    @PostConstruct
    void validateProperties() {
        if (isBlank(properties.serviceKey())) {
            log.warn("국세청 사업자등록정보 API serviceKey가 설정되지 않았습니다. "
                    + "환경 변수 NTS_BUSINESS_SERVICE_KEY를 설정해주세요.");
        }
        if (isBlank(properties.baseUrl())) {
            log.warn("국세청 사업자등록정보 API baseUrl이 설정되지 않았습니다.");
        }
    }

    /**
     * 사업자등록번호, 개업일자, 대표자명으로 국세청 진위확인을 수행합니다.
     *
     * @param bossNumber         사업자등록번호(숫자 10자리)
     * @param businessStartDate  개업일자(yyyyMMdd)
     * @param representativeName 대표자명
     * @return 계속사업자 검증 성공 결과
     */
    public NtsBusinessValidationResult validate(String bossNumber, String businessStartDate,
                                                String representativeName) {
        if (isBlank(properties.serviceKey()) || isBlank(properties.baseUrl())) {
            throw new CustomException(ErrorCode.USER_BOSS_VERIFICATION_UNAVAILABLE);
        }

        NtsBusinessValidateRequest requestBody =
                NtsBusinessValidateRequest.of(bossNumber, businessStartDate, representativeName);

        NtsBusinessValidateResponse response;
        try {
            response = restClient.post()
                    .uri(validateUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(NtsBusinessValidateResponse.class);
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("국세청 사업자등록정보 API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.USER_BOSS_VERIFICATION_UNAVAILABLE);
        }

        return toValidationResult(response);
    }

    private URI validateUri() {
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        return URI.create(baseUrl + "/validate?serviceKey=" + properties.serviceKey());
    }

    private NtsBusinessValidationResult toValidationResult(NtsBusinessValidateResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new CustomException(ErrorCode.USER_BOSS_VERIFICATION_UNAVAILABLE);
        }

        List<NtsBusinessValidateResponse.Result> data = response.data();
        NtsBusinessValidateResponse.Result result = data.getFirst();

        if (!VALID_MATCHED.equals(result.valid())) {
            throw new CustomException(ErrorCode.USER_BOSS_VERIFICATION_FAILED);
        }

        NtsBusinessStatus status = result.status();
        if (status == null || !ACTIVE_BUSINESS.equals(status.businessStatusCode())) {
            throw new CustomException(ErrorCode.USER_BOSS_NOT_ACTIVE_BUSINESS);
        }

        return new NtsBusinessValidationResult(status.businessStatusCode(), status.businessStatus());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
