package io.sourceripple.source.domain;

public record CanonicalUrl(String value, String scheme, String host, int port) {

    public String origin() {
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        String authority = host.contains(":") ? "[" + host + "]" : host;
        return scheme + "://" + authority + (defaultPort ? "" : ":" + port);
    }

    @Override
    public String toString() {
        return value;
    }
}
