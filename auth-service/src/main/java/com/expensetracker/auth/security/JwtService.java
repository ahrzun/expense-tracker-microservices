package com.expensetracker.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints HS256 JSON Web Tokens.
 *
 * <p>A JWT is three base64url segments joined by dots:
 * {@code header.payload.signature}. The first two are <em>encoded, not
 * encrypted</em> — anyone holding the token can read every claim. So the token
 * carries identity, never secrets: no password, no hash, no personal data
 * beyond the email the holder already knows.
 *
 * <p>The third segment is {@code HMAC-SHA256(header + "." + payload, secret)}.
 * Tampering with a claim changes the signed input, so the recomputed MAC no
 * longer matches and verification fails. Producing a signature that <em>does</em>
 * match requires the secret key, which never leaves the server — that, and only
 * that, is what makes the token unforgeable. Its integrity rests entirely on
 * the secret staying secret.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;

        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        // HS256 requires a key of at least 256 bits. A shorter key weakens the
        // MAC, so fail loudly at startup rather than issue weak tokens.
        if (keyBytes.length < 32) {
            throw new WeakKeyException(
                    "jwt.secret must be at least 32 bytes (256 bits) for HS256; got " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public IssuedToken issue(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.expiry());

        String token = Jwts.builder()
                // sub: who the token is about. The stable user id, not the email —
                // an email can be changed later without invalidating identity.
                .subject(userId.toString())
                // A convenience claim so a downstream service can display the user
                // without another round trip. Readable by anyone; that is fine.
                .claim("email", email)
                // iss: who minted it, so a validator can reject tokens from elsewhere.
                .issuer(properties.issuer())
                // iat: when it was minted. Useful for auditing and for a future
                // "invalidate everything issued before X" check.
                .issuedAt(Date.from(now))
                // exp: the expiry. This is the only thing limiting the damage from a
                // stolen token, because nothing here can revoke one early.
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new IssuedToken(token, expiresAt, properties.expiry().toSeconds());
    }

    public record IssuedToken(String token, Instant expiresAt, long expiresInSeconds) {
    }
}
