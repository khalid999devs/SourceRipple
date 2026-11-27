package io.sourceripple.support;

import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties.Admin;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties.Fetch;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties.RateLimit;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties.Workspace;

public final class TestProperties {

    private TestProperties() {}

    public static SourceRippleProperties withAdmin(Admin admin) {
        return new SourceRippleProperties(null, null, null, admin, null, null, null, null, null, null, null, null);
    }

    public static SourceRippleProperties withFetch(Fetch fetch) {
        return new SourceRippleProperties(null, null, null, null, null, null, null, null, fetch, null, null, null);
    }

    public static SourceRippleProperties withRateLimit(RateLimit rateLimit) {
        return new SourceRippleProperties(null, null, null, null, null, null, null, null, null, null, rateLimit, null);
    }

    public static SourceRippleProperties withWorkspace(Workspace workspace) {
        return new SourceRippleProperties(null, null, null, null, null, null, workspace, null, null, null, null, null);
    }
}
