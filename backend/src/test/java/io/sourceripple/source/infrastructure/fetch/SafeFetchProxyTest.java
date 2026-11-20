package io.sourceripple.source.infrastructure.fetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.shared.domain.TestFetchLimits;
import io.sourceripple.source.application.RobotsPolicyService;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchBlockedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.AddressPolicy;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SafeFetchProxyTest {

    @Mock
    private WebFetcher delegate;

    @Mock
    private RobotsPolicyService robots;

    private SafeFetchProxy proxy;

    @BeforeEach
    void setUp() {
        UrlCanonicalizer canonicalizer =
                new UrlCanonicalizer(Set.of(80, 443, 8080, 8443), new AddressPolicy(false));
        proxy = new SafeFetchProxy(delegate, canonicalizer, robots, TestFetchLimits.defaults());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1/admin",
        "http://10.0.0.5/",
        "http://192.168.1.1:8080/",
        "http://169.254.169.254/latest/meta-data/",
        "http://[::1]/",
        "http://[fd00::5]/"
    })
    void blocksLoopbackPrivateAndMetadataTargetsWithoutCallingTheDelegate(String url) {
        assertThatThrownBy(() -> proxy.fetch(FetchRequest.get(url, FetchPurpose.PREVIEW)))
                .isInstanceOf(SourceFetchBlockedException.class);

        verify(delegate, never()).fetch(any());
    }

    @Test
    void blocksARedirectToTheMetadataAddressAfterTheFirstHop() {
        when(delegate.fetch(any())).thenReturn(redirect("http://169.254.169.254/latest/meta-data/"));

        assertThatThrownBy(() -> proxy.fetch(FetchRequest.get("https://docs.example.com/", FetchPurpose.PREVIEW)))
                .isInstanceOf(SourceFetchBlockedException.class);

        verify(delegate, times(1)).fetch(any());
    }

    @Test
    void followsRedirectsAndReportsTheFinalUrl() {
        when(delegate.fetch(any()))
                .thenReturn(redirect("/v2/payments"))
                .thenReturn(ok());

        FetchResult result = proxy.fetch(FetchRequest.get("https://docs.example.com/payments", FetchPurpose.PREVIEW));

        assertThat(result.finalUrl()).isEqualTo("https://docs.example.com/v2/payments");
        verify(robots, times(2)).check(any());
    }

    @Test
    void stopsAfterThreeRedirects() {
        when(delegate.fetch(any())).thenReturn(redirect("https://docs.example.com/loop"));

        assertThatThrownBy(() -> proxy.fetch(FetchRequest.get("https://docs.example.com/", FetchPurpose.PREVIEW)))
                .isInstanceOf(SourceFetchFailedException.class);

        verify(delegate, times(4)).fetch(any());
    }

    @Test
    void robotsDisallowStopsTheFetchBeforeTheDelegate() {
        doThrow(new RobotsDisallowedException("https://docs.example.com/private")).when(robots).check(any());

        assertThatThrownBy(() -> proxy.fetch(
                        FetchRequest.get("https://docs.example.com/private", FetchPurpose.SCHEDULED)))
                .isInstanceOf(RobotsDisallowedException.class);

        verify(delegate, never()).fetch(any());
    }

    @Test
    void theRobotsRequestItselfSkipsTheRobotsCheck() {
        when(delegate.fetch(any())).thenReturn(ok());

        proxy.fetch(FetchRequest.get("https://docs.example.com/robots.txt", FetchPurpose.ROBOTS));

        verify(robots, never()).check(any());
    }

    private static FetchResult redirect(String location) {
        return new FetchResult(302, null, null, new byte[0], null, null, location, null, 0, 1);
    }

    private static FetchResult ok() {
        return new FetchResult(200, null, "text/html", "<p>ok</p>".getBytes(), null, null, null, null, 9, 1);
    }
}
