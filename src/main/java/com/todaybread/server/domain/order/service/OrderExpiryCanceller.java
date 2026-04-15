package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderEntity;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import com.todaybread.server.domain.order.entity.OrderStatus;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 개별 만료 주문의 취소 및 재고 복원을 담당하는 서비스입니다.
 * 별도 클래스로 분리하여 Spring AOP 프록시를 통한 REQUIRES_NEW 트랜잭션이 정상 작동하도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpiryCanceller {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BreadRepository breadRepository;

    /**
     * 개별 만료 주문을 취소하고 재고를 복원합니다.
     * 별도 트랜잭션(REQUIRES_NEW)으로 실행되어 하나의 실패가 다른 주문 처리에 영향을 주지 않습니다.
     *
     * @param orderId 취소할 주문 ID
     * @return 취소 결과
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CancelResult cancelExpiredOrder(Long orderId) {
        Optional<OrderEntity> orderOptional = orderRepository.findByIdWithLock(orderId);

        if (orderOptional.isEmpty()) {
            log.warn("주문을 찾을 수 없어 건너뜀: orderId={}", orderId);
            return CancelResult.SKIPPED_NOT_FOUND;
        }

        OrderEntity order = orderOptional.get();

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("주문 상태 변경으로 건너뜀: orderId={}, status={}", orderId, order.getStatus());
            return CancelResult.SKIPPED_STATUS_CHANGED;
        }

        order.updateStatus(OrderStatus.CANCELLED);

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);

        List<Long> breadIds = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (breadIds.isEmpty()) {
            log.info("주문 취소 완료 (복원할 재고 없음): orderId={}", orderId);
            return CancelResult.CANCELLED;
        }

        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(breadIds);

        Map<Long, BreadEntity> breadMap = breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, Function.identity()));

        for (OrderItemEntity item : orderItems) {
            BreadEntity bread = breadMap.get(item.getBreadId());
            if (bread == null) {
                log.warn("빵을 찾을 수 없어 재고 복원 건너뜀: orderId={}, breadId={}", orderId, item.getBreadId());
                continue;
            }
            bread.increaseQuantity(item.getQuantity());
        }

        log.info("주문 취소 완료: orderId={}", orderId);
        return CancelResult.CANCELLED;
    }
}
