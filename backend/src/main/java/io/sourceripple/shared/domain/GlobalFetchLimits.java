package io.sourceripple.shared.domain;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class GlobalFetchLimits {

    private static volatile GlobalFetchLimits instance;

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration totalTimeout;
    private final long maxCompressedBytes;
    private final long maxDecodedBytes;
    private final int maxRedirects;
    private final int concurrency;
    private final Duration perHostGap;
    private final Set<Integer> allowedPorts;
    private final Set<String> allowedContentTypes;
    private final long largePageBytes;

    private GlobalFetchLimits(Builder builder) {
        this.connectTimeout = Objects.requireNonNull(builder.connectTimeout);
        this.readTimeout = Objects.requireNonNull(builder.readTimeout);
        this.totalTimeout = Objects.requireNonNull(builder.totalTimeout);
        this.maxCompressedBytes = builder.maxCompressedBytes;
        this.maxDecodedBytes = builder.maxDecodedBytes;
        this.maxRedirects = builder.maxRedirects;
        this.concurrency = builder.concurrency;
        this.perHostGap = Objects.requireNonNull(builder.perHostGap);
        this.allowedPorts = Set.copyOf(builder.allowedPorts);
        this.allowedContentTypes = builder.allowedContentTypes.stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.largePageBytes = builder.largePageBytes;
    }

    /** Builds the instance once; later calls must describe the same limits. */
    public static synchronized GlobalFetchLimits initialize(Builder builder) {
        GlobalFetchLimits candidate = new GlobalFetchLimits(builder);
        if (instance == null) {
            instance = candidate;
        } else if (!instance.equals(candidate)) {
            throw new IllegalStateException("GlobalFetchLimits is already initialized with different values");
        }
        return instance;
    }

    public static GlobalFetchLimits getInstance() {
        GlobalFetchLimits current = instance;
        if (current == null) {
            throw new IllegalStateException("GlobalFetchLimits has not been initialized");
        }
        return current;
    }

    public static Builder builder() {
        return new Builder();
    }

    static GlobalFetchLimits detached(Builder builder) {
        return new GlobalFetchLimits(builder);
    }

    public boolean allowsContentType(String mediaType) {
        return mediaType != null && allowedContentTypes.contains(mediaType.toLowerCase(Locale.ROOT));
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    public Duration totalTimeout() {
        return totalTimeout;
    }

    public long maxCompressedBytes() {
        return maxCompressedBytes;
    }

    public long maxDecodedBytes() {
        return maxDecodedBytes;
    }

    public int maxRedirects() {
        return maxRedirects;
    }

    public int concurrency() {
        return concurrency;
    }

    public Duration perHostGap() {
        return perHostGap;
    }

    public Set<Integer> allowedPorts() {
        return allowedPorts;
    }

    public Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    public long largePageBytes() {
        return largePageBytes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GlobalFetchLimits that
                && maxCompressedBytes == that.maxCompressedBytes
                && maxDecodedBytes == that.maxDecodedBytes
                && maxRedirects == that.maxRedirects
                && concurrency == that.concurrency
                && largePageBytes == that.largePageBytes
                && connectTimeout.equals(that.connectTimeout)
                && readTimeout.equals(that.readTimeout)
                && totalTimeout.equals(that.totalTimeout)
                && perHostGap.equals(that.perHostGap)
                && allowedPorts.equals(that.allowedPorts)
                && allowedContentTypes.equals(that.allowedContentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                connectTimeout,
                readTimeout,
                totalTimeout,
                maxCompressedBytes,
                maxDecodedBytes,
                maxRedirects,
                concurrency,
                perHostGap,
                allowedPorts,
                allowedContentTypes,
                largePageBytes);
    }

    public static final class Builder {

        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(10);
        private Duration totalTimeout = Duration.ofSeconds(15);
        private long maxCompressedBytes = 2L * 1024 * 1024;
        private long maxDecodedBytes = 5L * 1024 * 1024;
        private int maxRedirects = 3;
        private int concurrency = 2;
        private Duration perHostGap = Duration.ofSeconds(10);
        private Set<Integer> allowedPorts = Set.of(80, 443, 8080, 8443);
        private Set<String> allowedContentTypes = Set.of(
                "text/html",
                "application/xhtml+xml",
                "application/rss+xml",
                "application/atom+xml",
                "application/xml",
                "text/xml");
        private long largePageBytes = 1024L * 1024;

        private Builder() {}

        public Builder timeouts(Duration connect, Duration read, Duration total) {
            this.connectTimeout = connect;
            this.readTimeout = read;
            this.totalTimeout = total;
            return this;
        }

        public Builder byteCaps(long compressed, long decoded) {
            this.maxCompressedBytes = compressed;
            this.maxDecodedBytes = decoded;
            return this;
        }

        public Builder maxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder perHostGap(Duration perHostGap) {
            this.perHostGap = perHostGap;
            return this;
        }

        public Builder allowedPorts(Set<Integer> allowedPorts) {
            this.allowedPorts = allowedPorts;
            return this;
        }

        public Builder allowedContentTypes(Set<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
            return this;
        }

        public Builder largePageBytes(long largePageBytes) {
            this.largePageBytes = largePageBytes;
            return this;
        }
    }
}
