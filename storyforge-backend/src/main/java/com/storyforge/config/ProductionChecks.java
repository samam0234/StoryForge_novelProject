package com.storyforge.config;

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
}
