package com.dj.chargewise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI chargewiseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Chargewise RAG API")
                .version("v1")
                .description("""
                        AI-powered chargeback and dispute resolution copilot. \
                        Load scheme rules, then analyse a dispute to get its category, \
                        the required evidence and citations to the rules used. \
                        All scheme rules are fictional.""")
                .contact(new Contact()
                        .name("Dusmanta Kumar Jena")
                        .url("https://github.com/dusmantajena/chargewise-rag")));
    }
}
