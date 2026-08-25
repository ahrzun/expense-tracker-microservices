package com.expensetracker.expense.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound payload for creating or replacing an expense.
 *
 * <p>Deliberately separate from the {@code Expense} entity. Binding requests
 * straight onto an entity lets a caller set fields they should not control
 * (id, createdAt) and couples the public API shape to the database schema, so
 * a column rename becomes a breaking API change.
 *
 * <p>Constraints mirror the database ones so bad input fails as a 400 with a
 * useful message instead of a 500 from a constraint violation.
 */
public record ExpenseRequest(

        @NotBlank(message = "description is required")
        @Size(max = 255, message = "description must be at most 255 characters")
        String description,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "category is required")
        @Size(max = 64, message = "category must be at most 64 characters")
        String category,

        @NotNull(message = "spentOn is required")
        @PastOrPresent(message = "spentOn cannot be in the future")
        LocalDate spentOn
) {
}
