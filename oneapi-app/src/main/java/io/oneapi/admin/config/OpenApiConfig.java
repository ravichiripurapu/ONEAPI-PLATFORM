package io.oneapi.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OneAPI Admin API")
                        .version("1.0")
                        .description("Admin API for managing database connections and schema versions")
                        .contact(new Contact()
                                .name("OneAPI Team")
                                .email("admin@oneapi.io")));
    }
}
