package com.expensetracker.expense.web;

import com.expensetracker.expense.domain.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Outbound representation of an expense. */
public record ExpenseResponse(
        UUID id,
        String description,
        BigDecimal amount,
        String category,
        LocalDate spentOn,
        Instant createdAt,
        Instant updatedAt
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getSpentOn(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
