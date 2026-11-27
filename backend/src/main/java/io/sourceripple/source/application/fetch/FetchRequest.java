package io.sourceripple.source.application.fetch;

import java.time.Duration;

public record FetchRequest(String url, String etag, String lastModified, FetchPurpose purpose, Duration timeout) {

    public static FetchRequest get(String url, FetchPurpose purpose) {
        return new FetchRequest(url, null, null, purpose, null);
    }

    public static FetchRequest conditional(String url, String etag, String lastModified, FetchPurpose purpose) {
        return new FetchRequest(url, etag, lastModified, purpose, null);
    }

    public FetchRequest withUrl(String newUrl) {
        return new FetchRequest(newUrl, etag, lastModified, purpose, timeout);
    }

    public FetchRequest withoutValidators() {
        return new FetchRequest(url, null, null, purpose, timeout);
    }

    public FetchRequest withTimeout(Duration newTimeout) {
        return new FetchRequest(url, etag, lastModified, purpose, newTimeout);
    }
}
