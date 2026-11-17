package io.sourceripple.shared.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "sourceripple")
public record SourceRippleProperties(
        String publicBaseUrl,
        String encryptionKey,
        String tickToken,
        Admin admin,
        Metrics metrics,
        Scheduler scheduler,
        Watches watches,
        Fetch fetch,
        Webhooks webhooks,
        Demo demo) {

    public record Admin(String email, String password) {}

    public record Metrics(String username, String password) {}

    public enum SchedulerMode {
        INTERNAL,
        EXTERNAL,
        BOTH
    }

    public record Scheduler(
            SchedulerMode mode, Duration loopInterval, Duration tickBudget, int claimBatch, Duration lease) {}

    public record Watches(
            int defaultIntervalHours,
            int minIntervalHours,
            int versionsRetained,
            int maxPerProject,
            int maxPerOrganization,
            int maxOrganizationsPerUser,
            int maxActiveSources) {}

    public record Fetch(
            boolean allowPrivateTargets,
            Duration connectTimeout,
            Duration readTimeout,
            Duration totalTimeout,
            DataSize maxCompressedBytes,
            DataSize maxDecodedBytes,
            int maxRedirects,
            int concurrency,
            Duration perHostGap,
            List<Integer> allowedPorts) {}

    public record Webhooks(Duration timeout, int maxAttempts) {}

    public record Demo(boolean enabled, String webhookSecret, String userPassword) {}
}
