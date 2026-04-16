package com.todaybread.server.domain.order.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.order.entity.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryRestorer {

    private final BreadRepository breadRepository;

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
