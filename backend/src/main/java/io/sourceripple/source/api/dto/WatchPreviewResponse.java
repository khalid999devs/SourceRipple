package io.sourceripple.source.api.dto;

import io.sourceripple.content.domain.SourceKind;
import java.util.List;

public record WatchPreviewResponse(
        String canonicalUrl, SourceKind kind, String title, List<SectionPreview> sections, List<String> warnings) {

    public record SectionPreview(String key, String title, String excerpt) {}
}
