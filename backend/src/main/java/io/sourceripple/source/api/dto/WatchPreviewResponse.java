package io.sourceripple.source.api.dto;

import io.sourceripple.content.application.ContentWarning;
import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.content.domain.NormalizedSection;
import io.sourceripple.content.domain.SourceKind;
import java.util.List;

public record WatchPreviewResponse(
        String canonicalUrl,
        SourceKind kind,
        String title,
        List<SectionPreview> sections,
        List<ContentWarning> warnings) {

    public static WatchPreviewResponse of(
            String canonicalUrl, NormalizedDocument document, List<ContentWarning> warnings) {
        List<SectionPreview> sections = document.sections().stream().map(SectionPreview::from).toList();
        return new WatchPreviewResponse(canonicalUrl, document.kind(), document.title(), sections, warnings);
    }

    public static WatchPreviewResponse robotsDisallowed(String canonicalUrl) {
        return new WatchPreviewResponse(
                canonicalUrl, null, null, List.of(), List.of(ContentWarning.ROBOTS_DISALLOWED));
    }

    public record SectionPreview(String key, String title, String excerpt) {

        public static final int EXCERPT_CHARS = 200;

        public static SectionPreview from(NormalizedSection section) {
            String text = section.text();
            String excerpt = text.length() <= EXCERPT_CHARS ? text : text.substring(0, EXCERPT_CHARS).trim() + "…";
            return new SectionPreview(section.key(), section.title(), excerpt);
        }
    }
}
