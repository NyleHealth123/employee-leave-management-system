package com.example.leavemanagement.unit;
import com.example.leavemanagement.request.domain.CancellationPolicy;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class CancellationPolicyTest {
  @Test void pendingOwnedRequestIsEligible(){assertThat(CancellationPolicy.evaluate(true,"PENDING",LocalDate.now(),0,Instant.parse("2026-08-20T00:00:00Z"),ZoneId.of("Asia/Kolkata")).allowed()).isTrue();}
  @Test void approvedRequestAtCutoffIsBlocked(){var start=LocalDate.of(2026,8,22);var zone=ZoneId.of("Asia/Kolkata");var cutoff=start.atStartOfDay(zone).minusDays(1).toInstant();assertThat(CancellationPolicy.evaluate(true,"APPROVED",start,1,cutoff,zone).allowed()).isFalse();}
  @Test void approvedFutureLeaveBeforeCutoffIsAllowed(){assertThat(CancellationPolicy.evaluate(true,"APPROVED",LocalDate.of(2026,8,22),1,Instant.parse("2026-08-20T00:00:00Z"),ZoneId.of("Asia/Kolkata")).allowed()).isTrue();}
  @Test void cutoffUsesOrganizationTimezoneAtExactInstant(){var start=LocalDate.of(2026,8,22);var zone=ZoneId.of("Asia/Kolkata");var cutoff=LocalDate.of(2026,8,21).atStartOfDay(zone).toInstant();assertThat(CancellationPolicy.evaluate(true,"APPROVED",start,1,cutoff.minusNanos(1),zone).allowed()).isTrue();assertThat(CancellationPolicy.evaluate(true,"APPROVED",start,1,cutoff,zone).allowed()).isFalse();}
  @Test void terminalAndForeignRequestsAreBlocked(){var start=LocalDate.now();assertThat(CancellationPolicy.evaluate(true,"CANCELLED",start,0,Instant.now(),ZoneId.of("UTC")).allowed()).isFalse();assertThat(CancellationPolicy.evaluate(false,"PENDING",start,0,Instant.now(),ZoneId.of("UTC")).allowed()).isFalse();}
}
