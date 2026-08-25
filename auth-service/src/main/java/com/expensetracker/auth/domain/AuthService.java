package com.expensetracker.auth.domain;

import com.expensetracker.auth.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * A valid BCrypt hash of a value nobody knows, compared against when the
     * email does not exist so that login takes the same time either way.
     * Generated at startup rather than hard-coded so it is never a real hash
     * of a real password.
     */
    private static final String DUMMY_HASH =
            new BCryptPasswordEncoder(12).encode(UUID.randomUUID().toString());

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Authenticated register(String email, String rawPassword) {
        String normalized = User.normalizeEmail(email);

        // Checked here for a clean 409. The UNIQUE constraint on the column is
        // still the real guarantee — this check and the insert are not atomic,
        // so two simultaneous registrations of the same address are resolved by
        // the database, not by this if-statement.
        if (repository.existsByEmail(normalized)) {
            throw new EmailAlreadyRegisteredException(normalized);
        }

        User user = new User(normalized, passwordEncoder.encode(rawPassword));
        repository.save(user);

        return authenticated(user);
    }

    @Transactional(readOnly = true)
    public Authenticated login(String email, String rawPassword) {
        String normalized = User.normalizeEmail(email);

        Optional<User> found = repository.findByEmail(normalized);

        // Always pay the BCrypt cost, even when the email is unknown.
        //
        // Returning early on a missing account would make "no such user" answer
        // in a millisecond while "wrong password" takes the ~250ms of a real
        // hash comparison. That timing difference is a user-enumeration oracle
        // just as usable as a distinguishable error message — identical wording
        // does not help if the clock gives the answer away. Hashing against a
        // throwaway hash keeps both paths the same shape.
        String hashToCheck = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (found.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }

        return authenticated(found.get());
    }

    private Authenticated authenticated(User user) {
        JwtService.IssuedToken issued = jwtService.issue(user.getId(), user.getEmail());
        return new Authenticated(user, issued);
    }

    public record Authenticated(User user, JwtService.IssuedToken token) {
    }
}
