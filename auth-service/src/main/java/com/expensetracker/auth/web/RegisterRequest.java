package com.expensetracker.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        @Size(max = 320, message = "email must be at most 320 characters")
        String email,

        /*
         * A length floor and nothing else. Composition rules ("one uppercase,
         * one digit, one symbol") push people towards Password1! and are no
         * longer recommended by NIST; length is what actually costs an attacker.
         * The upper bound exists because BCrypt silently truncates input beyond
         * 72 bytes — without it, two different long passwords could both work
         * on the same account.
         */
        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String password
) {
}
