package com.expensetracker.auth;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Real PostgreSQL 16 for integration tests, started once per JVM.
 *
 * <p>Singleton pattern rather than {@code @Testcontainers}/{@code @Container}:
 * with the annotation pair on a static field of a shared base class, JUnit
 * stops the container after the first test class and every later class fails
 * against a dead database. Ryuk reaps this one at JVM exit.
 */
abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }
}
