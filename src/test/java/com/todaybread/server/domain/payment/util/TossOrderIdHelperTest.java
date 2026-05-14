package com.todaybread.server.domain.payment.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TossOrderIdHelperTest {

    @Test
    void toTossOrderId_createsCurrentFormatWithinTossLengthLimit() {
        String tossOrderId = TossOrderIdHelper.toTossOrderId(
                5396L, "4f2a8e1c-9b6d-4c3a-8d00-123456789abc");

        assertThat(tossOrderId).isEqualTo("tb_5396_4f2a8e1c9b6d4c3a8d00123456789abc");
        assertThat(tossOrderId).hasSizeLessThanOrEqualTo(64);
        assertThat(TossOrderIdHelper.isCurrentFormat(tossOrderId)).isTrue();
    }

    @Test
    void fromTossOrderId_parsesCurrentFormat() {
        Long orderId = TossOrderIdHelper.fromTossOrderId("tb_5396_4f2a8e1c9b6d4c3a8d00123456789abc");

        assertThat(orderId).isEqualTo(5396L);
    }

    @Test
    void fromTossOrderId_parsesLegacyFormat() {
        Long orderId = TossOrderIdHelper.fromTossOrderId("order_5396");

        assertThat(orderId).isEqualTo(5396L);
        assertThat(TossOrderIdHelper.isCurrentFormat("order_5396")).isFalse();
    }

    @Test
    void matchesOrderIdempotencyKey_comparesWithNormalisedKey() {
        boolean matches = TossOrderIdHelper.matchesOrderIdempotencyKey(
                "tb_5396_4f2a8e1c9b6d4c3a8d00123456789abc",
                "4f2a8e1c-9b6d-4c3a-8d00-123456789abc");

        assertThat(matches).isTrue();
    }

    @Test
    void fromTossOrderId_rejectsInvalidFormat() {
        assertThatThrownBy(() -> TossOrderIdHelper.fromTossOrderId("5396"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
