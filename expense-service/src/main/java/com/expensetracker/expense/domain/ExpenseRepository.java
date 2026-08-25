package com.expensetracker.expense.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /** Listing order for the collection endpoint: most recently spent first. */
    List<Expense> findAllByOrderBySpentOnDesc();
}
