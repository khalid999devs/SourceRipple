package io.sourceripple.source.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.sourceripple.support.AbstractIntegrationTest;
import io.sourceripple.support.Fixtures;
import jakarta.servlet.http.Cookie;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class WatchPreviewIT extends AbstractIntegrationTest {

    private final MockWebServer vendor = new MockWebServer();
    private Cookie maintainer;
    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        vendor.setDispatcher(new VendorSite());
        vendor.start(0);
        maintainer = registerAndLogin("MAINTAINER");
        projectId = createProject(maintainer);
    }

    @AfterEach
    void stop() {
        vendor.close();
    }

    @Test
    void htmlPreviewReturnsTheParametersSection() throws Exception {
        preview(maintainer, url("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("HTML"))
                .andExpect(jsonPath("$.title").value("Vendor Payments API"))
                .andExpect(jsonPath("$.sections[1].key").value("payments/parameters"))
                .andExpect(jsonPath("$.sections[1].excerpt").value(containsString("customer_id")))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    void rssPreviewReturnsOneSectionPerItem() throws Exception {
        preview(maintainer, url("/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("RSS"))
                .andExpect(jsonPath("$.sections.length()").value(2))
                .andExpect(jsonPath("$.sections[0].key").value("changelog-2026-10-01"));
    }

    @Test
    void scriptShellGetsTheJsRenderedWarning() throws Exception {
        preview(maintainer, url("/console"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value("LIKELY_JS_RENDERED"));
    }

    @Test
    void unsupportedAndBlockedUrlsAreSemanticErrors() throws Exception {
        preview(maintainer, url("/logo.png"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_CONTENT"));
        preview(maintainer, "ftp://vendor.example/payments")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("SOURCE_FETCH_BLOCKED"));
        preview(maintainer, url("/missing"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("SOURCE_FETCH_FAILED"));
    }

    @Test
    void robotsDisallowIsAWarning() throws Exception {
        preview(maintainer, url("/private/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings[0]").value("ROBOTS_DISALLOWED"));
    }

    @Test
    void onlyMaintainerMembersMayPreview() throws Exception {
        preview(registerAndLogin("MAINTAINER"), url("/payments")).andExpect(status().isNotFound());
        preview(registerAndLogin("REVIEWER"), url("/payments")).andExpect(status().isForbidden());
    }

    @Test
    void eleventhPreviewInAMinuteIsRateLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            preview(maintainer, url("/payments")).andExpect(status().isOk());
        }

        preview(maintainer, url("/payments"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));
    }

    private ResultActions preview(Cookie session, String target) throws Exception {
        return mvc.perform(post("/api/v1/projects/" + projectId + "/watches/preview")
                .with(xsrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"" + target + "\"}"));
    }

    private String url(String path) {
        return vendor.url(path).toString();
    }

    private String createProject(Cookie owner) throws Exception {
        String organizationId = JsonPath.read(
                mvc.perform(post("/api/v1/organizations")
                                .with(xsrf())
                                .cookie(owner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Vendor watchers\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        return JsonPath.read(
                mvc.perform(post("/api/v1/organizations/" + organizationId + "/projects")
                                .with(xsrf())
                                .cookie(owner)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Checkout\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }

    private static final class VendorSite extends Dispatcher {

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return switch (request.getUrl().encodedPath()) {
                case "/robots.txt" -> text("text/plain", "User-agent: *\nDisallow: /private\n");
                case "/payments" -> fixture("text/html; charset=utf-8", "payments-v1.html");
                case "/feed.xml" -> fixture("application/rss+xml", "feed-v1.xml");
                case "/console" -> fixture("text/html", "js-shell.html");
                case "/logo.png" -> text("image/png", "png");
                default -> new MockResponse.Builder().code(404).build();
            };
        }

        private static MockResponse fixture(String contentType, String name) {
            return new MockResponse.Builder()
                    .addHeader("Content-Type", contentType)
                    .body(new Buffer().write(Fixtures.read(name)))
                    .build();
        }

        private static MockResponse text(String contentType, String body) {
            return new MockResponse.Builder().addHeader("Content-Type", contentType).body(body).build();
        }
    }
}
