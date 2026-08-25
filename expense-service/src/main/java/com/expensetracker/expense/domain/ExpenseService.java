package com.expensetracker.expense.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Transaction boundary for expense operations.
 *
 * <p>Transactions are demarcated here rather than in the controller so that the
 * unit of work is the business operation, and rather than in the repository so
 * that a future multi-repository operation is still atomic.
 */
@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public List<Expense> findAll() {
        return repository.findAllByOrderBySpentOnDesc();
    }

    public Expense findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    @Transactional
    public Expense create(String description, BigDecimal amount, String category, LocalDate spentOn) {
        return repository.save(new Expense(description, amount, category, spentOn));
    }

    /**
     * Loads the entity and mutates it inside the transaction, so the change is
     * flushed by dirty checking. Deliberately not {@code save()} on a detached
     * object built from the request: that would silently overwrite createdAt
     * and any column the DTO does not carry.
     */
    @Transactional
    public Expense update(UUID id, String description, BigDecimal amount, String category, LocalDate spentOn) {
        Expense expense = findById(id);
        expense.update(description, amount, category, spentOn);
        return expense;
    }

    @Transactional
    public void delete(UUID id) {
        // Read first so a missing id is a 404 rather than a silent no-op.
        Expense expense = findById(id);
        repository.delete(expense);
    }
}
