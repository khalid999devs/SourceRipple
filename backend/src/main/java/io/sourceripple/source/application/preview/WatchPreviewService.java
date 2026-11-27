package io.sourceripple.source.application.preview;

import io.sourceripple.content.application.ContentInspector;
import io.sourceripple.content.application.ContentWarning;
import io.sourceripple.content.application.SourceAdapterFactory;
import io.sourceripple.content.domain.NormalizedDocument;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.status.RateLimitedException;
import io.sourceripple.shared.domain.GlobalFetchLimits;
import io.sourceripple.shared.infrastructure.RateLimiter;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.source.api.dto.WatchPreviewRequest;
import io.sourceripple.source.api.dto.WatchPreviewResponse;
import io.sourceripple.source.application.fetch.FetchPurpose;
import io.sourceripple.source.application.fetch.FetchRequest;
import io.sourceripple.source.application.fetch.FetchResult;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.application.fetch.failure.RobotsDisallowedException;
import io.sourceripple.source.application.fetch.failure.SourceFetchFailedException;
import io.sourceripple.source.domain.UrlCanonicalizer;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class WatchPreviewService {

    private final WebFetcher fetcher;
    private final UrlCanonicalizer canonicalizer;
    private final SourceAdapterFactory adapters;
    private final ContentInspector inspector;
    private final GlobalFetchLimits limits;
    private final RateLimiter rateLimiter;
    private final SourceRippleProperties.Limit previewLimit;

    public WatchPreviewService(
            WebFetcher fetcher,
            UrlCanonicalizer canonicalizer,
            SourceAdapterFactory adapters,
            ContentInspector inspector,
            GlobalFetchLimits limits,
            RateLimiter rateLimiter,
            SourceRippleProperties properties) {
        this.fetcher = fetcher;
        this.canonicalizer = canonicalizer;
        this.adapters = adapters;
        this.inspector = inspector;
        this.limits = limits;
        this.rateLimiter = rateLimiter;
        this.previewLimit = properties.rateLimit().preview();
    }

    @PreAuthorize("hasRole('MAINTAINER') and @access.canWriteProject(#projectId)")
    public WatchPreviewResponse preview(UUID projectId, WatchPreviewRequest request, UUID userId) {
        requireWithinRateLimit(userId);
        String requestedUrl = canonicalizer.canonicalize(request.url()).value();

        FetchResult result;
        try {
            result = fetcher.fetch(FetchRequest.get(requestedUrl, FetchPurpose.PREVIEW));
        } catch (RobotsDisallowedException disallowed) {
            return WatchPreviewResponse.robotsDisallowed(requestedUrl);
        }
        requireSuccess(result);

        NormalizedDocument document =
                adapters.adapt(result.mediaType(), result.body(), result.declaredCharset(), result.finalUrl());
        List<ContentWarning> warnings = inspector.warnings(document, result.body().length, limits.largePageBytes());
        return WatchPreviewResponse.of(requestedUrl, document, warnings);
    }

    private void requireWithinRateLimit(UUID userId) {
        rateLimiter
                .tryAcquire("preview:user:" + userId, previewLimit.requests(), previewLimit.window())
                .ifPresent(wait -> {
                    throw new RateLimitedException(
                            ErrorCode.RATE_LIMITED, "Too many previews. Try again shortly.", wait);
                });
    }

    private static void requireSuccess(FetchResult result) {
        if (result.isSuccess()) {
            return;
        }
        int status = result.status();
        boolean retryable = status == 408 || status == 429 || status >= 500;
        throw new SourceFetchFailedException("The page answered with HTTP " + status + ".", retryable, status);
    }
}
