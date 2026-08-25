package com.expensetracker.expense.domain;

import java.util.UUID;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID id) {
        super("No expense with id " + id);
    }
}
