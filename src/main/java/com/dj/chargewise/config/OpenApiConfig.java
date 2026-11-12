package com.dj.chargewise.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI chargewiseOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private static Info apiInfo() {
        return new Info()
                .title("Chargewise RAG API")
                .version("v1")
                .description("""
                        AI-powered chargeback and dispute resolution copilot. \
                        Log in to get a token, click Authorize, then load scheme rules (ADMIN) \
                        or analyse a dispute (AGENT) to get its category, the required evidence \
                        and citations to the rules used. All scheme rules are fictional.""")
                .contact(new Contact()
                        .name("Dusmanta Kumar Jena")
                        .url("https://github.com/dusmantajena/chargewise-rag"));
    }

    private static SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the accessToken returned by POST /api/v1/auth/login");
    }
}
