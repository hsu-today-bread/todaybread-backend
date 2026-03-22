package com.todaybread.server.config.jwt;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 인증된 JWT에서 프로젝트에서 자주 쓰는 값을 읽는 헬퍼입니다.
 */
public final class JwtRoleHelper {

    private static final String BOSS_ROLE = "BOSS";

    private JwtRoleHelper() {
    }

    public static Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public static boolean isBoss(Jwt jwt) {
        return BOSS_ROLE.equals(jwt.getClaimAsString("role"));
    }
}
