package io.sourceripple.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.infrastructure.HtmlPageAdapter;
import io.sourceripple.support.Fixtures;
import org.junit.jupiter.api.Test;

class ContentInspectorTest {

    private final HtmlPageAdapter adapter = new HtmlPageAdapter();
    private final ContentInspector inspector = new ContentInspector();
    private final ExtractionRuleSetPool rules = new ExtractionRuleSetPool();

    @Test
    void flagsAScriptMountedShellWithAlmostNoText() {
        byte[] shell = Fixtures.read("js-shell.html");

        assertThat(inspector.warnings(adapt(shell), shell.length, 1024 * 1024))
                .containsExactly(ContentWarning.LIKELY_JS_RENDERED);
    }

    @Test
    void aServerRenderedPageHasNoWarningsUntilItIsLarge() {
        byte[] page = Fixtures.read("payments-v1.html");

        assertThat(inspector.warnings(adapt(page), page.length, 1024 * 1024)).isEmpty();
        assertThat(inspector.warnings(adapt(page), page.length, 100)).containsExactly(ContentWarning.LARGE_PAGE);
    }

    private NormalizedDocument adapt(byte[] body) {
        return adapter.adapt(body, null, "https://vendor.example/", rules.defaultFor(adapter.kind()));
    }
}
