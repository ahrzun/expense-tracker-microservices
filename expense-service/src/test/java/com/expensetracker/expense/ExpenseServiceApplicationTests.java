package com.expensetracker.expense;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real application against a real PostgreSQL container and asserts
 * that Actuator serves health at /health — and that health now reflects actual
 * database connectivity rather than just "the process is up".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseServiceApplicationTests extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsServedAtSlashHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void healthReportsDatabaseConnectivity() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        // The "db" component is contributed by Actuator's DataSourceHealthIndicator,
        // which appeared automatically when a DataSource joined the context. A
        // hand-written /health controller would not know the database exists.
        assertThat(response.getBody()).contains("\"db\"");
        assertThat(response.getBody()).contains("PostgreSQL");
    }
}
