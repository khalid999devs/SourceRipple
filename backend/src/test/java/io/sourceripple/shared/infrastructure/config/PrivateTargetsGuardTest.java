package io.sourceripple.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.support.TestProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class PrivateTargetsGuardTest {

    @Test
    void prodRefusesToStartWithPrivateTargetsAllowed() {
        assertThatThrownBy(() -> guard(true, "prod").afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void localAndTestMayAllowPrivateTargets() {
        assertThatCode(() -> guard(true, "local").afterPropertiesSet()).doesNotThrowAnyException();
        assertThatCode(() -> guard(false, "prod").afterPropertiesSet()).doesNotThrowAnyException();
    }

    private static PrivateTargetsGuard guard(boolean allowPrivateTargets, String profile) {
        SourceRippleProperties.Fetch fetch = new SourceRippleProperties.Fetch(
                allowPrivateTargets, null, null, null, null, null, 3, 2, null, List.of(), List.of(), null);
        SourceRippleProperties properties = TestProperties.withFetch(fetch);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new PrivateTargetsGuard(properties, environment);
    }
}
