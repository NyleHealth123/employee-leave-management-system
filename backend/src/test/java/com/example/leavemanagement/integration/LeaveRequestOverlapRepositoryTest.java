package com.example.leavemanagement.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import com.example.leavemanagement.request.persistence.LeaveRequestRepository;

import java.sql.Timestamp;
import java.time.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class LeaveRequestOverlapRepositoryTest extends PostgresIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

    @Autowired JdbcTemplate jdbc;
    @Autowired LeaveRequestRepository requests;

    @BeforeEach
    void cleanBusinessData() {
        jdbc.execute("truncate table user_account, leave_type restart identity cascade");
    }

    @ParameterizedTest
    @CsvSource({"AM,AM", "PM,PM", "AM,'AM;PM'", "PM,'AM;PM'", "'AM;PM',AM", "'AM;PM',PM", "'AM;PM','AM;PM'"})
    void everyActivePartialAndFullDayOverlapShapeIsRejected(String occupied, String requested) {
        var fixture = fixture();
        var occupiedRequest = insertRequest(fixture, "occupied");
        for (var slot : occupied.split(";")) insertSlot(occupiedRequest, fixture.employee(), slot, true);
        var candidate = insertRequest(fixture, "candidate");
        assertThatThrownBy(() -> {
            for (var slot : requested.split(";")) insertSlot(candidate, fixture.employee(), slot, true);
        }).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void complementaryHalfDaysAndOtherEmployeesRemainAvailable() {
        var fixture = fixture();
        insertSlot(insertRequest(fixture, "occupied-am"), fixture.employee(), "AM", true);
        insertSlot(insertRequest(fixture, "available-pm"), fixture.employee(), "PM", true);
        var otherEmployee = fixture("other");
        insertSlot(insertRequest(otherEmployee, "other-employee"), otherEmployee.employee(), "AM", true);
    }

    @Test
    void inactiveSlotsDoNotBlockNewActiveOccupancy() {
        var fixture = fixture();
        insertSlot(insertRequest(fixture, "inactive"), fixture.employee(), "AM", false);
        insertSlot(insertRequest(fixture, "replacement"), fixture.employee(), "AM", true);
    }

    @Test
    void concurrentActiveSlotInsertsAllowExactlyOneWinner() throws Exception {
        var fixture = fixture();
        var first = insertRequest(fixture, "concurrent-one");
        var second = insertRequest(fixture, "concurrent-two");
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstResult = executor.submit(() -> insertConcurrently(first, fixture.employee(), start));
            Future<Boolean> secondResult = executor.submit(() -> insertConcurrently(second, fixture.employee(), start));
            start.countDown();
            assertThat(java.util.List.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void teamCalendarRepositoryEnforcesSameManagerActiveStatusScopeAndFieldProjection() {
        var shared = sharedTypeAndPolicy();
        var manager = insertEmployee("manager", null, true);
        var otherManager = insertEmployee("other-manager", null, true);
        var viewer = insertEmployee("viewer", manager.id(), true);
        var coworker = insertEmployee("coworker", manager.id(), true);
        var inactiveCoworker = insertEmployee("inactive", manager.id(), false);
        var differentTeam = insertEmployee("different", otherManager.id(), true);
        var noManagerViewer = insertEmployee("solo", null, true);

        insertRequest(viewer, shared, "PENDING", "viewer-pending");
        insertRequest(viewer, shared, "REJECTED", "viewer-rejected");
        insertRequest(coworker, shared, "APPROVED", "coworker-approved");
        insertRequest(coworker, shared, "CANCELLED", "coworker-cancelled");
        insertRequest(inactiveCoworker, shared, "PENDING", "inactive-pending");
        insertRequest(differentTeam, shared, "PENDING", "different-pending");
        insertRequest(noManagerViewer, shared, "APPROVED", "solo-approved");

        assertThat(requests.findEmployeeTeamCalendar(viewer.id()))
                .extracting(LeaveRequestRepository.TeamCalendarRow::getEmployeeDisplayName,
                        LeaveRequestRepository.TeamCalendarRow::getStatus)
                .containsExactlyInAnyOrder(tuple("viewer", "PENDING"), tuple("coworker", "APPROVED"));
        assertThat(requests.findEmployeeTeamCalendar(noManagerViewer.id()))
                .extracting(LeaveRequestRepository.TeamCalendarRow::getEmployeeDisplayName,
                        LeaveRequestRepository.TeamCalendarRow::getStatus)
                .containsExactly(tuple("solo", "APPROVED"));
    }

    private boolean insertConcurrently(UUID request, UUID employee, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            insertSlot(request, employee, "AM", true);
            return true;
        } catch (DuplicateKeyException expected) {
            return false;
        }
    }

    private void assertActiveConflict(UUID request, UUID employee, String slot) {
        assertThatThrownBy(() -> insertSlot(request, employee, slot, true))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private Fixture fixture() {
        return fixture("");
    }

    private Fixture fixture(String suffix) {
        suffix = suffix + UUID.randomUUID().toString().substring(0, 8);
        var user = UUID.randomUUID();
        var employee = UUID.randomUUID();
        var type = UUID.randomUUID();
        var policy = UUID.randomUUID();
        var now = Timestamp.from(Instant.now());
        jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)", user, "u" + suffix,
                "u" + suffix, "hash", true, now, now, now, 0);
        jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)",
                employee, "E" + suffix, user, "Employee", "e" + suffix + "@example.com", true, now, now);
        jdbc.update("insert into leave_type values (?,?,?,?,?,0)", type, "ANNUAL" + suffix, "Annual", null, true);
        jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)", policy, type, 1,
                LocalDate.of(2026, 1, 1), null, true, true, "EXCLUDE", "EXCLUDE", false, 1, now);
        return new Fixture(employee, type, policy);
    }

    private Shared sharedTypeAndPolicy() {
        var type = UUID.randomUUID(); var policy = UUID.randomUUID(); var now = Timestamp.from(Instant.now());
        jdbc.update("insert into leave_type values (?,?,?,?,?,0)", type, "SHARED", "Shared", null, true);
        jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)", policy, type, 1,
                LocalDate.of(2026, 1, 1), null, false, true, "INCLUDE", "INCLUDE", false, 1, now);
        return new Shared(type, policy);
    }

    private Employee insertEmployee(String label, UUID manager, boolean active) {
        var user = UUID.randomUUID(); var employee = UUID.randomUUID(); var now = Timestamp.from(Instant.now());
        jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)", user, label, label, "hash", true, now, now, now, 0);
        jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,manager_id,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,?,0)",
                employee, "E-" + label, user, label, label + "@example.com", manager, active, now, now);
        return new Employee(employee);
    }

    private void insertRequest(Employee employee, Shared shared, String status, String key) {
        var id = UUID.randomUUID();
        jdbc.update("insert into leave_request(id,employee_id,leave_type_id,submitted_policy_version_id,start_date,end_date,duration_mode,chargeable_units,reason,status,submitted_at,policy_snapshot,idempotency_key,version) values (?,?,?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,0)",
                id, employee.id(), shared.type(), shared.policy(), DATE, DATE, "FULL_DAY", 2, "Private reason", status,
                Timestamp.from(Instant.now()), "{}", key);
    }

    private UUID insertRequest(Fixture fixture, String key) {
        var id = UUID.randomUUID();
        jdbc.update("insert into leave_request(id,employee_id,leave_type_id,submitted_policy_version_id,start_date,end_date,duration_mode,chargeable_units,reason,status,submitted_at,policy_snapshot,idempotency_key,version) values (?,?,?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,0)",
                id, fixture.employee(), fixture.type(), fixture.policy(), DATE, DATE, "FULL_DAY", 2,
                "Rest", "PENDING", Timestamp.from(Instant.now()), "{}", key);
        return id;
    }

    private void insertSlot(UUID request, UUID employee, String slot, boolean active) {
        jdbc.update("insert into leave_request_slot values (?,?,?,?,?,?)", UUID.randomUUID(), request,
                employee, DATE, slot, active);
    }

    private record Fixture(UUID employee, UUID type, UUID policy) {}
    private record Shared(UUID type, UUID policy) {}
    private record Employee(UUID id) {}
}
