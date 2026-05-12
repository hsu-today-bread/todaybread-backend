package com.todaybread.server.domain.user.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessNumberHasherTest {

    @Test
    void hash_isDeterministicAndDoesNotExposeOriginalNumber() {
        BusinessNumberHasher hasher = new BusinessNumberHasher("secret");

        String hash = hasher.hash("1234567890");

        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(hasher.hash("1234567890"));
        assertThat(hash).doesNotContain("1234567890");
    }

    @Test
    void last4_returnsLastFourDigits() {
        BusinessNumberHasher hasher = new BusinessNumberHasher("secret");

        assertThat(hasher.last4("1234567890")).isEqualTo("7890");
    }
}
