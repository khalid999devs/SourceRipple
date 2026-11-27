package io.sourceripple.shared.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class PrivateTargetsGuard implements InitializingBean {

    private final SourceRippleProperties properties;
    private final Environment environment;

    public PrivateTargetsGuard(SourceRippleProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.fetch().allowPrivateTargets() && environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("SOURCERIPPLE_FETCH_ALLOW_PRIVATE_TARGETS=true is refused in the prod"
                    + " profile: it would let watches reach private and metadata addresses.");
        }
    }
}
