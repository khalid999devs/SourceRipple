package io.sourceripple.shared.infrastructure.security;

import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/** Stateless HTTP Basic protection for the Prometheus scrape endpoint. */
@Configuration(proxyBeanMethods = false)
public class MetricsSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain metricsSecurityFilterChain(HttpSecurity http, SourceRippleProperties properties)
            throws Exception {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        List<UserDetails> users = new ArrayList<>();
        SourceRippleProperties.Metrics metrics = properties.metrics();
        if (metrics != null && StringUtils.hasText(metrics.username()) && StringUtils.hasText(metrics.password())) {
            users.add(User.withUsername(metrics.username())
                    .password(encoder.encode(metrics.password()))
                    .roles("METRICS")
                    .build());
        }
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(users));
        provider.setPasswordEncoder(encoder);

        // A private authentication manager keeps the scrape credential from ever signing in to the app.
        return http.securityMatcher("/actuator/prometheus")
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("METRICS"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .build();
    }
}
