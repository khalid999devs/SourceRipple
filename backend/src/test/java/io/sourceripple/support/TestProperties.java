package io.sourceripple.support;

import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties.RateLimit;

public final class TestProperties {

    private TestProperties() {}

    public static SourceRippleProperties withRateLimit(RateLimit rateLimit) {
        return new SourceRippleProperties(null, null, null, null, null, null, null, null, null, rateLimit, null);
    }
}
