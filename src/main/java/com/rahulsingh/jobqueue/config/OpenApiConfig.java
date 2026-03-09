package com.rahulsingh.jobqueue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobQueueOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Job Queue API")
                        .description("Async job lifecycle APIs with worker leasing, visibility timeout, DLQ operations, and idempotent enqueue")
                        .version("v1")
                        .contact(new Contact().name("Rahul Singh")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local server")));
    }
}
