package io.sourceripple.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.infrastructure.HtmlPageAdapter;
import io.sourceripple.content.infrastructure.RssFeedAdapter;
import io.sourceripple.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceAdapterFactoryTest {

    private final SourceAdapterFactory factory = new SourceAdapterFactory(
            List.of(new HtmlPageAdapter(), new RssFeedAdapter()), new ExtractionRuleSetPool());

    @Test
    void picksTheAdapterFromTheContentType() {
        assertThat(factory.select("text/html", new byte[0]).kind()).isEqualTo(SourceKind.HTML);
        assertThat(factory.select("application/xhtml+xml", new byte[0]).kind()).isEqualTo(SourceKind.HTML);
        assertThat(factory.select("application/rss+xml", new byte[0]).kind()).isEqualTo(SourceKind.RSS);
        assertThat(factory.select("application/atom+xml", new byte[0]).kind()).isEqualTo(SourceKind.RSS);
    }

    @Test
    void sniffsTheBodyWhenTheTypeIsGenericOrMissing() {
        assertThat(factory.select("application/xml", Fixtures.read("feed-v1.xml")).kind())
                .isEqualTo(SourceKind.RSS);
        assertThat(factory.select("text/xml", "<feed xmlns=\"http://www.w3.org/2005/Atom\">".getBytes()).kind())
                .isEqualTo(SourceKind.RSS);
        assertThat(factory.select(null, Fixtures.read("payments-v1.html")).kind()).isEqualTo(SourceKind.HTML);
    }

    @Test
    void rejectsContentThatIsNeitherHtmlNorAFeed() {
        assertThatThrownBy(() -> factory.select("application/xml", "<invoice><total>3</total></invoice>".getBytes()))
                .isInstanceOf(UnsupportedContentException.class);
    }

    @Test
    void adaptsWithTheSharedDefaultRules() {
        assertThat(factory.adapt("text/html", Fixtures.read("payments-v1.html"), null, "https://vendor.example/")
                        .kind())
                .isEqualTo(SourceKind.HTML);
    }
}
