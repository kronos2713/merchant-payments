package com.florinparaschiv.payments.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String PROBLEM_SCHEMA = "Problem";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    @Bean
    OpenAPI merchantPaymentsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("merchant-payments API")
                .version("v0.1")
                .description("""
                        Payment-request service shaped like iDEAL: merchant onboarding, \
                        payment requests and payment. Errors use RFC 9457 Problem Details \
                        (application/problem+json). Phase 1 has no authentication.\
                        """));
    }

    @Bean
    OpenApiCustomizer problemDetailsOnErrorResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());

            Content problemContent = new Content().addMediaType(PROBLEM_MEDIA_TYPE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA)));

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path ->
                    path.readOperations().forEach(operation ->
                            operation.getResponses().forEach((code, response) -> {
                                if (code.startsWith("4") || code.startsWith("5")) {
                                    response.setContent(problemContent);
                                }
                            })));
        };
    }

    private static Schema<?> problemSchema() {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("field", new StringSchema().example("amountMinor"));
        fieldError.addProperty("message", new StringSchema().example("must not be null"));

        ObjectSchema problem = new ObjectSchema();
        problem.description("RFC 9457 Problem Details");
        problem.addProperty("type", new StringSchema()
                .example("https://merchant-payments.example/problems/conflict"));
        problem.addProperty("title", new StringSchema().example("Conflict with current state"));
        problem.addProperty("status", new IntegerSchema().example(409));
        problem.addProperty("detail", new StringSchema()
                .example("payment request has already been paid"));
        problem.addProperty("instance", new StringSchema()
                .example("/payment-requests/fd3386a9-728c-4e61-952e-200ffab85f00/payment"));
        problem.addProperty("errors", new ArraySchema().items(fieldError)
                .description("Field errors; present only on validation failures"));
        return problem;
    }
}