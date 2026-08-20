package com.example.leavemanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-demo")
class LocalDemoDatasetTest extends PostgresIntegrationTest {
    private static final String TEST_HASH = "{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void demoCredentials(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.placeholders.demo_admin_password_hash", () -> TEST_HASH);
        registry.add("spring.flyway.placeholders.demo_manager_password_hash", () -> TEST_HASH);
        registry.add("spring.flyway.placeholders.demo_employee_password_hash", () -> TEST_HASH);
    }

    @Test
    void seedsOrganizationStructureConfigurationAndRepresentativeStatuses() {
        assertThat(count("select count(*) from employee_profile")).isGreaterThanOrEqualTo(50);
        assertThat(count("select count(*) from user_account_role where role_code='ADMINISTRATOR'")).isGreaterThanOrEqualTo(1);
        assertThat(count("select count(*) from user_account_role where role_code='MANAGER'")).isGreaterThanOrEqualTo(4);
        assertThat(count("select count(*) from user_account_role where role_code='EMPLOYEE'")).isGreaterThanOrEqualTo(50);
        assertThat(count("select count(*) from employee_profile e join employee_profile m on m.id=e.manager_id where m.id in (select e2.id from employee_profile e2 join user_account_role r on r.account_id=e2.user_account_id where r.role_code='MANAGER')")).isGreaterThanOrEqualTo(50);
        assertThat(count("select count(*) from leave_type")).isGreaterThanOrEqualTo(3);
        assertThat(count("select count(*) from leave_policy_version")).isGreaterThanOrEqualTo(3);
        assertThat(count("select count(*) from policy_weekly_off")).isGreaterThanOrEqualTo(1);
        assertThat(count("select count(*) from company_holiday where active")).isGreaterThanOrEqualTo(3);
        assertThat(count("select count(*) from leave_balance")).isGreaterThanOrEqualTo(50);
        assertThat(count("select count(*) from leave_request")).isEqualTo(50);
        for (var status : new String[]{"PENDING", "APPROVED", "REJECTED", "CANCELLED"}) {
            assertThat(count("select count(*) from leave_request where status='" + status + "'")).isGreaterThan(0);
        }
        assertThat(count("select count(*) from audit_event")).isGreaterThanOrEqualTo(50);
    }

    @Test
    void usesEncodedCredentialPlaceholdersAndProductionConfigurationDoesNotActivateDemoLocation() {
        assertThat(jdbc.queryForList("select password_hash from user_account where normalized_login like 'demo.%'", String.class))
                .allMatch(value -> value.startsWith("{bcrypt}"));
        assertThat(getClass().getResourceAsStream("/application.yml")).isNotNull();
        try (var stream = getClass().getResourceAsStream("/application.yml")) {
            var production = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(production).doesNotContain("db/local-demo");
        } catch (java.io.IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
