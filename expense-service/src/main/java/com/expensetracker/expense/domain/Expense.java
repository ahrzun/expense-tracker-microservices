package com.expensetracker.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single recorded expense.
 *
 * <p>The mapping here must match {@code V1__create_expenses_table.sql} exactly:
 * Hibernate runs with {@code ddl-auto=validate} and will refuse to start the
 * application if an entity and its table have drifted apart.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    /**
     * Assigned by the application rather than a database sequence. A UUID does
     * not leak how many rows exist or let a caller enumerate other people's
     * expenses by decrementing an integer — which matters once these rows belong
     * to users. The cost is a wider, less index-local key than a bigserial.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /** BigDecimal, never double — see the NUMERIC note in the migration. */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected Expense() {
    }

    public Expense(String description, BigDecimal amount, String category, LocalDate spentOn) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.spentOn = spentOn;
    }

    /** Applies an edit in place; {@code @UpdateTimestamp} refreshes updatedAt on flush. */
    public void update(String description, BigDecimal amount, String category, LocalDate spentOn) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.spentOn = spentOn;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
