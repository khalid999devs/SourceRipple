package io.sourceripple.identity.infrastructure.security;

import io.sourceripple.identity.infrastructure.security.handler.JsonAccessDeniedHandler;
import io.sourceripple.identity.infrastructure.security.handler.JsonAuthenticationEntryPoint;
import io.sourceripple.shared.api.spa.SpaRoutes;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            DatabaseUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Account status is checked only after the password matches, so a wrong password never reveals it.
        provider.setPreAuthenticationChecks(user -> {});
        provider.setPostAuthenticationChecks(SecurityConfig::checkAccountStatus);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http.authorizeHttpRequests(SecurityConfig::urlRules)
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .cors(cors -> cors.disable())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("SESSION"))
                .requestCache(cache -> cache.disable())
                .build();
    }

    private static void urlRules(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        RequestMatcher spaPage = request -> HttpMethod.GET.matches(request.getMethod())
                && SpaRoutes.isSpaPage(request.getRequestURI().substring(request.getContextPath().length()));

        auth.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                .permitAll()
                .requestMatchers(
                        HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/demo-login")
                .permitAll()
                .requestMatchers("/api/v1/public/**")
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
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
                .requestMatchers(HttpMethod.GET, "/demo/vendor/**")
                .permitAll()
                .requestMatchers(spaPage)
                .permitAll()
                .anyRequest()
                .authenticated();
    }

    private static void checkAccountStatus(UserDetails user) {
        if (!user.isAccountNonLocked()) {
            throw new LockedException("The account is locked.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("The account is disabled.");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("The account has expired.");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("The credentials have expired.");
        }
    }
}
