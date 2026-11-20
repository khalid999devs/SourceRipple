package io.sourceripple.source.infrastructure.fetch;

import io.sourceripple.shared.domain.GlobalFetchLimits;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.source.application.RobotsPolicyService;
import io.sourceripple.source.application.fetch.WebFetcher;
import io.sourceripple.source.domain.AddressPolicy;
import io.sourceripple.source.domain.UrlCanonicalizer;
import okhttp3.Dns;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class FetchConfig {

    static final String REPOSITORY_URL = "https://github.com/khalid999devs/SourceRipple";

    @Bean
    AddressPolicy addressPolicy(SourceRippleProperties properties) {
        return new AddressPolicy(properties.fetch().allowPrivateTargets());
    }

    @Bean
    UrlCanonicalizer urlCanonicalizer(GlobalFetchLimits limits, AddressPolicy addressPolicy) {
        return new UrlCanonicalizer(limits.allowedPorts(), addressPolicy);
    }

    @Bean
    @Primary
    WebFetcher webFetcher(
            GlobalFetchLimits limits,
            AddressPolicy addressPolicy,
            UrlCanonicalizer canonicalizer,
            RobotsPolicyService robots,
            ObjectProvider<BuildProperties> buildProperties) {
        OkHttpWebFetcher http = new OkHttpWebFetcher(
                limits,
                new GuardedDns(addressPolicy, Dns.SYSTEM),
                new RemoteAddressInterceptor(addressPolicy),
                userAgent(buildProperties.getIfAvailable()));
        return new SafeFetchProxy(http, canonicalizer, robots, limits);
    }

    static String userAgent(BuildProperties build) {
        String version = build == null ? "dev" : build.getVersion();
        return "SourceRippleBot/" + version + " (+" + REPOSITORY_URL + ")";
    }
}
