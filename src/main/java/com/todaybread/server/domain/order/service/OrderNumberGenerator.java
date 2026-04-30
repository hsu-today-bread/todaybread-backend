package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * 주문 번호 생성기입니다.
 * O, I를 제외한 영숫자 32자 문자셋에서 4자리 주문 번호를 랜덤 생성합니다.
 * 같은 가게 + 같은 날짜 내에서 유일성을 보장합니다.
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    /** O, I 제외 A-Z(24자) + 2-9(8자) = 32자 문자셋 */
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 4;
    private static final int MAX_RETRIES = 10;

    private final OrderRepository orderRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 가게와 날짜 내에서 유일한 4자리 주문 번호를 생성합니다.
     *
     * @param storeId   가게 ID
     * @param orderDate 주문 날짜
     * @return 유일한 4자리 주문 번호
     * @throws CustomException 최대 재시도 초과 시 ORDER_NUMBER_GENERATION_FAILED 예외
     */
    public String generate(Long storeId, LocalDate orderDate) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String orderNumber = generateRandom();
            boolean exists = orderRepository.existsByStoreIdAndOrderDateAndOrderNumber(
                    storeId, orderDate, orderNumber);
            if (!exists) {
                return orderNumber;
            }
        }

        throw new CustomException(ErrorCode.ORDER_NUMBER_GENERATION_FAILED);
    }

    /**
     * 랜덤 4자리 주문 번호를 생성합니다.
     *
     * @return 랜덤 주문 번호
     */
    private String generateRandom() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARSET.charAt(secureRandom.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
