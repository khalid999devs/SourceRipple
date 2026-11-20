package io.sourceripple.source.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.AddressPolicy;
import io.sourceripple.source.domain.CanonicalUrl;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RobotsPolicyServiceTest {

    private final UrlCanonicalizer canonicalizer = new UrlCanonicalizer(Set.of(80, 443), new AddressPolicy(false));

    @Mock
    private WebFetcher fetcher;

    private RobotsPolicyService robots;

    @BeforeEach
    void setUp() {
        robots = new RobotsPolicyService(fetcher, Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void disallowedPathIsRejectedAndRulesAreCachedPerHost() {
        when(fetcher.fetch(any())).thenReturn(robotsTxt("User-agent: SourceRippleBot\nDisallow: /private\n"));

        assertThatThrownBy(() -> robots.check(url("https://docs.example.com/private/keys")))
                .isInstanceOf(RobotsDisallowedException.class);
        assertThatCode(() -> robots.check(url("https://docs.example.com/payments"))).doesNotThrowAnyException();

        verify(fetcher, times(1)).fetch(any());
    }

    @Test
    void missingRobotsTxtAllowsEverything() {
        when(fetcher.fetch(any()))
                .thenReturn(new FetchResult(404, null, "text/html", new byte[0], null, null, null, null, 0, 1));

        assertThatCode(() -> robots.check(url("https://docs.example.com/anything"))).doesNotThrowAnyException();
    }

    @Test
    void unreachableRobotsTxtDoesNotBlock() {
        when(fetcher.fetch(any())).thenThrow(SourceFetchFailedException.retryable("timeout"));

        assertThatCode(() -> robots.check(url("https://docs.example.com/payments"))).doesNotThrowAnyException();
    }

    private CanonicalUrl url(String raw) {
        return canonicalizer.canonicalize(raw);
    }

    private static FetchResult robotsTxt(String body) {
        return new FetchResult(200, null, "text/plain", body.getBytes(), null, null, null, null, body.length(), 1);
    }
}
