package io.sourceripple.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.sourceripple.content.application.ExtractionRuleSetPool;
import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.support.Fixtures;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RssFeedAdapterTest {

    private final RssFeedAdapter adapter = new RssFeedAdapter();
    private final ExtractionRuleSetPool rules = new ExtractionRuleSetPool();

    @Test
    void mapsItemsToSectionsKeyedByGuidThenLink() {
        NormalizedDocument feed = adapt(Fixtures.read("feed-v1.xml"));

        assertThat(feed.title()).isEqualTo("Vendor API Changelog");
        assertThat(feed.sections())
                .extracting(NormalizedSection::key)
                .containsExactly("changelog-2026-10-01", "https://vendor.example/changelog/nutrition");
        assertThat(feed.sections().getFirst().text()).isEqualTo("The Payments endpoint is generally available.");
    }

    @Test
    void aNewItemAddsOneSectionAndLeavesTheOthersUnchanged() {
        NormalizedDocument v1 = adapt(Fixtures.read("feed-v1.xml"));
        NormalizedDocument v2 = adapt(Fixtures.read("feed-v2.xml"));

        assertThat(v2.sections()).hasSize(3).containsAll(v1.sections());
        assertThat(v2.contentSha256()).isNotEqualTo(v1.contentSha256());
    }

    @Test
    void readsAtomEntriesById() {
        String atom = """
                <?xml version="1.0" encoding="utf-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Status</title>
                  <entry><id>urn:status:1</id><title>Degraded</title><summary>Slow payouts.</summary></entry>
                  <entry><title>No id</title><link href="https://status.example/2"/><summary>Fixed.</summary></entry>
                </feed>
                """;

        NormalizedDocument feed = adapt(atom.getBytes(StandardCharsets.UTF_8));

        assertThat(feed.title()).isEqualTo("Status");
        assertThat(feed.sections())
                .extracting(NormalizedSection::key)
                .containsExactly("urn:status:1", "https://status.example/2");
    }

    private NormalizedDocument adapt(byte[] body) {
        NormalizedDocument document = adapter.adapt(body, null, "https://vendor.example/", rules.defaultFor(SourceKind.RSS));
        assertThat(document.kind()).isEqualTo(SourceKind.RSS);
        return document;
    }
}
