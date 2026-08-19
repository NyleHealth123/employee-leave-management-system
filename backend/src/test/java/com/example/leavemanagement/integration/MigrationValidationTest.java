package com.example.leavemanagement.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
class MigrationValidationTest extends PostgresIntegrationTest {
 @Autowired JdbcTemplate jdbc;
 @Test void allMigrationsApplyAndCriticalConstraintsExist(){assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success",Integer.class)).isEqualTo(5);assertThat(jdbc.queryForObject("select count(*) from pg_indexes where indexname='uq_active_leave_slot'",Integer.class)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from pg_trigger where tgname='trg_audit_event_immutable'",Integer.class)).isEqualTo(1);}
}

