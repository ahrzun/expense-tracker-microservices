package com.expensetracker.auth.web;

import com.expensetracker.auth.domain.AuthService;

import java.time.Instant;
import java.util.UUID;

/**
 * The token plus the minimum a client needs to use it.
 *
 * <p>Never contains the password hash. {@code expiresIn} saves every client
 * from having to base64-decode the payload to find out when to re-authenticate.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UUID userId,
        String email
) {

    public static AuthResponse from(AuthService.Authenticated authenticated) {
        return new AuthResponse(
                authenticated.token().token(),
                "Bearer",
                authenticated.token().expiresInSeconds(),
                authenticated.token().expiresAt(),
                authenticated.user().getId(),
                authenticated.user().getEmail()
        );
    }
}
