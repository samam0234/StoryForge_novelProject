package com.storyforge.config;

import java.net.URI;

/**
 * 운영 프로필이 개발 기본값으로 뜨지 않게 하는 검사. SECURITY.md 배포 전 체크리스트.
 */
public final class ProductionChecks {

    private ProductionChecks() {
    }

    public static void verify(String dbUser, String dbPassword, String origin) {
        if (isBlank(dbUser) || "storyforge".equals(dbUser) || isBlank(dbPassword) || "storyforge".equals(dbPassword)) {
            throw new IllegalStateException("운영에서는 DB_USER와 DB_PASSWORD를 개발 기본값 storyforge가 아닌 값으로 지정해야 합니다.");
        }
        if (isBlank(origin) || origin.contains(",")) {
            throw new IllegalStateException("운영에서는 FRONTEND_ORIGIN을 실제 주소 하나로 지정해야 합니다.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static void verify(String dbUser, String dbPassword, String frontendOrigin, String consoleOrigin) {
        verify(dbUser, dbPassword, frontendOrigin);
        verifyOrigin(frontendOrigin, "FRONTEND_ORIGIN");
        verifyOrigin(consoleOrigin, "CONSOLE_ORIGIN");
    }

    private static void verifyOrigin(String value, String name) {
        try {
            URI uri = URI.create(value == null ? "" : value);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !uri.getRawPath().isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("운영에서는 " + name + "을 경로·와일드카드 없는 HTTPS 출처 하나로 지정해야 합니다.");
        }
    }
}
