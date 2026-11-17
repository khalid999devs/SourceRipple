package io.sourceripple.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpaRoutesTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/pricing", "/login", "/app", "/app/", "/app/projects/42", "/unknown-route"})
    void clientRoutesAreSpaPages(String path) {
        assertThat(SpaRoutes.isSpaPage(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/projects",
                "/actuator/health",
                "/v3/api-docs",
                "/internal/scheduler/tick",
                "/mcp",
                "/demo/vendor/payments",
                "/assets/index.js",
                "/favicon.svg",
                "/app/report.pdf",
                "//double",
                ""
            })
    void serverAndFilePathsAreNotSpaPages(String path) {
        assertThat(SpaRoutes.isSpaPage(path)).isFalse();
    }
}
