package com.expensetracker.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the gateway on a random port and asserts two things that matter for
 * this slice:
 *
 * <ol>
 *   <li>the application actually starts on the reactive stack, and</li>
 *   <li>Actuator serves health at /health.</li>
 * </ol>
 *
 * <p>If {@code spring-boot-starter-web} ever leaks onto this module's classpath,
 * Boot starts a servlet container instead of Netty and Spring Cloud Gateway
 * aborts startup — this test fails loudly rather than at deploy time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @Test
    void healthEndpointIsServedAtSlashHealth() {
        webTestClient.get().uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void runsOnTheReactiveStackNotServlet() {
        // A reactive web application context, not a servlet one.
        assertThat(context).isNotInstanceOf(WebApplicationContext.class);
        // Sanity check that the reactive client stack is genuinely present.
        assertThat(context.getBean(WebClient.Builder.class)).isNotNull();
    }
}
