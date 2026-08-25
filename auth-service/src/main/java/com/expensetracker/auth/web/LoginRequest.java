package com.expensetracker.auth.web;

import jakarta.validation.constraints.NotBlank;

/**
 * No format or length constraints beyond presence. Login must accept whatever
 * the account was created with, including passwords predating any future
 * change to the registration rules.
 */
public record LoginRequest(

        @NotBlank(message = "email is required")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
}
