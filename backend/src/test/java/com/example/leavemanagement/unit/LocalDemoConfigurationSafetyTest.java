package com.example.leavemanagement.unit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDemoConfigurationSafetyTest {
    @Test
    void requiresEnvironmentSuppliedHashesWithoutFallbacksOrPlaintextSeedCredentials() throws IOException {
        var config = resource("/application-local-demo.yml");
        var seed = resource("/db/local-demo/R__local_demo_data.sql");
        var testConfig = resource("/application-test.yml");

        assertThat(config).contains("${DEMO_ADMIN_PASSWORD_HASH}", "${DEMO_MANAGER_PASSWORD_HASH}", "${DEMO_EMPLOYEE_PASSWORD_HASH}");
        assertThat(config).doesNotContain(":change-me", "{noop}", "password:");
        assertThat(testConfig).doesNotContain("db/local-demo");
        assertThat(seed).contains("${demo_admin_password_hash}", "${demo_manager_password_hash}", "${demo_employee_password_hash}");
        assertThat(seed).doesNotContain("change-me", "password123", "{noop}");
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertThat(stream).as("resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
