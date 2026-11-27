package io.sourceripple.source.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.AddressPolicy;
import io.sourceripple.source.domain.CanonicalUrl;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RobotsPolicyServiceTest {

    private final UrlCanonicalizer canonicalizer = new UrlCanonicalizer(Set.of(80, 443), new AddressPolicy(false));

    @Mock
    private WebFetcher fetcher;

    @Mock
    private Clock clock;

    private RobotsPolicyService robots;
    private Instant now = Instant.parse("2026-11-20T10:00:00Z");

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenAnswer(invocation -> now);
        robots = new RobotsPolicyService(fetcher, clock);
    }

    @Test
    void disallowedPathIsRejectedAndRulesAreCachedPerHost() {
        when(fetcher.fetch(any())).thenReturn(robotsTxt("User-agent: SourceRippleBot\nDisallow: /private\n"));

        assertThatThrownBy(() -> check(url("https://docs.example.com/private/keys")))
                .isInstanceOf(RobotsDisallowedException.class);
        assertThatCode(() -> check(url("https://docs.example.com/payments"))).doesNotThrowAnyException();

        verify(fetcher, times(1)).fetch(any());
    }

    @Test
    void missingRobotsTxtAllowsEverything() {
        when(fetcher.fetch(any())).thenReturn(status(404));

        assertThatCode(() -> check(url("https://docs.example.com/anything"))).doesNotThrowAnyException();
    }

    @Test
    void unreachableRobotsTxtDoesNotBlock() {
        when(fetcher.fetch(any())).thenThrow(SourceFetchFailedException.retryable("timeout"));

        assertThatCode(() -> check(url("https://docs.example.com/payments"))).doesNotThrowAnyException();
    }

    @Test
    void aServerErrorDefersVisitsForAnHourAsARetryableFailure() {
        when(fetcher.fetch(any())).thenReturn(status(503));

        assertThatThrownBy(() -> check(url("https://docs.example.com/payments")))
                .isInstanceOfSatisfying(SourceFetchFailedException.class, ex -> assertThat(ex.retryable()).isTrue());
        assertThatThrownBy(() -> check(url("https://docs.example.com/payments")))
                .isInstanceOf(SourceFetchFailedException.class);
        verify(fetcher, times(1)).fetch(any());

        now = now.plus(Duration.ofMinutes(61));
        when(fetcher.fetch(any())).thenReturn(robotsTxt("User-agent: *\nAllow: /\n"));

        assertThatCode(() -> check(url("https://docs.example.com/payments"))).doesNotThrowAnyException();
        verify(fetcher, times(2)).fetch(any());
    }

    @Test
    void theRobotsFetchUsesTheCallersRemainingBudget() {
        when(fetcher.fetch(any())).thenReturn(robotsTxt("User-agent: *\nAllow: /\n"));

        robots.check(url("https://docs.example.com/payments"), Duration.ofSeconds(3));

        ArgumentCaptor<FetchRequest> request = ArgumentCaptor.forClass(FetchRequest.class);
        verify(fetcher).fetch(request.capture());
        assertThat(request.getValue().purpose()).isEqualTo(FetchPurpose.ROBOTS);
        assertThat(request.getValue().timeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void theCacheKeepsABoundedNumberOfOrigins() {
        when(fetcher.fetch(any())).thenReturn(status(404));

        for (int i = 0; i <= RobotsPolicyService.MAX_CACHED_ORIGINS; i++) {
            check(url("https://host-" + i + ".example.com/"));
        }

        assertThat(robots.cachedOrigins()).isEqualTo(RobotsPolicyService.MAX_CACHED_ORIGINS);
    }

    private void check(CanonicalUrl url) {
        robots.check(url, Duration.ofSeconds(15));
    }

    private CanonicalUrl url(String raw) {
        return canonicalizer.canonicalize(raw);
    }

    private static FetchResult status(int code) {
        return new FetchResult(code, null, "text/html", new byte[0], null, null, null, null, 0, 1);
    }

    private static FetchResult robotsTxt(String body) {
        return new FetchResult(200, null, "text/plain", body.getBytes(), null, null, null, null, body.length(), 1);
    }
}
