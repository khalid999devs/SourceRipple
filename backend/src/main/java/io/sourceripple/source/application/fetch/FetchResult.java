package io.sourceripple.source.application.fetch;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record FetchResult(
        int status,
        String finalUrl,
        String contentType,
        byte[] body,
        String etag,
        String lastModified,
        String location,
        String retryAfter,
        long bytes,
        long durationMs) {

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public boolean isNotModified() {
        return status == 304;
    }

    public boolean isRedirect() {
        return (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) && location != null;
    }

    public String mediaType() {
        if (contentType == null) {
            return null;
        }
        int semicolon = contentType.indexOf(';');
        return (semicolon < 0 ? contentType : contentType.substring(0, semicolon)).trim().toLowerCase(Locale.ROOT);
    }

    public Charset charset() {
        if (contentType != null) {
            for (String part : contentType.split(";")) {
                String trimmed = part.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    try {
                        return Charset.forName(trimmed.substring(8).replace("\"", "").trim());
                    } catch (IllegalArgumentException ignored) {
                        return StandardCharsets.UTF_8;
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    public FetchResult withFinalUrl(String url, long totalDurationMs) {
        return new FetchResult(
                status, url, contentType, body, etag, lastModified, location, retryAfter, bytes, totalDurationMs);
    }
}
