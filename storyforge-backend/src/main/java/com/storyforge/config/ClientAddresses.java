package com.storyforge.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 요청 제한에 쓰는 클라이언트 주소.
 * 바로 앞 연결이 신뢰 목록에 있을 때만 X-Forwarded-For를 보고, 오른쪽부터 신뢰 주소가 아닌 첫 홉을 클라이언트로 봅니다.
 */
public final class ClientAddresses {

    private ClientAddresses() {
    }

    public static Set<String> parse(String raw) {
        Set<String> addresses = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(addresses::add);
        return Set.copyOf(addresses);
    }

    public static String resolve(HttpServletRequest request, Set<String> trustedProxies) {
        String remote = request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
        if (trustedProxies == null || trustedProxies.isEmpty() || !trustedProxies.contains(remote)) {
            return remote;
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header == null || header.isBlank()) {
            return remote;
        }
        String[] hops = header.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (hop.isEmpty() || trustedProxies.contains(hop)) {
                continue;
            }
            return hop;
        }
        return remote;
    }
}
