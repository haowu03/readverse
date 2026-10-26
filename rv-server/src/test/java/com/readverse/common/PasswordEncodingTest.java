package com.readverse.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncodingTest {

    @Test
    void bcryptMatchesRawPasswordAndRejectsWrongPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Reader@123");

        assertThat(encoder.matches("Reader@123", hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
