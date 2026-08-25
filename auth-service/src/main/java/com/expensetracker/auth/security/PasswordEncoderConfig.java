package com.expensetracker.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class PasswordEncoderConfig {

    /**
     * BCrypt at cost factor 12.
     *
     * <p>The cost is the base-2 logarithm of the number of key-setup rounds, so
     * 12 means 2^12 = 4096 iterations — roughly 200-300ms per hash on current
     * hardware. That is the point: it is slow <em>by design</em>, and the cost
     * can be raised as hardware improves without changing any stored hash,
     * because the factor is recorded inside each hash string.
     *
     * <p>Spring's default is 10. 12 is a deliberate step up, bounded by the fact
     * that this cost is paid on every login on the request thread.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
