package com.todaybread.server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 및 실패 시,규격을 정하기 위한 이넘 클래스입니다.
 * 
 * 성공 (2xx) → 정상 DTO 그대로 사용
 * 실패 (4xx, 5xx) → ErrorResponse에서 에러 코드 읽고 분기
 *
 * [컨벤션]
 *  - 핸들러는 얇게 유지하고, 의미는 ErrorCode에서 구체적으로 표현합니다.
 *  - HTTP 상태코드는 큰 분류로 사용합니다.
 *  - code 필드는 도메인_번호(COMMON_001, USER_001, ...)를 사용합니다.
 *  - 영역_API형태_의미 순서로 네이밍합니다.
 *      - 공통 오류 인 경우... COMMON_ ....
 *      - 유저 도메인인 경우... USER_ ...
 *
 * [HTTP 상태코드 가이드]
 *  - 400 BAD_REQUEST: 요청값/형식/검증 오류
 *  - 401 UNAUTHORIZED: 인증 실패 또는 인증 필요
 *  - 403 FORBIDDEN: 인증은 되었지만 권한 없음
 *  - 404 NOT_FOUND: 대상 리소스 없음
 *  - 405 METHOD_NOT_ALLOWED: 허용되지 않은 HTTP 메서드
 *  - 409 CONFLICT: 중복/상태 충돌
 *  - 500 INTERNAL_SERVER_ERROR: 서버 내부 예외
 */
@Getter
public enum ErrorCode {

    /**
     * ============================
     * 공통 오류
     * ============================
     */
    COMMON_REQUEST_VALIDATION_FAILED("COMMON_001", "요청값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    COMMON_HTTP_METHOD_NOT_ALLOWED("COMMON_002", "허용되지 않은 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    COMMON_INTERNAL_SERVER_ERROR("COMMON_003", "서버 내부 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMON_ACCESS_DENIED("COMMON_004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    COMMON_FILE_SIZE_EXCEEDED("COMMON_005", "파일 크기는 5MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMON_IMAGE_INVALID_TYPE("COMMON_006", "허용되지 않는 파일 형식입니다. (jpeg, png, gif, webp만 가능)", HttpStatus.BAD_REQUEST),
    COMMON_IMAGE_STORAGE_FAILED("COMMON_007", "파일 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMON_DUPLICATE_CONFLICT("COMMON_008", "중복된 데이터가 존재합니다.", HttpStatus.CONFLICT),

    /**
     * ============================
     * 유저 오류
     * ============================
     */
    USER_REGISTER_EMAIL_ALREADY_EXISTS("USER_001", "이미 가입한 이메일입니다.", HttpStatus.CONFLICT),
    USER_REGISTER_PHONE_ALREADY_EXISTS("USER_002", "이미 가입한 전화번호입니다.", HttpStatus.CONFLICT),
    USER_REGISTER_NICKNAME_ALREADY_EXISTS("USER_003", "이미 사용중인 닉네임입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("USER_004", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_RECOVERY_NOT_FOUND("USER_005","가입 정보를 찾을 수 없습니다.",HttpStatus.NOT_FOUND),
    USER_BOSS_ALREADY_APPROVED("USER_006", "이미 사장님 등록이 완료된 상태입니다.", HttpStatus.CONFLICT),
    USER_BOSS_NUMBER_FORMAT_ERROR("USER_007","사업자 번호 형식이 맞지 않습니다.",HttpStatus.BAD_REQUEST),
    USER_RESET_TOKEN_INVALID("USER_008", "유효하지 않은 비밀번호 재설정 토큰입니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 키워드 오류
     * ============================
     */
    KEYWORD_ALREADY_EXISTS("KEYWORD_001", "이미 등록된 키워드입니다.", HttpStatus.CONFLICT),
    KEYWORD_LIMIT_EXCEEDED("KEYWORD_002", "키워드는 최대 5개까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),
    KEYWORD_LENGTH_LIMIT("KEYWORD_003", "키워드는 최대 10자까지 입력할 수 있습니다.", HttpStatus.BAD_REQUEST),
    KEYWORD_NOT_FOUND("KEYWORD_004", "키워드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    KEYWORD_FORBIDDEN("KEYWORD_005", "해당 키워드에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),

    /**
     * ============================
     * 가게 오류
     * ============================
     */
    STORE_BOSS_REQUIRED("STORE_001", "사장님 등록 후 이용 가능한 기능입니다.", HttpStatus.FORBIDDEN),
    STORE_ALREADY_EXISTS("STORE_002", "이미 등록된 가게가 있습니다.", HttpStatus.CONFLICT),
    STORE_PHONE_EXISTS("STORE_003", "가게 전화번호가 중복이 됩니다.",HttpStatus.CONFLICT),
    STORE_NOT_FOUND("STORE_004","가게를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STORE_BUSINESS_HOURS_INVALID("STORE_005", "영업시간 데이터가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    STORE_DAY_OF_WEEK_DUPLICATE("STORE_006", "요일 데이터가 중복됩니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 가게 이미지 오류
     * ============================
     */
    STORE_IMAGE_NOT_FOUND("STORE_IMAGE_001", "이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STORE_IMAGE_LIMIT_EXCEEDED("STORE_IMAGE_002", "이미지는 최대 5장까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 단골 가게 오류
     * ============================
     */
    FAVOURITE_STORE_LIMIT_EXCEEDED("FAVOURITE_STORE_001", "단골 가게는 최대 5개까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 음식 관련 오류
     * ============================
     */
    BREAD_NOT_FOUND("BREAD_001","상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BREAD_ACCESS_DENIED("BREAD_002","상품에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    BREAD_INSUFFICIENT_QUANTITY("BREAD_003","해당 상품의 재고가 부족합니다.",HttpStatus.CONFLICT),
    BREAD_INVALID_PRICE("BREAD_004", "가격은 0원 이상이여야 합니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * JWT 토큰 관련 오류
     * ============================
     */
    AUTH_ACCESS_TOKEN_EXPIRED("AUTH_001", "Access 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCESS_TOKEN_INVALID("AUTH_002", "유효하지 않은 Access 토큰입니다.", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_TOKEN_INVALID("AUTH_003", "유효하지 않은 Refresh 토큰입니다.", HttpStatus.UNAUTHORIZED),

    /**
     * ============================
     * 장바구니 오류
     * ============================
     */
    CART_SINGLE_STORE_ONLY("CART_001", "장바구니에는 하나의 매장 빵만 담을 수 있습니다.", HttpStatus.CONFLICT),
    CART_ITEM_NOT_FOUND("CART_002", "장바구니 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CART_EMPTY("CART_003", "장바구니가 비어 있습니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 주문 오류
     * ============================
     */
    ORDER_NOT_FOUND("ORDER_001", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED("ORDER_002", "주문에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    ORDER_STATUS_CANNOT_CHANGE("ORDER_003", "변경할 수 없는 주문 상태입니다.", HttpStatus.CONFLICT),
    ORDER_NUMBER_GENERATION_FAILED("ORDER_004", "주문 번호 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    /**
     * ============================
     * 결제 오류
     * ============================
     */
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_001", "결제 금액이 주문 금액과 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_ORDER_STATUS_INVALID("PAYMENT_003", "결제할 수 없는 주문 상태입니다.", HttpStatus.CONFLICT),
    PAYMENT_PROVIDER_ERROR("PAYMENT_004", "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_GATEWAY),
    PAYMENT_CANCEL_FAILED("PAYMENT_007", "결제 취소 처리 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    PAYMENT_IDEMPOTENCY_KEY_MISSING("PAYMENT_008", "Idempotency-Key 헤더가 필요합니다.", HttpStatus.BAD_REQUEST),

    /**
     * ============================
     * 리뷰 오류
     * ============================
     */
    REVIEW_PURCHASE_REQUIRED("REVIEW_001", "구매 이력이 없어 리뷰를 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REVIEW_ALREADY_EXISTS("REVIEW_002", "이미 해당 주문 항목에 대한 리뷰를 작성했습니다.", HttpStatus.CONFLICT),
    REVIEW_BREAD_NOT_AVAILABLE("REVIEW_003", "해당 상품이 삭제되어 리뷰를 작성할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REVIEW_IMAGE_LIMIT_EXCEEDED("REVIEW_005", "리뷰 이미지는 최대 2장까지 첨부할 수 있습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    private ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
