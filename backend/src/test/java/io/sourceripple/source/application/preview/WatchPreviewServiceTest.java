package io.sourceripple.source.application.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.sourceripple.content.application.ContentInspector;
import io.sourceripple.content.application.ExtractionRuleSetPool;
import io.sourceripple.content.application.SourceAdapterFactory;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.infrastructure.HtmlPageAdapter;
import io.sourceripple.content.infrastructure.RssFeedAdapter;
import io.sourceripple.shared.domain.TestFetchLimits;
import io.sourceripple.shared.infrastructure.RateLimiter;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.source.api.dto.WatchPreviewRequest;
import io.sourceripple.source.api.dto.WatchPreviewResponse;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.AddressPolicy;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchPreviewServiceTest {

    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private WebFetcher fetcher;

    private WatchPreviewService service;

    @BeforeEach
    void setUp() {
        SourceRippleProperties.Limit limit = new SourceRippleProperties.Limit(10, Duration.ofMinutes(1));
        SourceRippleProperties properties = new SourceRippleProperties(
                null, null, null, null, null, null, null, null, null,
                new SourceRippleProperties.RateLimit(limit, limit), null);
        service = new WatchPreviewService(
                fetcher,
                new UrlCanonicalizer(Set.of(80, 443), new AddressPolicy(false)),
                new SourceAdapterFactory(List.of(new HtmlPageAdapter(), new RssFeedAdapter()), new ExtractionRuleSetPool()),
                new ContentInspector(),
                TestFetchLimits.defaults(),
                new RateLimiter(Clock.systemUTC()),
                properties);
    }

    @Test
    void returnsCanonicalUrlKindTitleSectionsAndShortExcerpts() {
        String longText = "word ".repeat(100);
        String html = "<html><head><title>Docs</title></head><body><main><h2>Long</h2><p>" + longText
                + "</p></main></body></html>";
        when(fetcher.fetch(any())).thenReturn(ok("https://docs.example.com/", html));

        WatchPreviewResponse preview = service.preview(PROJECT, new WatchPreviewRequest("HTTPS://Docs.Example.com"), USER);

        assertThat(preview.canonicalUrl()).isEqualTo("https://docs.example.com/");
        assertThat(preview.kind()).isEqualTo(SourceKind.HTML);
        assertThat(preview.title()).isEqualTo("Docs");
        assertThat(preview.sections().getFirst().key()).isEqualTo("long");
        assertThat(preview.sections().getFirst().excerpt()).hasSizeLessThanOrEqualTo(201).endsWith("…");
        assertThat(preview.warnings()).isEmpty();
    }

    @Test
    void robotsDisallowBecomesAWarningInsteadOfAnError() {
        when(fetcher.fetch(any())).thenThrow(new RobotsDisallowedException("https://docs.example.com/"));

        WatchPreviewResponse preview = service.preview(PROJECT, new WatchPreviewRequest("https://docs.example.com/"), USER);

        assertThat(preview.warnings()).containsExactly("ROBOTS_DISALLOWED");
        assertThat(preview.sections()).isEmpty();
    }

    @Test
    void serverErrorsAreRetryableFailuresAndNotFoundIsNot() {
        when(fetcher.fetch(any())).thenReturn(status(503)).thenReturn(status(404));

        assertThatThrownBy(() -> service.preview(PROJECT, new WatchPreviewRequest("https://docs.example.com/"), USER))
                .isInstanceOfSatisfying(SourceFetchFailedException.class, ex -> assertThat(ex.retryable()).isTrue());
        assertThatThrownBy(() -> service.preview(PROJECT, new WatchPreviewRequest("https://docs.example.com/"), USER))
                .isInstanceOfSatisfying(SourceFetchFailedException.class, ex -> assertThat(ex.retryable()).isFalse());
    }

    private static FetchResult ok(String url, String html) {
        return new FetchResult(200, url, "text/html", html.getBytes(), null, null, null, null, html.length(), 5);
    }

    private static FetchResult status(int code) {
        return new FetchResult(code, "https://docs.example.com/", "text/html", new byte[0], null, null, null, null, 0, 5);
    }
}
