package com.fiapx.api.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Insira o token JWT retornado no login para autenticar as requisições.")
                                )
                )
                .info(
                        new Info()
                                .title("🎬 FIAP X - API de Processamento de Vídeos")
                                .version("1.0.0")
                                .description("Documentação interativa da API de ingestão assíncrona, autenticação JWT e extração de frames de vídeos.")
                                .contact(new Contact().name("Michel Marinho de Oliveira - FIAP SOAT").email("michel@fiap.com.br"))
                                .license(new License().name("Apache 2.0").url("https://springdoc.org"))
                );
    }
}
