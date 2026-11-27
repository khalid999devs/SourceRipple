package io.sourceripple.source.domain;

public record CanonicalUrl(String value, String scheme, String host, int port) {

    public static int defaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    public static String authority(String scheme, String host, int port) {
        String bracketed = host.contains(":") ? "[" + host + "]" : host;
        return port == defaultPort(scheme) ? bracketed : bracketed + ":" + port;
    }

    public String origin() {
        return scheme + "://" + authority(scheme, host, port);
    }

    @Override
    public String toString() {
        return value;
    }
}
