package io.sourceripple.source.domain;

import io.sourceripple.source.application.fetch.SourceFetchBlockedException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class UrlCanonicalizer {

    private static final Pattern NUMERIC_HOST = Pattern.compile("[0-9.]+");
    private static final int MAX_LENGTH = 2048;

    private final Set<Integer> allowedPorts;
    private final AddressPolicy addressPolicy;

    public UrlCanonicalizer(Set<Integer> allowedPorts, AddressPolicy addressPolicy) {
        this.allowedPorts = Set.copyOf(allowedPorts);
        this.addressPolicy = addressPolicy;
    }

    public CanonicalUrl canonicalize(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_LENGTH) {
            throw blocked("Enter an http or https URL.");
        }
        URI uri = parse(raw.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw blocked("Only http and https URLs can be watched.");
        }
        if (uri.getRawUserInfo() != null || (uri.getRawAuthority() != null && uri.getRawAuthority().contains("@"))) {
            throw blocked("URLs with a user name or password are not allowed.");
        }
        String rawHost = uri.getHost();
        int rawPort = uri.getPort();
        if (rawHost == null && uri.getRawAuthority() != null) {
            // java.net.URI leaves the host empty for internationalized names, so split the authority by hand.
            String authority = uri.getAuthority();
            int colon = authority.lastIndexOf(':');
            rawHost = colon > 0 ? authority.substring(0, colon) : authority;
            rawPort = colon > 0 ? parsePort(authority.substring(colon + 1)) : -1;
        }
        String host = canonicalHost(rawHost);
        int defaultPort = scheme.equals("https") ? 443 : 80;
        int port = rawPort == -1 ? defaultPort : rawPort;
        if (port != defaultPort && !allowedPorts.contains(port)) {
            throw blocked("Port " + port + " is not allowed.");
        }
        rejectForbiddenLiteral(host);

        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String authority = (host.contains(":") ? "[" + host + "]" : host) + (port == defaultPort ? "" : ":" + port);
        return new CanonicalUrl(scheme + "://" + authority + path + query, scheme, host, port);
    }

    private static URI parse(String raw) {
        try {
            URI uri = new URI(raw);
            if (!uri.isAbsolute() || uri.isOpaque()) {
                throw blocked("Enter an absolute http or https URL.");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw blocked("This is not a valid URL.");
        }
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw blocked("The URL has an invalid port.");
        }
    }

    private static String canonicalHost(String host) {
        if (host == null || host.isBlank()) {
            throw blocked("The URL has no host.");
        }
        String unbracketed = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (unbracketed.contains(":")) {
            return unbracketed.toLowerCase(Locale.ROOT);
        }
        String trimmed = unbracketed.endsWith(".") ? unbracketed.substring(0, unbracketed.length() - 1) : unbracketed;
        try {
            return IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw blocked("The URL has an invalid host.");
        }
    }

    private void rejectForbiddenLiteral(String host) {
        if (!host.contains(":") && !NUMERIC_HOST.matcher(host).matches()) {
            return;
        }
        InetAddress address;
        try {
            // Only IP literals reach this point, so no DNS lookup happens.
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw blocked("The URL has an invalid IP address.");
        }
        if (!addressPolicy.isAllowed(address)) {
            throw blocked("This URL points to a private or reserved address.");
        }
    }

    private static SourceFetchBlockedException blocked(String message) {
        return new SourceFetchBlockedException(message);
    }
}
