package com.expensetracker.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge gateway. Runs as a reactive (WebFlux/Netty) application.
 *
 * <p>No routes and no JWT filter yet — Slice 1 only establishes that the
 * gateway application starts successfully on the reactive stack.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
