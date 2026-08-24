package com.example.leavemanagement.integration;

import com.example.leavemanagement.reporting.persistence.LeaveSummaryRepository;
import com.example.leavemanagement.reporting.persistence.OrganizationLeaveRequestRepository;
import com.example.leavemanagement.request.persistence.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueryPerformanceTest extends PostgresIntegrationTest {
    private static final int EMPLOYEE_COUNT = 60;
    private static final int REQUESTS_PER_EMPLOYEE = 10;
    private static final int MEASURED_RUNS = 20;
    private static final Duration INTERACTIVE_TARGET = Duration.ofMillis(500);
    private static final Duration REPORT_TARGET = Duration.ofSeconds(2);

    @Autowired JdbcTemplate jdbc;
    @Autowired LeaveRequestRepository requests;
    @Autowired OrganizationLeaveRequestRepository organizationRequests;
    @Autowired LeaveSummaryRepository summaries;

    private UUID managerId;
    private UUID viewerId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void seedRepresentativeMvpFixture() {
        jdbc.execute("truncate table user_account, leave_type restart identity cascade");
        periodStart = LocalDate.of(2026, 1, 1);
        periodEnd = LocalDate.of(2026, 12, 31);
        var leaveTypeId = id("leave-type");
        var policyId = id("leave-policy");
        var now = Instant.parse("2026-08-24T00:00:00Z");
        managerId = id("employee-0");
        viewerId = id("employee-1");

        jdbc.update("insert into leave_type values (?,?,?,?,?,0)", leaveTypeId, "ANNUAL", "Annual leave", null, true);
        jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)", policyId, leaveTypeId, 1,
                periodStart, null, true, true, "EXCLUDE", "EXCLUDE", false, 1, Timestamp.from(now));

        for (int employeeIndex = 0; employeeIndex < EMPLOYEE_COUNT; employeeIndex++) {
            var accountId = id("account-" + employeeIndex);
            var employeeId = id("employee-" + employeeIndex);
            var createdAt = Timestamp.from(now.plusSeconds(employeeIndex));
            jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)", accountId, "user" + employeeIndex,
                    "user" + employeeIndex, "{noop}test-only", true, createdAt, createdAt, createdAt, 0);
            jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,manager_id,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,?,0)",
                    employeeId, "EMP-" + employeeIndex, accountId, "Employee " + employeeIndex,
                    "employee" + employeeIndex + "@example.test", employeeIndex == 0 ? null : managerId, true, createdAt, createdAt);

            for (int requestIndex = 0; requestIndex < REQUESTS_PER_EMPLOYEE; requestIndex++) {
                var start = periodStart.plusDays((employeeIndex * REQUESTS_PER_EMPLOYEE + requestIndex) % 330L);
                var status = List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED").get(requestIndex % 4);
                jdbc.update("insert into leave_request(id,employee_id,leave_type_id,submitted_policy_version_id,start_date,end_date,duration_mode,chargeable_units,reason,status,submitted_at,policy_snapshot,idempotency_key,version) values (?,?,?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,0)",
                        id("request-" + employeeIndex + '-' + requestIndex), employeeId, leaveTypeId, policyId,
                        start, start.plusDays(requestIndex % 2), "FULL_DAY", 2, "Performance fixture", status,
                        Timestamp.from(now.plusSeconds(employeeIndex * REQUESTS_PER_EMPLOYEE + requestIndex)), "{}",
                        "performance-" + employeeIndex + '-' + requestIndex);
            }
        }
        jdbc.execute("analyze employee_profile");
        jdbc.execute("analyze leave_request");
    }

    @Test
    void approvedIndexesExistForMeasuredQueries() {
        var indexNames = jdbc.queryForList("select indexname from pg_indexes where schemaname='public'", String.class);

        assertThat(jdbc.queryForObject("select count(*) from employee_profile", Integer.class)).isEqualTo(EMPLOYEE_COUNT);
        assertThat(jdbc.queryForObject("select count(*) from leave_request", Integer.class)).isEqualTo(EMPLOYEE_COUNT * REQUESTS_PER_EMPLOYEE);
        assertThat(indexNames).contains(
                "ix_request_owner_submitted",
                "ix_request_owner_status_start",
                "ix_request_status_start",
                "ix_employee_manager"
        );
    }

    @Test
    void interactiveAndCalendarQueriesMeetTheP95Target() {
        var historyPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "submittedAt"));
        assertP95Below("employee request history", INTERACTIVE_TARGET,
                () -> requests.findAllByEmployeeId(viewerId, historyPage));
        assertP95Below("manager team calendar", INTERACTIVE_TARGET,
                () -> requests.findManagerTeamCalendar(managerId, periodStart, periodEnd));
        assertP95Below("employee team agenda", INTERACTIVE_TARGET,
                () -> requests.findEmployeeTeamCalendar(viewerId));
    }

    @Test
    void paginatedAndAggregateReportQueriesMeetTheP95Target() {
        var reportPage = PageRequest.of(0, 20);
        assertP95Below("organization report page and summaries", REPORT_TARGET, () -> {
            organizationRequests.search(periodStart, periodEnd, null, reportPage);
            summaries.byStatus(periodStart, periodEnd);
            summaries.byLeaveType(periodStart, periodEnd);
        });
    }

    private void assertP95Below(String queryName, Duration target, Runnable query) {
        for (int warmup = 0; warmup < 3; warmup++) query.run();
        var samples = new long[MEASURED_RUNS];
        for (int run = 0; run < MEASURED_RUNS; run++) {
            var started = System.nanoTime();
            query.run();
            samples[run] = System.nanoTime() - started;
        }
        Arrays.sort(samples);
        var percentileIndex = (int) Math.ceil(MEASURED_RUNS * 0.95) - 1;
        assertThat(samples[percentileIndex])
                .as("%s p95 query time must remain below %s", queryName, target)
                .isLessThan(target.toNanos());
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
