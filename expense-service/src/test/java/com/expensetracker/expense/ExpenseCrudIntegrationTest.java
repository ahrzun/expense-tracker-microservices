package com.expensetracker.expense;

import com.expensetracker.expense.domain.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end CRUD against a real PostgreSQL 16 container, exercising the whole
 * stack: HTTP → validation → service/transaction → Hibernate → Flyway-created schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseCrudIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void flywayCreatedTheTableInTheServiceOwnedSchema() {
        // Proves the schema-per-service decision is actually in force: both the
        // table and Flyway's own history table live in expense_service, not public.
        Integer tables = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'expense_service'
                  and table_name in ('expenses', 'flyway_schema_history')
                """, Integer.class);

        assertThat(tables).isEqualTo(2);
    }

    @Test
    void createsReadsUpdatesAndDeletesAnExpense() {
        Map<String, Object> body = Map.of(
                "description", "Groceries",
                "amount", "42.50",
                "category", "FOOD",
                "spentOn", LocalDate.now().minusDays(1).toString());

        // CREATE -> 201 + Location
        ResponseEntity<Map> created = restTemplate.postForEntity("/expenses", body, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();

        UUID id = UUID.fromString((String) created.getBody().get("id"));
        assertThat(created.getBody().get("description")).isEqualTo("Groceries");
        // Round-trips through NUMERIC(12,2) with the scale intact.
        assertThat(new BigDecimal(created.getBody().get("amount").toString()))
                .isEqualByComparingTo("42.50");

        // READ one
        ResponseEntity<Map> fetched = restTemplate.getForEntity("/expenses/" + id, Map.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("category")).isEqualTo("FOOD");

        // LIST
        ResponseEntity<Map[]> listed = restTemplate.getForEntity("/expenses", Map[].class);
        assertThat(listed.getBody()).hasSize(1);

        // UPDATE
        Map<String, Object> edit = Map.of(
                "description", "Groceries and coffee",
                "amount", "51.00",
                "category", "FOOD",
                "spentOn", LocalDate.now().toString());
        restTemplate.put("/expenses/" + id, edit);

        ResponseEntity<Map> afterUpdate = restTemplate.getForEntity("/expenses/" + id, Map.class);
        assertThat(afterUpdate.getBody().get("description")).isEqualTo("Groceries and coffee");
        // createdAt survives an update; only updatedAt moves.
        assertThat(afterUpdate.getBody().get("createdAt")).isEqualTo(created.getBody().get("createdAt"));

        // DELETE -> 204, then gone
        restTemplate.delete("/expenses/" + id);
        assertThat(restTemplate.getForEntity("/expenses/" + id, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidInputWithFourHundredNotFiveHundred() {
        Map<String, Object> invalid = Map.of(
                "description", "",
                "amount", "-5.00",
                "category", "FOOD",
                "spentOn", LocalDate.now().plusYears(1).toString());

        ResponseEntity<Map> response = restTemplate.postForEntity("/expenses", invalid, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsKeys("description", "amount", "spentOn");
    }

    @Test
    void unknownIdIsFourOhFour() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/expenses/" + UUID.randomUUID(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
