package com.expensetracker.auth.domain;

/**
 * Thrown for BOTH "no such email" and "wrong password".
 *
 * <p>Deliberately indistinguishable. Separate messages would turn the login
 * endpoint into a user-enumeration oracle: an attacker could discover which
 * email addresses hold accounts simply by watching which error comes back,
 * which is useful for credential stuffing and phishing.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
