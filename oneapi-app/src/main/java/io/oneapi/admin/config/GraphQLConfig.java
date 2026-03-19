package io.oneapi.admin.config;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration for custom scalars.
 */
@Configuration
public class GraphQLConfig {

    /**
     * JSON scalar - represents any JSON object/array/value
     */
    @Bean
    public GraphQLScalarType jsonScalar() {
        return graphql.scalars.ExtendedScalars.Json;
    }

    /**
     * Long scalar - represents 64-bit integers
     */
    @Bean
    public GraphQLScalarType longScalar() {
        return graphql.scalars.ExtendedScalars.GraphQLLong;
    }

    /**
     * Register custom scalars with GraphQL runtime
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(jsonScalar())
                .scalar(longScalar());
    }
}
