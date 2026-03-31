package com.todaybread.server.global.util;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 인증된 JWT에서 프로젝트에서 자주 쓰는 값을 읽는 헬퍼입니다.
 */
public final class JwtRoleHelper {

    private static final String BOSS_ROLE = "BOSS";

    private JwtRoleHelper() {
    }

    /**
     * 인증된 JWT에서 유저 ID를 추출합니다.
     *
     * @param jwt 인증된 JWT 토큰
     * @return 유저 ID
     */
    public static Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    /**
     * 인증된 JWT에서 사장님 여부를 확인합니다.
     *
     * @param jwt 인증된 JWT 토큰
     * @return 사장님이면 true, 아니면 false
     */
    public static boolean isBoss(Jwt jwt) {
        return BOSS_ROLE.equals(jwt.getClaimAsString("role"));
    }
}
