package com.expensetracker.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered account.
 *
 * <p>Only ever holds the BCrypt hash. The plaintext password exists on the
 * request thread long enough to be hashed or verified and is never assigned to
 * a field, never persisted, and never logged.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Stored lower-cased; see {@link #normalizeEmail}. */
    @Column(name = "email", nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. Not for application use. */
    protected User() {
    }

    public User(String email, String passwordHash) {
        this.id = UUID.randomUUID();
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
    }

    /**
     * Lower-cases and trims, so the plain UNIQUE constraint on the column is a
     * real case-insensitive guarantee. Without this, Alice@x.com and alice@x.com
     * would be two accounts and login would depend on how the user typed it.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
