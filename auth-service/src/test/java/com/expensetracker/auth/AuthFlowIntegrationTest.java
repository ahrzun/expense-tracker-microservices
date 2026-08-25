package com.expensetracker.auth;

import com.expensetracker.auth.domain.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registration and login against a real PostgreSQL container, including the
 * properties of the issued token that actually matter for security.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private SecretKey key() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static Map<String, Object> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    @Test
    void registersThenLogsInWithTheSameCredentials() {
        ResponseEntity<Map> registered = restTemplate.postForEntity(
                "/auth/register", credentials("alice@example.com", "correct-horse-battery"), Map.class);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody().get("tokenType")).isEqualTo("Bearer");
        assertThat(registered.getBody().get("email")).isEqualTo("alice@example.com");
        assertThat((String) registered.getBody().get("token")).isNotBlank();

        ResponseEntity<Map> loggedIn = restTemplate.postForEntity(
                "/auth/login", credentials("alice@example.com", "correct-horse-battery"), Map.class);

        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loggedIn.getBody().get("userId")).isEqualTo(registered.getBody().get("userId"));
    }

    @Test
    void storesOnlyABcryptHashNeverThePlaintext() {
        restTemplate.postForEntity(
                "/auth/register", credentials("bob@example.com", "correct-horse-battery"), Map.class);

        String stored = userRepository.findByEmail("bob@example.com").orElseThrow().getPasswordHash();

        assertThat(stored).doesNotContain("correct-horse-battery");
        // BCrypt's own format: $2<variant>$<cost>$<22-char salt><31-char digest>.
        // The cost factor travels inside the hash, which is what lets it be
        // raised later without invalidating existing passwords.
        assertThat(stored).startsWith("$2a$12$");
        assertThat(stored).hasSize(60);
    }

    @Test
    void theSamePasswordHashesDifferentlyForTwoUsers() {
        restTemplate.postForEntity("/auth/register",
                credentials("c@example.com", "identical-password-x"), Map.class);
        restTemplate.postForEntity("/auth/register",
                credentials("d@example.com", "identical-password-x"), Map.class);

        String first = userRepository.findByEmail("c@example.com").orElseThrow().getPasswordHash();
        String second = userRepository.findByEmail("d@example.com").orElseThrow().getPasswordHash();

        // Per-password random salt. This is why a stolen table cannot be attacked
        // with a precomputed rainbow table, and why an attacker who cracks one
        // account learns nothing about another with the same password.
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void tokenCarriesTheExpectedClaimsAndVerifiesAgainstTheSecret() {
        ResponseEntity<Map> registered = restTemplate.postForEntity(
                "/auth/register", credentials("eve@example.com", "correct-horse-battery"), Map.class);
        String token = (String) registered.getBody().get("token");

        Claims claims = Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject())
                .isEqualTo(registered.getBody().get("userId").toString());
        assertThat(claims.get("email")).isEqualTo("eve@example.com");
        assertThat(claims.getIssuer()).isEqualTo("expense-tracker-auth-test");
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }

    @Test
    void payloadIsReadableByAnyone_soItMustNotCarrySecrets() {
        ResponseEntity<Map> registered = restTemplate.postForEntity(
                "/auth/register", credentials("frank@example.com", "correct-horse-battery"), Map.class);
        String token = (String) registered.getBody().get("token");

        // No key involved: the payload is base64url, not encryption.
        String payload = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("frank@example.com");
        // Which is exactly why nothing sensitive goes in it.
        assertThat(payload).doesNotContain("correct-horse-battery");
        assertThat(payload).doesNotContain("$2a$");
    }

    @Test
    void aTamperedTokenFailsVerification() {
        ResponseEntity<Map> registered = restTemplate.postForEntity(
                "/auth/register", credentials("grace@example.com", "correct-horse-battery"), Map.class);
        String token = (String) registered.getBody().get("token");

        // Re-encode the payload with a different subject, keeping the original
        // signature — the classic "just edit the claims" forgery attempt.
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\",\"email\":\"attacker@example.com\"}")
                        .getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        // The signature was computed over the original payload, so the recomputed
        // MAC no longer matches. Producing one that does requires the secret.
        assertThatThrownBy(() -> Jwts.parser().verifyWith(key()).build().parseSignedClaims(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void aTokenSignedWithADifferentSecretIsRejected() {
        SecretKey attackerKey = new SecretKeySpec(
                "an-attacker-key-that-is-also-long-enough-for-hs256".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");

        String forged = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "attacker@example.com")
                .signWith(attackerKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> Jwts.parser().verifyWith(key()).build().parseSignedClaims(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void duplicateEmailIsConflictAndIsCaseInsensitive() {
        restTemplate.postForEntity("/auth/register",
                credentials("henry@example.com", "correct-horse-battery"), Map.class);

        ResponseEntity<Map> duplicate = restTemplate.postForEntity("/auth/register",
                credentials("HENRY@Example.COM", "a-different-password"), Map.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void wrongPasswordAndUnknownEmailAreIndistinguishable() {
        restTemplate.postForEntity("/auth/register",
                credentials("iris@example.com", "correct-horse-battery"), Map.class);

        ResponseEntity<Map> wrongPassword = restTemplate.postForEntity(
                "/auth/login", credentials("iris@example.com", "wrong-password-here"), Map.class);
        ResponseEntity<Map> unknownEmail = restTemplate.postForEntity(
                "/auth/login", credentials("nobody@example.com", "wrong-password-here"), Map.class);

        // Same status and same body: the endpoint is not a user-enumeration oracle.
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().get("detail"))
                .isEqualTo(unknownEmail.getBody().get("detail"));
    }

    @Test
    void shortPasswordIsRejectedWithFieldLevelDetail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register", credentials("jane@example.com", "short"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) response.getBody().get("errors")).containsKey("password");
        assertThat(userRepository.count()).isZero();
    }
}
