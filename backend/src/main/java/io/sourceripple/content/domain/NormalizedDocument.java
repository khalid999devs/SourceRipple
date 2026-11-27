package io.sourceripple.content.domain;

import java.util.List;

public record NormalizedDocument(
        SourceKind kind,
        String title,
        List<NormalizedSection> sections,
        String contentSha256,
        boolean scriptMounted) {

    public static NormalizedDocument of(SourceKind kind, String title, List<NormalizedSection> sections) {
        return of(kind, title, sections, false);
    }

    public static NormalizedDocument of(
            SourceKind kind, String title, List<NormalizedSection> sections, boolean scriptMounted) {
        StringBuilder canonical = new StringBuilder();
        for (NormalizedSection section : sections) {
            canonical.append(section.key()).append('\t').append(section.sha256()).append('\n');
        }

        String contentSha256 = TextNormalizer.sha256(canonical.toString());
        return new NormalizedDocument(kind, title, List.copyOf(sections), contentSha256, scriptMounted);
    }

    public int textLength() {
        return sections.stream().mapToInt(section -> section.title().length() + section.text().length()).sum();
    }
}
