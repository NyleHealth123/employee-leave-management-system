package com.example.leavemanagement.integration;

import com.example.leavemanagement.reporting.persistence.LeaveSummaryRepository;
import com.example.leavemanagement.reporting.persistence.OrganizationLeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;

import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LeaveReportingRepositoryTest extends PostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationLeaveRequestRepository requests;
    @Autowired LeaveSummaryRepository summaries;

    @BeforeEach void clean() { jdbc.execute("truncate table user_account, leave_type restart identity cascade"); }

    @Test void inclusivePeriodFiltersAndStablePagination() {
        var f = fixture();
        insertRequest(f, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "PENDING", 1, "one");
        insertRequest(f, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10), "APPROVED", 2, "two");
        var page = requests.search(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10), null, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test void summariesUseStoredUnitsAndSeparateStatusesAndTypes() {
        var f = fixture(); insertRequest(f, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), "PENDING", 1, "half"); insertRequest(f, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 2), "APPROVED", 2, "full");
        assertThat(summaries.byStatus(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2))).extracting(LeaveSummaryRepository.Bucket::chargeableDays).containsExactly(1.0, 0.5);
        assertThat(summaries.byLeaveType(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2))).hasSize(1).first().extracting(LeaveSummaryRepository.Bucket::chargeableDays).isEqualTo(1.5);
    }

    private Fixture fixture() { var user=UUID.randomUUID(); var employee=UUID.randomUUID(); var type=UUID.randomUUID(); var now=Timestamp.from(Instant.now()); jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)",user,"u"+user,"u"+user,"hash",true,now,now,now,0); jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)",employee,"E"+employee,user,"Employee","e"+employee+"@x.test",true,now,now); jdbc.update("insert into leave_type values (?,?,?,?,?,0)",type,"ANNUAL"+type,"Annual",null,true); var policy=UUID.randomUUID(); jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)",policy,type,1,LocalDate.of(2026,1,1),null,true,true,"EXCLUDE","EXCLUDE",false,1,now); return new Fixture(employee,type,policy); }
    private void insertRequest(Fixture f, LocalDate start, LocalDate end, String status, int units, String key) { jdbc.update("insert into leave_request(id,employee_id,leave_type_id,submitted_policy_version_id,start_date,end_date,duration_mode,chargeable_units,reason,status,submitted_at,policy_snapshot,idempotency_key,version) values (?,?,?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,0)",UUID.randomUUID(),f.employee(),f.type(),f.policy(),start,end,"FULL_DAY",units,"reason",status,Timestamp.from(Instant.now()),"{}",key); }
    private record Fixture(UUID employee, UUID type, UUID policy) {}
}
