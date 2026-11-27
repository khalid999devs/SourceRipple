package io.sourceripple.shared.infrastructure.config;

import io.sourceripple.shared.domain.GlobalFetchLimits;
import java.util.HashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FetchLimitsConfig {

    @Bean
    GlobalFetchLimits globalFetchLimits(SourceRippleProperties properties) {
        SourceRippleProperties.Fetch fetch = properties.fetch();
        return GlobalFetchLimits.initialize(GlobalFetchLimits.builder()
                .timeouts(fetch.connectTimeout(), fetch.readTimeout(), fetch.totalTimeout())
                .byteCaps(fetch.maxCompressedBytes().toBytes(), fetch.maxDecodedBytes().toBytes())
                .maxRedirects(fetch.maxRedirects())
                .concurrency(fetch.concurrency())
                .perHostGap(fetch.perHostGap())
                .allowedPorts(new HashSet<>(fetch.allowedPorts()))
                .allowedContentTypes(new HashSet<>(fetch.allowedContentTypes()))
                .largePageBytes(fetch.largePageBytes().toBytes()));
    }
}
