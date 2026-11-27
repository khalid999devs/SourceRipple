package io.sourceripple.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.sourceripple.content.application.ExtractionRuleSetPool;
import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.support.Fixtures;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HtmlPageAdapterTest {

    private final HtmlPageAdapter adapter = new HtmlPageAdapter();
    private final ExtractionRuleSetPool rules = new ExtractionRuleSetPool();

    @Test
    void producesTheSameStableKeysOnEveryRun() {
        NormalizedDocument first = adapt(Fixtures.read("payments-v1.html"));
        NormalizedDocument second = adapt(Fixtures.read("payments-v1.html"));

        assertThat(keys(first))
                .containsExactly("payments", "payments/parameters", "payments/responses", "payments/errors");
        assertThat(second).isEqualTo(first);
        assertThat(first.title()).isEqualTo("Vendor Payments API");
    }

    @Test
    void navHeaderFooterAndScriptEditsLeaveEveryHashUnchanged() {
        NormalizedDocument original = adapt(Fixtures.read("payments-v1.html"));
        NormalizedDocument chromeOnly = adapt(Fixtures.read("nav-footer-only-change.html"));

        assertThat(chromeOnly.sections()).isEqualTo(original.sections());
        assertThat(chromeOnly.contentSha256()).isEqualTo(original.contentSha256());
    }

    @Test
    void chromeInsideTheMainElementIsRemovedToo() {
        NormalizedDocument original = adapt(Fixtures.read("payments-v1.html"));

        NormalizedDocument chromeInside = adapt(Fixtures.read("chrome-inside-main.html"));

        assertThat(chromeInside.sections()).isEqualTo(original.sections());
        assertThat(chromeInside.sections())
                .allSatisfy(section -> assertThat(section.title() + section.text()).doesNotContain("CHROME"));
    }

    @Test
    void onlyTheParametersSectionChangesBetweenV1AndV2() {
        Map<String, String> v1 = hashes(adapt(Fixtures.read("payments-v1.html")));
        Map<String, String> v2 = hashes(adapt(Fixtures.read("payments-v2.html")));

        assertThat(v1.keySet()).isEqualTo(v2.keySet());
        assertThat(v1.keySet().stream().filter(key -> !v1.get(key).equals(v2.get(key))))
                .containsExactly("payments/parameters");
    }

    @Test
    void serializesTablesAsCellRows() {
        NormalizedSection parameters = section(adapt(Fixtures.read("payments-v1.html")), "payments/parameters");

        assertThat(parameters.text())
                .isEqualTo("Name | Type | Required\ncustomer_id | string | yes\namount | integer (cents) | yes");
    }

    @Test
    void usesHeadingIdsAnchorsIntroAndDuplicateSuffixes() {
        String html = """
                <html><body><main>
                <p>Before any heading.</p>
                <h2 id="auth">Authentication</h2><p>Use a key.</p>
                <h2><a name="limits"></a>Rate limits</h2><p>10 per second.</p>
                <h2>Notes</h2><p>One.</p>
                <h2>Notes</h2><p>Two.</p>
                </main></body></html>
                """;

        NormalizedDocument document = adapt(html.getBytes(StandardCharsets.UTF_8));

        assertThat(keys(document)).containsExactly("_intro", "auth", "limits", "notes", "notes-2");
        assertThat(section(document, "_intro").text()).isEqualTo("Before any heading.");
    }

    @Test
    void stripsZeroWidthCharactersAndCollapsesWhitespace() {
        String html = "<html><body><main><h2>Fees</h2><p>Flat​   fee \n of 2%</p></main></body></html>";

        assertThat(section(adapt(html.getBytes(StandardCharsets.UTF_8)), "fees").text()).isEqualTo("Flat fee of 2%");
    }

    @Test
    void inlineTextKeepsItsOrderAndLooseTextIsKept() {
        String html = """
                <html><body><main>
                Loose intro text.
                <h2>Usage</h2>
                <div>Use the <code>amount</code> field.</div>
                Then <em>send</em> the request.
                </main></body></html>
                """;

        NormalizedDocument document = adapt(html.getBytes(StandardCharsets.UTF_8));

        assertThat(section(document, "_intro").text()).isEqualTo("Loose intro text.");
        assertThat(section(document, "usage").text()).isEqualTo("Use the amount field.\nThen send the request.");
    }

    @Test
    void flagsAnEmptyScriptMountRoot() {
        assertThat(adapt(Fixtures.read("js-shell.html")).scriptMounted()).isTrue();
        assertThat(adapt(Fixtures.read("payments-v1.html")).scriptMounted()).isFalse();
    }

    private NormalizedDocument adapt(byte[] body) {
        NormalizedDocument document =
                adapter.adapt(body, null, "https://vendor.example/payments", rules.defaultFor(SourceKind.HTML));
        assertThat(document.kind()).isEqualTo(SourceKind.HTML);
        return document;
    }

    private static List<String> keys(NormalizedDocument document) {
        return document.sections().stream().map(NormalizedSection::key).toList();
    }

    private static Map<String, String> hashes(NormalizedDocument document) {
        return document.sections().stream().collect(Collectors.toMap(NormalizedSection::key, NormalizedSection::sha256));
    }

    private static NormalizedSection section(NormalizedDocument document, String key) {
        return document.sections().stream()
                .collect(Collectors.toMap(NormalizedSection::key, Function.identity()))
                .get(key);
    }
}
