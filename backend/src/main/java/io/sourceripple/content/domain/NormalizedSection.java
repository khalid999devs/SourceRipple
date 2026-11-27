package io.sourceripple.content.domain;

public record NormalizedSection(String key, String title, String text, String sha256) {

    public static NormalizedSection of(String key, String title, String text) {
        return new NormalizedSection(key, title, text, TextNormalizer.sha256(title + "\n" + text));
    }
}
