package io.sourceripple.content.application;

import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.SourceKind;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ContentInspector {

    static final int MIN_TEXT_CHARS = 200;

    public List<ContentWarning> warnings(NormalizedDocument document, long bodyBytes, long largePageBytes) {
        List<ContentWarning> warnings = new ArrayList<>();
        if (isLikelyJsRendered(document)) {
            warnings.add(ContentWarning.LIKELY_JS_RENDERED);
        }
        if (bodyBytes > largePageBytes) {
            warnings.add(ContentWarning.LARGE_PAGE);
        }
        return warnings;
    }

    public boolean isLikelyJsRendered(NormalizedDocument document) {
        return document.kind() == SourceKind.HTML
                && document.scriptMounted()
                && document.textLength() < MIN_TEXT_CHARS;
    }
}
