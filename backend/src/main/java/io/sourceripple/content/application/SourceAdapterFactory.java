package io.sourceripple.content.application;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.SourceAdapter;
import io.sourceripple.content.domain.SourceKind;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SourceAdapterFactory {

    private static final Set<String> HTML_TYPES = Set.of("text/html", "application/xhtml+xml");
    private static final Set<String> FEED_TYPES = Set.of("application/rss+xml", "application/atom+xml");
    private static final int SNIFF_BYTES = 1024;

    private final Map<SourceKind, SourceAdapter> adapters = new EnumMap<>(SourceKind.class);
    private final ExtractionRuleSetPool rules;

    public SourceAdapterFactory(List<SourceAdapter> adapters, ExtractionRuleSetPool rules) {
        adapters.forEach(adapter -> this.adapters.put(adapter.kind(), adapter));
        this.rules = rules;
    }

    public SourceAdapter select(String mediaType, byte[] body) {
        return adapters.get(detect(mediaType, body));
    }

    public NormalizedDocument adapt(String mediaType, byte[] body, Charset declaredCharset, String baseUrl) {
        SourceAdapter adapter = select(mediaType, body);
        return adapter.adapt(body, declaredCharset, baseUrl, rules.defaultFor(adapter.kind()));
    }

    static SourceKind detect(String mediaType, byte[] body) {
        String type = mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT);
        if (HTML_TYPES.contains(type)) {
            return SourceKind.HTML;
        }
        if (FEED_TYPES.contains(type)) {
            return SourceKind.RSS;
        }

        String head = new String(body, 0, Math.min(body.length, SNIFF_BYTES), StandardCharsets.ISO_8859_1)
                .toLowerCase(Locale.ROOT);
        if (head.contains("<rss") || head.contains("<feed") || head.contains("<rdf:rdf")) {
            return SourceKind.RSS;
        }
        if (head.contains("<!doctype html") || head.contains("<html")) {
            return SourceKind.HTML;
        }
        throw new UnsupportedContentException("This content is neither an HTML page nor an RSS or Atom feed.");
    }
}
