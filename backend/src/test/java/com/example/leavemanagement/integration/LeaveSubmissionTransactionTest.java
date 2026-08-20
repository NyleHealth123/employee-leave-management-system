package com.example.leavemanagement.integration;

import com.example.leavemanagement.request.application.LeaveCalculationService;
import com.example.leavemanagement.request.application.LeaveSubmissionService;
import com.example.leavemanagement.request.domain.DurationMode;
import com.example.leavemanagement.shared.api.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LeaveSubmissionTransactionTest extends PostgresIntegrationTest {
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 9, 1);
    private static final LocalDate SECOND_DAY = LocalDate.of(2026, 9, 2);

    @Autowired LeaveSubmissionService submissions;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanBusinessData() {
        jdbc.execute("truncate table user_account, leave_type restart identity cascade");
        jdbc.execute("create or replace function fail_test_insert() returns trigger language plpgsql as $$ begin raise exception 'injected persistence failure'; end $$");
    }

    @Test
    void allocatesAcrossDeterministicallyOrderedBalancePeriods() {
        var fixture = fixture(4, true);
        var request = submissions.submit(fixture.user(), fixture.employee(), input(FIRST_DAY, SECOND_DAY), "multi-period");
        assertThat(jdbc.queryForList("select b.period_start, l.units from leave_request_balance_line l join leave_balance b on b.id=l.balance_id where l.request_id=? order by b.period_start, b.id", request.getId()))
                .extracting(row -> row.get("units")).containsExactly(2, 2);
        assertThat(jdbc.queryForList("select movement_type from leave_balance_movement where request_id=? order by balance_id", String.class, request.getId())).containsExactly("RESERVE", "RESERVE");
        assertThat(jdbc.queryForObject("select sum(reserved_units) from leave_balance where employee_id=?", Integer.class, fixture.employee())).isEqualTo(4);
    }

    @Test
    void concurrentNonOverlappingSubmissionsBothSucceedWhenBalanceIsSufficient() throws Exception {
        var fixture = fixture(4, false);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Outcome> first = executor.submit(() -> submit(fixture, FIRST_DAY, "sufficient-one"));
            Future<Outcome> second = executor.submit(() -> submit(fixture, SECOND_DAY, "sufficient-two"));
            assertThat(java.util.List.of(first.get(), second.get())).containsOnly(Outcome.SUCCESS);
        }
        assertThat(count("leave_request")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select reserved_units from leave_balance where employee_id=?", Integer.class, fixture.employee())).isEqualTo(4);
    }

    @Test
    void concurrentNonOverlappingSubmissionsHaveOneAtomicLoserWhenBalanceIsInsufficient() throws Exception {
        var fixture = fixture(2, false);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Outcome> first = executor.submit(() -> submit(fixture, FIRST_DAY, "insufficient-one"));
            Future<Outcome> second = executor.submit(() -> submit(fixture, SECOND_DAY, "insufficient-two"));
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(Outcome.SUCCESS, Outcome.INSUFFICIENT_BALANCE);
        }
        assertThat(count("leave_request")).isEqualTo(1);
        assertThat(count("leave_request_slot")).isEqualTo(2);
        assertThat(count("leave_request_balance_line")).isEqualTo(1);
        assertThat(count("leave_balance_movement")).isEqualTo(1);
        assertThat(count("leave_request_status_history")).isEqualTo(1);
        assertThat(count("audit_event")).isEqualTo(1);
    }

    @Test
    void duplicateIdempotencyKeyReturnsTheOriginalResultWithoutExtraWrites() {
        var fixture = fixture(4, false);
        var input = input(FIRST_DAY, FIRST_DAY);
        var first = submissions.submit(fixture.user(), fixture.employee(), input, "same-command");
        var duplicate = submissions.submit(fixture.user(), fixture.employee(), input, "same-command");
        assertThat(duplicate.getId()).isEqualTo(first.getId());
        assertThat(count("leave_request")).isEqualTo(1);
        assertThat(count("leave_balance_movement")).isEqualTo(1);
        assertThat(count("leave_request_status_history")).isEqualTo(1);
        assertThat(count("audit_event")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"leave_request", "leave_request_slot", "leave_request_balance_line", "leave_balance_movement", "leave_request_status_history", "audit_event"})
    void everyRequiredPersistenceFailureRollsBackTheWholeSubmission(String failingTable) {
        var fixture = fixture(4, false);
        jdbc.execute("create trigger test_fail_insert before insert on " + failingTable + " for each row execute function fail_test_insert()");
        try {
            assertThatThrownBy(() -> submissions.submit(fixture.user(), fixture.employee(), input(FIRST_DAY, FIRST_DAY), "rollback-" + failingTable)).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("drop trigger if exists test_fail_insert on " + failingTable);
        }
        assertThat(count("leave_request")).isZero();
        assertThat(count("leave_request_slot")).isZero();
        assertThat(count("leave_request_balance_line")).isZero();
        assertThat(count("leave_balance_movement")).isZero();
        assertThat(count("leave_request_status_history")).isZero();
        assertThat(count("audit_event")).isZero();
        assertThat(jdbc.queryForObject("select reserved_units from leave_balance where employee_id=?", Integer.class, fixture.employee())).isZero();
    }

    private Outcome submit(Fixture fixture, LocalDate date, String key) {
        try {
            submissions.submit(fixture.user(), fixture.employee(), input(date, date), key);
            return Outcome.SUCCESS;
        } catch (DomainException exception) {
            if ("INSUFFICIENT_BALANCE".equals(exception.code())) return Outcome.INSUFFICIENT_BALANCE;
            throw exception;
        }
    }

    private Fixture fixture(int allocatedUnits, boolean splitPeriods) {
        var user = UUID.randomUUID(); var employee = UUID.randomUUID(); var type = UUID.randomUUID(); var policy = UUID.randomUUID(); var now = Timestamp.from(Instant.now());
        jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)", user, "employee", "employee", "hash", true, now, now, now, 0);
        jdbc.update("insert into user_account_role values (?,?)", user, "EMPLOYEE");
        jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)", employee, "E1", user, "Employee", "employee@example.com", true, now, now);
        jdbc.update("insert into leave_type values (?,?,?,?,?,0)", type, "ANNUAL", "Annual", null, true);
        jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)", policy, type, 1, LocalDate.of(2026, 1, 1), null, true, true, "INCLUDE", "INCLUDE", false, 1, now);
        if (splitPeriods) {
            insertBalance(employee, type, FIRST_DAY, FIRST_DAY, 2, now);
            insertBalance(employee, type, SECOND_DAY, LocalDate.of(2026, 12, 31), 2, now);
        } else insertBalance(employee, type, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), allocatedUnits, now);
        return new Fixture(user, employee, type);
    }

    private void insertBalance(UUID employee, UUID type, LocalDate from, LocalDate to, int units, Timestamp now) {
        jdbc.update("insert into leave_balance(id,employee_id,leave_type_id,period_start,period_end,allocated_units,adjustment_units,reserved_units,consumed_units,version,created_at,updated_at) values (?,?,?,?,?,?,0,0,0,0,?,?)", UUID.randomUUID(), employee, type, from, to, units, now, now);
    }

    private LeaveCalculationService.Input input(LocalDate start, LocalDate end) {
        return new LeaveCalculationService.Input(currentType(), start, end, DurationMode.FULL_DAY, "Rest");
    }

    private UUID currentType() { return jdbc.queryForObject("select id from leave_type where code='ANNUAL'", UUID.class); }
    private int count(String table) { return jdbc.queryForObject("select count(*) from " + table, Integer.class); }
    private record Fixture(UUID user, UUID employee, UUID type) {}
    private enum Outcome { SUCCESS, INSUFFICIENT_BALANCE }
}
