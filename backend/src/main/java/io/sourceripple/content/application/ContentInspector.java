package io.sourceripple.content.application;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.SourceKind;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class ContentInspector {

    static final int MIN_TEXT_CHARS = 200;
    private static final String SCRIPT_ROOTS = "#root, #app, #__next, #__nuxt, #svelte, [data-reactroot], app-root";

    public List<ContentWarning> warnings(
            NormalizedDocument document, byte[] body, Charset declaredCharset, long largePageBytes) {
        List<ContentWarning> warnings = new ArrayList<>();
        if (document.kind() == SourceKind.HTML && isLikelyJsRendered(document, body, declaredCharset)) {
            warnings.add(ContentWarning.LIKELY_JS_RENDERED);
        }
        if (body.length > largePageBytes) {
            warnings.add(ContentWarning.LARGE_PAGE);
        }
        return warnings;
    }

    public boolean isLikelyJsRendered(NormalizedDocument document, byte[] body, Charset declaredCharset) {
        if (document.textLength() >= MIN_TEXT_CHARS) {
            return false;
        }
        Document html = parse(body, declaredCharset);
        boolean hasScripts = !html.select("script").isEmpty();
        boolean emptyMountRoot = html.select(SCRIPT_ROOTS).stream().anyMatch(root -> root.text().isBlank());
        return hasScripts && emptyMountRoot;
    }

    private static Document parse(byte[] body, Charset declaredCharset) {
        try {
            return Jsoup.parse(
                    new ByteArrayInputStream(body), declaredCharset == null ? null : declaredCharset.name(), "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
