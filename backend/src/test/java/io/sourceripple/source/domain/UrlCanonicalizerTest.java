package io.sourceripple.source.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.source.application.fetch.failure.SourceFetchBlockedException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class UrlCanonicalizerTest {

    private final UrlCanonicalizer canonicalizer =
            new UrlCanonicalizer(Set.of(80, 443, 8080, 8443), new AddressPolicy(false));

    @ParameterizedTest
    @CsvSource({
        "HTTPS://Docs.Example.COM:443/api/Payments?b=2&a=1#parameters, https://docs.example.com/api/Payments?b=2&a=1",
        "http://example.com, http://example.com/",
        "http://example.com:80/x, http://example.com/x",
        "https://example.com:8443/x, https://example.com:8443/x",
        "http://example.com.:8080, http://example.com:8080/",
        "  https://bücher.example/  , https://xn--bcher-kva.example/"
    })
    void normalizesHostPortAndFragmentButKeepsTheQuery(String raw, String expected) {
        assertThat(canonicalizer.canonicalize(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ftp://example.com/file",
        "javascript:alert(1)",
        "file:///etc/passwd",
        "https://user:secret@example.com/",
        "https://example.com:22/",
        "#parameters",
        "/relative/path",
        "http://10.0.0.1/",
        "http://192.168.1.10:8080/",
        "http://127.0.0.1/",
        "http://169.254.169.254/latest/meta-data",
        "http://100.64.0.1/",
        "http://2130706433/",
        "http://[::1]/",
        "http://[fd00::1]/",
        "http://[::ffff:10.0.0.1]/",
        "http://0.0.0.0/",
        ""
    })
    void rejectsBadSchemesUserinfoPortsAndPrivateIpLiterals(String raw) {
        assertThatThrownBy(() -> canonicalizer.canonicalize(raw)).isInstanceOf(SourceFetchBlockedException.class);
    }

    @Test
    void privateLiteralsAreAllowedOnlyWhenTheFlagIsOn() {
        UrlCanonicalizer permissive = new UrlCanonicalizer(Set.of(80, 443, 8080, 8443), new AddressPolicy(true));

        CanonicalUrl url = permissive.canonicalize("http://127.0.0.1:8080/demo/vendor/payments");

        assertThat(url.value()).isEqualTo("http://127.0.0.1:8080/demo/vendor/payments");
        assertThat(url.host()).isEqualTo("127.0.0.1");
        assertThat(url.port()).isEqualTo(8080);
        assertThat(url.origin()).isEqualTo("http://127.0.0.1:8080");
    }

    @Test
    void publicIpLiteralsAreAllowed() {
        assertThat(canonicalizer.canonicalize("https://93.184.215.14/").host()).isEqualTo("93.184.215.14");
    }
}
