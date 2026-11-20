package io.sourceripple.shared.api.spa;

import java.util.Set;

/** Identifies request paths that the single-page app renders client-side. */
public final class SpaRoutes {

    private static final Set<String> RESERVED_FIRST_SEGMENTS = Set.of(
            "api", "actuator", "v3", "swagger-ui", "internal", "mcp", "demo", "assets", "error");

    private SpaRoutes() {}

    public static boolean isSpaPage(String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') {
            return false;
        }
        String trimmed = path.substring(1);
        if (trimmed.isEmpty()) {
            return true;
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] segments = trimmed.split("/", -1);
        if (RESERVED_FIRST_SEGMENTS.contains(segments[0])) {
            return false;
        }
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return false;
            }
        }
        return !segments[segments.length - 1].contains(".");
    }
}
