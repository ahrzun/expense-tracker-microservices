package com.expensetracker.expense.web;

import com.expensetracker.expense.domain.Expense;
import com.expensetracker.expense.domain.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Expense CRUD.
 *
 * <p>Unauthenticated for now. These rows are not yet scoped to a user — that
 * arrives with the gateway's JWT validation, at which point a {@code user_id}
 * column and an owner check are added by migration.
 */
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExpenseResponse> list() {
        return service.findAll().stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable UUID id) {
        return ExpenseResponse.from(service.findById(id));
    }

    /** 201 with a Location header, as a creating POST should. */
    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        Expense created = service.create(
                request.description(), request.amount(), request.category(), request.spentOn());

        URI location = uriBuilder.path("/expenses/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ExpenseResponse.from(created));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        Expense updated = service.update(
                id, request.description(), request.amount(), request.category(), request.spentOn());
        return ExpenseResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
