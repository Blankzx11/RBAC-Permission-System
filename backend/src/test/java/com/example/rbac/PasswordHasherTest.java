package com.example.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.rbac.common.PasswordHasher;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    @Test
    void verifiesDemoPasswordWithoutStoringPlaintext() {
        String hash = "pbkdf2$120000$O34Pel0sShucjX5vWks8LQ==$UMqWYzYkdeldgefmxw0H0KbJsRztCSI5EqHicqxLcGE=";
        assertThat(PasswordHasher.matches("DemoOnly12345", hash)).isTrue();
        assertThat(PasswordHasher.matches("wrong-password", hash)).isFalse();
    }
}
