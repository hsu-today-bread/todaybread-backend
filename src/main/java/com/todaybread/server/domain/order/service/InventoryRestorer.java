package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 취소 시 재고 복원을 담당하는 공통 컴포넌트입니다.
 * 사용자 취소({@link OrderService})와 만료 취소({@link OrderExpiryCanceller}) 양쪽에서 사용합니다.
 *
 * <p>비관적 락으로 빵 엔티티를 조회한 뒤 주문 항목 수량만큼 재고를 증가시킵니다.
 * 호출자가 반드시 활성 트랜잭션을 열어야 합니다({@code MANDATORY} 전파).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryRestorer {

    private final BreadRepository breadRepository;

    /**
     * 주문 항목에 해당하는 빵의 재고를 복원합니다.
     * 비관적 락으로 빵을 조회하여 동시 주문과의 재고 충돌을 방지합니다.
     *
     * @param orderId    주문 ID (로깅용)
     * @param orderItems 재고를 복원할 주문 항목 목록
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreInventory(Long orderId, List<OrderItemEntity> orderItems) {
        List<Long> breadIds = orderItems.stream()
                .map(OrderItemEntity::getBreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (breadIds.isEmpty()) {
            return;
        }

        List<BreadEntity> breads = breadRepository.findAllByIdWithLock(breadIds);
        Map<Long, BreadEntity> breadMap = breads.stream()
                .collect(Collectors.toMap(BreadEntity::getId, Function.identity()));

        for (OrderItemEntity item : orderItems) {
            if (item.getBreadId() == null) continue;
            BreadEntity bread = breadMap.get(item.getBreadId());
            if (bread == null) {
                log.warn("빵을 찾을 수 없어 재고 복원 건너뜀: orderId={}, breadId={}", orderId, item.getBreadId());
                continue;
            }
            bread.increaseQuantity(item.getQuantity());
        }
    }
}
