package io.sourceripple.content.domain;

import java.util.List;

public record NormalizedDocument(SourceKind kind, String title, List<NormalizedSection> sections, String contentSha256) {

    public static NormalizedDocument of(SourceKind kind, String title, List<NormalizedSection> sections) {
        StringBuilder canonical = new StringBuilder();
        for (NormalizedSection section : sections) {
            canonical.append(section.key()).append('\t').append(section.sha256()).append('\n');
        }
        return new NormalizedDocument(kind, title, List.copyOf(sections), TextNormalizer.sha256(canonical.toString()));
    }

    public int textLength() {
        return sections.stream().mapToInt(section -> section.title().length() + section.text().length()).sum();
    }
}
