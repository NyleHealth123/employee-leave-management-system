package com.example.leavemanagement.shared.config;

import com.example.leavemanagement.shared.application.LocalDemoResetService;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Configuration
@Profile("local-demo")
public class LocalDemoConfiguration {
    @Bean
    LocalDemoResetService localDemoResetService(
            Environment environment,
            DataSource dataSource,
            Flyway flyway,
            JdbcTemplate jdbc,
            @Value("${app.local-demo.reset-enabled:false}") boolean resetEnabled,
            @Value("${app.local-demo.expected-database:}") String expectedDatabase) {
        return new FlywayLocalDemoResetService(environment, dataSource, flyway, jdbc, resetEnabled, expectedDatabase);
    }

    public static void validateSafety(
            String[] activeProfiles,
            boolean resetEnabled,
            String expectedDatabase,
            String actualDatabase,
            String jdbcUrl,
            boolean demoMigrationConfigured,
            boolean authoritativeDemoDatasetPresent) {
        var profiles = Arrays.stream(activeProfiles).map(value -> value.toLowerCase(Locale.ROOT)).toList();
        if (!profiles.contains("local-demo") || profiles.contains("prod") || profiles.contains("production")) {
            throw unsafe("the active profiles do not establish an exclusive local-demo environment");
        }
        if (!resetEnabled) {
            throw unsafe("LOCAL_DEMO_RESET_ENABLED is not explicitly enabled");
        }
        if (expectedDatabase == null || expectedDatabase.isBlank()) {
            throw unsafe("LOCAL_DEMO_EXPECTED_DATABASE is missing");
        }
        if (actualDatabase == null || !expectedDatabase.equals(actualDatabase)) {
            throw unsafe("the connected database does not match LOCAL_DEMO_EXPECTED_DATABASE");
        }
        var normalizedDatabase = actualDatabase.toLowerCase(Locale.ROOT);
        if (normalizedDatabase.contains("prod") || normalizedDatabase.contains("production")) {
            throw unsafe("a production-named database can never be reset");
        }
        if (jdbcUrl == null || !(jdbcUrl.startsWith("jdbc:postgresql://localhost:")
                || jdbcUrl.startsWith("jdbc:postgresql://127.0.0.1:"))) {
            throw unsafe("the reset database must be a localhost PostgreSQL instance");
        }
        if (!demoMigrationConfigured) {
            throw unsafe("the authoritative local-demo Flyway location is not configured");
        }
        if (!authoritativeDemoDatasetPresent) {
            throw unsafe("the connected database is not an initialized authoritative local-demo database");
        }
    }

    private static IllegalStateException unsafe(String reason) {
        return new IllegalStateException("Local-demo reset refused: " + reason);
    }

    private static final class FlywayLocalDemoResetService implements LocalDemoResetService {
        private final Environment environment;
        private final DataSource dataSource;
        private final Flyway flyway;
        private final JdbcTemplate jdbc;
        private final boolean resetEnabled;
        private final String expectedDatabase;

        private FlywayLocalDemoResetService(Environment environment, DataSource dataSource, Flyway flyway,
                                            JdbcTemplate jdbc, boolean resetEnabled, String expectedDatabase) {
            this.environment = environment;
            this.dataSource = dataSource;
            this.flyway = flyway;
            this.jdbc = jdbc;
            this.resetEnabled = resetEnabled;
            this.expectedDatabase = expectedDatabase;
        }

        @Override
        public synchronized ResetResult reset() {
            var actualDatabase = jdbc.queryForObject("select current_database()", String.class);
            var jdbcUrl = jdbcUrl();
            var demoMigrationConfigured = Arrays.stream(flyway.getConfiguration().getLocations())
                    .anyMatch(location -> location.getDescriptor().contains("db/local-demo"));
            validateSafety(environment.getActiveProfiles(), resetEnabled, expectedDatabase, actualDatabase,
                    jdbcUrl, demoMigrationConfigured, authoritativeDemoDatasetPresent());

            flyway.clean();
            flyway.migrate();
            return verifyDataset();
        }

        private String jdbcUrl() {
            try (var connection = dataSource.getConnection()) {
                return connection.getMetaData().getURL();
            } catch (SQLException exception) {
                throw new IllegalStateException("Local-demo reset refused: database identity could not be established", exception);
            }
        }

        private ResetResult verifyDataset() {
            var statuses = new LinkedHashMap<String, Integer>();
            for (var status : new String[]{"PENDING", "APPROVED", "REJECTED", "CANCELLED"}) {
                statuses.put(status, count("select count(*) from leave_request where status = ?", status));
            }
            var result = new ResetResult(
                    count("select count(*) from employee_profile"),
                    count("select count(*) from user_account_role where role_code = 'ADMINISTRATOR'"),
                    count("select count(*) from user_account_role where role_code = 'MANAGER'"),
                    count("select count(*) from user_account_role where role_code = 'EMPLOYEE'"),
                    count("select count(*) from employee_profile where manager_id is not null"),
                    count("select count(*) from leave_type"),
                    count("select count(*) from leave_policy_version"),
                    count("select count(*) from policy_weekly_off"),
                    count("select count(*) from company_holiday where active"),
                    count("select count(*) from leave_balance"),
                    count("select count(*) from leave_request"),
                    count("select count(*) from audit_event"),
                    Map.copyOf(statuses));
            if (result.employees() < 50 || result.administrators() < 1 || result.managers() < 2 || result.employeeAccounts() < 50
                    || result.managerRelationships() < 50 || result.leaveTypes() == 0 || result.policies() == 0
                    || result.weeklyOffs() == 0 || result.holidays() == 0 || result.balances() < 50
                    || result.auditEvents() == 0 || statuses.values().stream().anyMatch(count -> count == 0)) {
                throw new IllegalStateException("Local-demo reset failed authoritative dataset verification");
            }
            return result;
        }

        private boolean authoritativeDemoDatasetPresent() {
            return count("select count(*) from organization_settings where id = md5('demo-organization')::uuid and name = 'Demo Leave Organization'") == 1
                    && count("select count(*) from user_account where normalized_login like 'demo.%'") >= 50;
        }

        private int count(String sql, Object... arguments) {
            return jdbc.queryForObject(sql, Integer.class, arguments);
        }
    }
}
