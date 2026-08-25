package com.expensetracker.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT signing configuration, bound from {@code jwt.*}.
 *
 * @param secret HS256 signing key. Supplied by the environment; there is no
 *               default, so a missing value fails startup instead of silently
 *               using a key that is in version control.
 * @param issuer the {@code iss} claim, identifying who minted the token.
 * @param expiry how long an issued token stays valid.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, String issuer, Duration expiry) {
}
