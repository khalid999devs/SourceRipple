package io.sourceripple.identity.infrastructure.security;

import io.sourceripple.shared.api.SpaRoutes;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Application filter chain: public probes, API docs and SPA pages; everything else is denied. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher spaPage = request -> HttpMethod.GET.matches(request.getMethod())
                && SpaRoutes.isSpaPage(request.getRequestURI().substring(request.getContextPath().length()));

        return http.authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/index.html",
                                "/assets/**",
                                "/favicon.svg",
                                "/robots.txt",
                                "/sitemap.xml",
                                "/og.png")
                        .permitAll()
                        .requestMatchers(spaPage)
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                // Stateless until the spring_session tables are migrated.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .build();
    }
}
