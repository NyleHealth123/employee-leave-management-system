package com.example.leavemanagement.unit;
import com.example.leavemanagement.request.domain.CancellationPolicy;
import com.example.leavemanagement.request.persistence.LeaveRequestEntity;
import com.example.leavemanagement.policy.persistence.LeavePolicyVersionEntity;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancellationPolicyTest {
  @Test void pendingOwnedRequestIsEligible(){var r=mock(LeaveRequestEntity.class);var p=mock(LeavePolicyVersionEntity.class);when(r.getStatus()).thenReturn("PENDING");when(r.getEmployeeId()).thenReturn(UUID.randomUUID());assertThat(CancellationPolicy.evaluate(r,p,x->true,Instant.parse("2026-08-20T00:00:00Z"),ZoneId.of("Asia/Kolkata")).allowed()).isTrue();}
  @Test void approvedRequestAtCutoffIsBlocked(){var r=mock(LeaveRequestEntity.class);var p=mock(LeavePolicyVersionEntity.class);var start=LocalDate.of(2026,8,22);when(r.getStatus()).thenReturn("APPROVED");when(r.getStartDate()).thenReturn(start);when(p.getCancellationCutoffDays()).thenReturn(1);var cutoff=start.atStartOfDay(ZoneId.of("Asia/Kolkata")).minusDays(1).toInstant();assertThat(CancellationPolicy.evaluate(r,p,x->true,cutoff,ZoneId.of("Asia/Kolkata")).allowed()).isFalse();}
  @Test void approvedFutureLeaveBeforeCutoffIsAllowed(){var r=mock(LeaveRequestEntity.class);var p=mock(LeavePolicyVersionEntity.class);when(r.getStatus()).thenReturn("APPROVED");when(r.getStartDate()).thenReturn(LocalDate.of(2026,8,22));when(p.getCancellationCutoffDays()).thenReturn(1);assertThat(CancellationPolicy.evaluate(r,p,x->true,Instant.parse("2026-08-20T00:00:00Z"),ZoneId.of("Asia/Kolkata")).allowed()).isTrue();}
  @Test void cutoffUsesOrganizationTimezoneAtExactInstant(){var r=mock(LeaveRequestEntity.class);var p=mock(LeavePolicyVersionEntity.class);when(r.getStatus()).thenReturn("APPROVED");when(r.getStartDate()).thenReturn(LocalDate.of(2026,8,22));when(p.getCancellationCutoffDays()).thenReturn(1);var zone=ZoneId.of("Asia/Kolkata");var cutoff=LocalDate.of(2026,8,21).atStartOfDay(zone).toInstant();assertThat(CancellationPolicy.evaluate(r,p,x->true,cutoff.minusNanos(1),zone).allowed()).isTrue();assertThat(CancellationPolicy.evaluate(r,p,x->true,cutoff,zone).allowed()).isFalse();}
  @Test void terminalAndForeignRequestsAreBlocked(){var r=mock(LeaveRequestEntity.class);var p=mock(LeavePolicyVersionEntity.class);when(r.getStatus()).thenReturn("CANCELLED");assertThat(CancellationPolicy.evaluate(r,p,x->false,Instant.now(),ZoneId.of("UTC")).allowed()).isFalse();}
}
