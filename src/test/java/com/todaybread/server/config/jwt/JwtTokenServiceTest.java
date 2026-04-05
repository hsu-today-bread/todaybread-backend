package com.todaybread.server.config.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAndParseRefreshToken_roundTripsUserId() {
        JwtTokenService service = new JwtTokenService(SECRET, 60_000L, 120_000L);

        String token = service.generateRefreshToken(7L);

        assertThat(service.parseRefreshToken(token)).isEqualTo(7L);
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtTokenService("short-secret", 60_000L, 120_000L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseRefreshToken_rejectsInvalidToken() {
        JwtTokenService service = new JwtTokenService(SECRET, 60_000L, 120_000L);

        assertThatThrownBy(() -> service.parseRefreshToken("not-a-jwt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
