package io.sourceripple.content.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0\\u2007\\u202F]+");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private TextNormalizer() {}

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        String visible = ZERO_WIDTH.matcher(nfc).replaceAll("");
        return WHITESPACE.matcher(visible).replaceAll(" ").trim();
    }

    public static String slugify(String text) {
        String ascii = Normalizer.normalize(normalize(text), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String slug = NON_SLUG.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-");
        return slug.replaceAll("(^-+|-+$)", "");
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
