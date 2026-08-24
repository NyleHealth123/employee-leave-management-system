package com.example.leavemanagement.unit;

import com.example.leavemanagement.shared.api.LocalDemoResetController;
import com.example.leavemanagement.shared.config.LocalDemoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDemoConfigurationSafetyTest {
    @Test
    void requiresEnvironmentSuppliedHashesWithoutFallbacksOrPlaintextSeedCredentials() throws IOException {
        var config = resource("/application-local-demo.yml");
        var seed = resource("/db/local-demo/R__local_demo_data.sql");
        var testConfig = resource("/application-test.yml");

        assertThat(config).contains("${DEMO_ADMIN_PASSWORD_HASH}", "${DEMO_MANAGER_PASSWORD_HASH}", "${DEMO_EMPLOYEE_PASSWORD_HASH}");
        assertThat(config).contains("${LOCAL_DEMO_RESET_ENABLED:false}", "${LOCAL_DEMO_EXPECTED_DATABASE:}");
        assertThat(config).doesNotContain(":change-me", "{noop}", "password:");
        assertThat(testConfig).doesNotContain("db/local-demo");
        assertThat(seed).contains("${demo_admin_password_hash}", "${demo_manager_password_hash}", "${demo_employee_password_hash}");
        assertThat(seed).doesNotContain("change-me", "password123", "{noop}");
    }

    @Test
    void resetComponentsAreActivatedOnlyForLocalDemo() {
        assertThat(LocalDemoConfiguration.class.getAnnotation(Profile.class).value()).containsExactly("local-demo");
        assertThat(LocalDemoResetController.class.getAnnotation(Profile.class).value()).containsExactly("local-demo");
    }

    @Test
    void refusesProductionAndNonDemoProfiles() {
        assertThatThrownBy(() -> validate(new String[]{"production"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", true)).hasMessageContaining("refused");
        assertThatThrownBy(() -> validate(new String[]{"local"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", true)).hasMessageContaining("refused");
        assertThatThrownBy(() -> validate(new String[]{"local-demo", "prod"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", true)).hasMessageContaining("refused");
    }

    @Test
    void refusesMissingOrUnverifiableSafetyConfiguration() {
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, false, "leave_management", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", true)).hasMessageContaining("not explicitly enabled");
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, true, "", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", true)).hasMessageContaining("missing");
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, true, "leave_management", "another_database",
                "jdbc:postgresql://localhost:5432/another_database", true)).hasMessageContaining("does not match");
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://database.example.test:5432/leave_management", true)).hasMessageContaining("localhost");
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, true, "leave_production", "leave_production",
                "jdbc:postgresql://localhost:5432/leave_production", true)).hasMessageContaining("production-named");
        assertThatThrownBy(() -> validate(new String[]{"local-demo"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://localhost:5432/leave_management", false)).hasMessageContaining("Flyway");
        assertThatThrownBy(() -> LocalDemoConfiguration.validateSafety(new String[]{"local-demo"}, true,
                "leave_management", "leave_management", "jdbc:postgresql://localhost:5432/leave_management",
                true, false)).hasMessageContaining("not an initialized authoritative local-demo database");
    }

    @Test
    void acceptsOnlyExplicitLocalDemoPostgresConfiguration() {
        assertThatCode(() -> validate(new String[]{"local-demo"}, true, "leave_management", "leave_management",
                "jdbc:postgresql://127.0.0.1:5432/leave_management", true)).doesNotThrowAnyException();
    }

    private void validate(String[] profiles, boolean enabled, String expectedDatabase, String actualDatabase,
                          String url, boolean demoMigrationConfigured) {
        LocalDemoConfiguration.validateSafety(profiles, enabled, expectedDatabase, actualDatabase, url,
                demoMigrationConfigured, true);
    }

    private String resource(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertThat(stream).as("resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
