package io.sourceripple.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String SESSION = "session";
    private static final String CSRF = "csrf";

    @Bean
    OpenAPI sourceRippleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SourceRipple API")
                        .description("Web change and project dependency monitoring")
                        .version("v1")
                        .license(new License().name("Apache-2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes(SESSION, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SESSION"))
                        .addSecuritySchemes(CSRF, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION).addList(CSRF));
    }

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("api").pathsToMatch("/api/v1/**").build();
    }
}
