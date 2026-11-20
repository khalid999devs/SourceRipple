package io.sourceripple.content.infrastructure;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceAdapter;
import io.sourceripple.content.domain.SourceKind;
import io.sourceripple.content.domain.TextNormalizer;
import io.sourceripple.content.domain.rules.ExtractionRuleSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

@Component
public class RssFeedAdapter implements SourceAdapter {

    @Override
    public SourceKind kind() {
        return SourceKind.RSS;
    }

    @Override
    public NormalizedDocument adapt(byte[] body, Charset declaredCharset, String baseUrl, ExtractionRuleSet rules) {
        Document feed = parse(body, declaredCharset, baseUrl);
        boolean atom = feed.selectFirst("feed") != null && feed.selectFirst("rss, channel") == null;
        String title = TextNormalizer.normalize(text(feed.selectFirst(atom ? "feed > title" : "channel > title")));
        List<NormalizedSection> sections = new ArrayList<>();
        Map<String, Integer> usedKeys = new HashMap<>();
        for (Element entry : feed.select(atom ? "entry" : "item")) {
            String key = atom ? atomKey(entry) : rssKey(entry);
            if (key.isEmpty()) {
                key = TextNormalizer.slugify(text(entry.selectFirst("title")));
            }
            int seen = usedKeys.merge(key, 1, Integer::sum);
            String entryTitle = TextNormalizer.normalize(text(entry.selectFirst("title")));
            String content = atom
                    ? firstText(entry, "summary", "content")
                    : firstText(entry, "description", "content|encoded", "encoded");
            sections.add(NormalizedSection.of(seen == 1 ? key : key + "-" + seen, entryTitle, plainText(content)));
        }
        return NormalizedDocument.of(SourceKind.RSS, title, sections);
    }

    private static Document parse(byte[] body, Charset declaredCharset, String baseUrl) {
        try {
            return Jsoup.parse(
                    new ByteArrayInputStream(body),
                    declaredCharset == null ? null : declaredCharset.name(),
                    baseUrl == null ? "" : baseUrl,
                    Parser.xmlParser());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String rssKey(Element item) {
        String guid = text(item.selectFirst("guid"));
        return guid.isEmpty() ? text(item.selectFirst("link")) : guid;
    }

    private static String atomKey(Element entry) {
        String id = text(entry.selectFirst("id"));
        if (!id.isEmpty()) {
            return id;
        }
        Element link = entry.selectFirst("link[href]");
        return link == null ? "" : link.attr("href").trim();
    }

    private static String firstText(Element entry, String... selectors) {
        for (String selector : selectors) {
            Element element = entry.selectFirst(selector);
            if (element != null && !element.text().isBlank()) {
                return element.text();
            }
        }
        return "";
    }

    // Feed descriptions often carry escaped HTML; only its text is kept.
    private static String plainText(String content) {
        return TextNormalizer.normalize(Jsoup.parse(content).text());
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().trim();
    }
}
