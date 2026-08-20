package com.example.leavemanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MigrationValidationTest extends PostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void cleanDatabaseIsFullyMigratedBeforeHibernateValidation() {
        assertThat(jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null order by installed_rank",
                String.class)).containsExactly("001", "002", "003", "004", "005");

        assertThat(jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE'",
                String.class)).contains("user_account", "employee_profile", "leave_policy_version",
                "leave_balance", "leave_request", "leave_request_slot", "leave_request_status_history",
                "audit_event", "flyway_schema_history");
    }

    @Test
    void requiredDatabaseConstraintsAndAppendOnlyGuardsExist() {
        assertThat(constraintNames()).contains(
                "ck_employee_not_own_manager",
                "leave_policy_version_leave_type_id_daterange_excl",
                "leave_balance_employee_id_leave_type_id_daterange_excl",
                "ck_balance_available",
                "ck_request_dates",
                "ck_half_day_single_date");

        assertThat(jdbc.queryForObject(
                "select count(*) from pg_indexes where schemaname='public' and indexname='uq_active_leave_slot'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList(
                "select tgname from pg_trigger where not tgisinternal and tgname like 'trg_%_immutable' order by tgname",
                String.class)).containsExactly(
                "trg_audit_event_immutable", "trg_balance_movement_immutable", "trg_status_history_immutable");
    }

    private List<String> constraintNames() {
        return jdbc.queryForList("select conname from pg_constraint", String.class);
    }
}
