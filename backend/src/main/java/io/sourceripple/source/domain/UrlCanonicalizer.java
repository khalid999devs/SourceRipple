package io.sourceripple.source.domain;

import io.sourceripple.source.application.fetch.failure.SourceFetchBlockedException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class UrlCanonicalizer {

    private static final Pattern DIGITS_AND_DOTS = Pattern.compile("[0-9.]+");
    private static final String OCTET = "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])";
    private static final Pattern DOTTED_QUAD = Pattern.compile("(" + OCTET + "\\.){3}" + OCTET);
    private static final int MAX_LENGTH = 2048;

    private final Set<Integer> allowedPorts;
    private final AddressPolicy addressPolicy;

    public UrlCanonicalizer(Set<Integer> allowedPorts, AddressPolicy addressPolicy) {
        this.allowedPorts = Set.copyOf(allowedPorts);
        this.addressPolicy = addressPolicy;
    }

    public CanonicalUrl canonicalize(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_LENGTH) {
            throw new SourceFetchBlockedException("Enter an http or https URL.");
        }
        URI uri = parse(raw.trim());
        String scheme = scheme(uri);
        rejectUserInfo(uri);

        HostAndPort authority = hostAndPort(uri);
        String host = canonicalHost(authority.host());
        int port = authority.port() == -1 ? CanonicalUrl.defaultPort(scheme) : authority.port();
        requireAllowedPort(scheme, port);
        rejectForbiddenLiteral(host);

        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String value = scheme + "://" + CanonicalUrl.authority(scheme, host, port) + path + query;
        return new CanonicalUrl(value, scheme, host, port);
    }

    private static URI parse(String raw) {
        try {
            URI uri = new URI(raw);
            if (!uri.isAbsolute() || uri.isOpaque()) {
                throw new SourceFetchBlockedException("Enter an absolute http or https URL.");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new SourceFetchBlockedException("This is not a valid URL.");
        }
    }

    private static String scheme(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new SourceFetchBlockedException("Only http and https URLs can be watched.");
        }
        return scheme;
    }

    private static void rejectUserInfo(URI uri) {
        if (uri.getRawUserInfo() != null || (uri.getRawAuthority() != null && uri.getRawAuthority().contains("@"))) {
            throw new SourceFetchBlockedException("URLs with a user name or password are not allowed.");
        }
    }

    private static HostAndPort hostAndPort(URI uri) {
        if (uri.getHost() != null || uri.getRawAuthority() == null) {
            return new HostAndPort(uri.getHost(), uri.getPort());
        }
        // java.net.URI leaves the host empty for internationalized names, so split the authority by hand.
        String authority = uri.getAuthority();
        int colon = authority.lastIndexOf(':');
        if (colon <= 0) {
            return new HostAndPort(authority, -1);
        }
        return new HostAndPort(authority.substring(0, colon), parsePort(authority.substring(colon + 1)));
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new SourceFetchBlockedException("The URL has an invalid port.");
        }
    }

    private void requireAllowedPort(String scheme, int port) {
        if (port != CanonicalUrl.defaultPort(scheme) && !allowedPorts.contains(port)) {
            throw new SourceFetchBlockedException("Port " + port + " is not allowed.");
        }
    }

    private static String canonicalHost(String host) {
        if (host == null || host.isBlank()) {
            throw new SourceFetchBlockedException("The URL has no host.");
        }
        String unbracketed = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (unbracketed.contains(":")) {
            return unbracketed.toLowerCase(Locale.ROOT);
        }

        String trimmed = unbracketed.endsWith(".") ? unbracketed.substring(0, unbracketed.length() - 1) : unbracketed;
        try {
            return IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new SourceFetchBlockedException("The URL has an invalid host.");
        }
    }

    private void rejectForbiddenLiteral(String host) {
        boolean ipv6 = host.contains(":");
        if (!ipv6 && !DIGITS_AND_DOTS.matcher(host).matches()) {
            return;
        }
        if (!ipv6 && !DOTTED_QUAD.matcher(host).matches()) {
            throw new SourceFetchBlockedException("The URL has an invalid IP address.");
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new SourceFetchBlockedException("The URL has an invalid IP address.");
        }
        if (!addressPolicy.isAllowed(address)) {
            throw new SourceFetchBlockedException("This URL points to a private or reserved address.");
        }
    }

    private record HostAndPort(String host, int port) {}
}
