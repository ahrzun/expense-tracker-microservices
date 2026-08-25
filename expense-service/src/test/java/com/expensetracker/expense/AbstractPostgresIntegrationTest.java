package com.expensetracker.expense;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need the real database.
 *
 * <p>Deliberately a real PostgreSQL 16 container rather than H2. An in-memory
 * database with a compatibility mode is a different SQL dialect: it would not
 * run the Flyway migration the same way, would not enforce NUMERIC(12,2) or the
 * CHECK constraint identically, and would let {@code ddl-auto=validate} pass
 * against a schema production would reject. The point of these tests is that
 * the migration and the entity mapping agree on the engine we actually deploy on.
 *
 * <p>The container uses the singleton pattern — started once in a static
 * initialiser and never explicitly stopped — rather than JUnit's
 * {@code @Testcontainers}/{@code @Container} pair. With {@code @Container} on a
 * static field of a shared base class, the JUnit extension stops the container
 * when the first test class finishes, and every later class fails against a dead
 * database. Testcontainers' Ryuk sidecar reaps this one when the JVM exits.
 *
 * <p>{@code @ServiceConnection} injects the container's JDBC URL, username and
 * password into the Spring context, so no {@code @DynamicPropertySource} is needed.
 */
abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }
}
