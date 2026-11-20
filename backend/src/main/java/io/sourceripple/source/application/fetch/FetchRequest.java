package io.sourceripple.source.application.fetch;

public record FetchRequest(String url, String etag, String lastModified, FetchPurpose purpose) {

    public static FetchRequest get(String url, FetchPurpose purpose) {
        return new FetchRequest(url, null, null, purpose);
    }

    public FetchRequest withUrl(String newUrl, boolean keepValidators) {
        return new FetchRequest(newUrl, keepValidators ? etag : null, keepValidators ? lastModified : null, purpose);
    }
}
