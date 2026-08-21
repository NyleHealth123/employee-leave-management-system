package com.example.leavemanagement.security;

import com.example.leavemanagement.audit.application.AuditQueryService;
import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.calendar.application.EmployeeTeamCalendarService;
import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.reporting.application.LeaveReportingService;
import com.example.leavemanagement.reporting.persistence.*;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthorizationMatrixTest {
    @Test void employeePrivateDetailAlwaysUsesTheAuthenticatedOwnerScope() {
        var requests=mock(LeaveRequestRepository.class);var id=UUID.randomUUID();var owner=UUID.randomUUID();var other=UUID.randomUUID();
        when(requests.findByIdAndEmployeeId(id,owner)).thenReturn(Optional.empty());
        var service=new EmployeeLeaveRequestQueryService(requests,mock(LeaveRequestStatusHistoryRepository.class),mock(LeavePolicyQueryService.class),mock(EmployeeProfileRepository.class),Clock.systemUTC());
        assertThatThrownBy(()->service.detail(owner,id)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
        verify(requests).findByIdAndEmployeeId(id,owner);verify(requests,never()).findByIdAndEmployeeId(id,other);verify(requests,never()).findById(id);
    }

    @Test void managerScopeIsDirectReportOnlyAndUnrelatedResourcesRemainHidden() {
        var requests=mock(LeaveRequestRepository.class);var manager=UUID.randomUUID();var id=UUID.randomUUID();
        when(requests.findDirectReport(id,manager)).thenReturn(Optional.empty());
        var service=new ManagerLeaveRequestQueryService(requests,mock(LeaveRequestStatusHistoryRepository.class),mock(EmployeeProfileRepository.class),mock(LeavePolicyQueryService.class),mock(LeaveRequestBalanceLineRepository.class),mock(LeaveBalanceRepository.class));
        assertThatThrownBy(()->service.detail(manager,id)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
        verify(requests).findDirectReport(id,manager);verify(requests,never()).findById(id);
    }

    @Test void employeeTeamCalendarUsesDedicatedPrivacyProjectionAndViewerPredicate() {
        var requests=mock(LeaveRequestRepository.class);var viewer=UUID.randomUUID();var row=mock(LeaveRequestRepository.TeamCalendarRow.class);
        when(row.getEmployeeDisplayName()).thenReturn("Coworker");when(row.getStartDate()).thenReturn(LocalDate.of(2026,9,1));when(row.getEndDate()).thenReturn(LocalDate.of(2026,9,2));when(row.getStatus()).thenReturn("PENDING");
        when(requests.findEmployeeTeamCalendar(viewer)).thenReturn(List.of(row));
        var entries=new EmployeeTeamCalendarService(requests).entries(viewer,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30));
        assertThat(entries).singleElement().satisfies(e->assertThat(e.status()).isEqualTo("PENDING"));
        assertThat(Arrays.stream(EmployeeTeamCalendarService.Entry.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("employeeDisplayName","startDate","endDate","status");
        verify(requests).findEmployeeTeamCalendar(viewer);
    }

    @Test void managerAndAdministratorRolesDoNotBypassEachOthersEndpointSemantics() {
        var requests=mock(LeaveRequestRepository.class);var service=managerDecision(requests);
        var adminOnly=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("ADMINISTRATOR"),"Admin");
        assertThatThrownBy(()->service.decide(adminOnly,UUID.randomUUID(),0,null,true)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("ACCESS_DENIED");
        verifyNoInteractions(requests);
        var multi=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("EMPLOYEE","MANAGER","ADMINISTRATOR"),"Multi");var requestId=UUID.randomUUID();
        when(requests.lockDirectReport(requestId,multi.employeeId())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.decide(multi,requestId,0,null,true)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
        verify(requests).lockDirectReport(requestId,multi.employeeId());
    }

    @Test void organizationReportingAndAuditRequireAdministratorBeforeRepositoryAccess() {
        var actors=mock(CurrentActorProvider.class);when(actors.require()).thenReturn(new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("EMPLOYEE","MANAGER"),"User"));
        var organization=mock(OrganizationLeaveRequestRepository.class);var summaries=mock(LeaveSummaryRepository.class);
        var reporting=new LeaveReportingService(organization,summaries,actors);
        assertThatThrownBy(()->reporting.list(LocalDate.now(),LocalDate.now(),null,0,20)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("ACCESS_DENIED");
        verifyNoInteractions(organization,summaries);
        var events=mock(AuditEventRepository.class);var audit=new AuditQueryService(events,actors,new ObjectMapper());
        assertThatThrownBy(()->audit.list(0,20,null,null)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("ACCESS_DENIED");
        verifyNoInteractions(events);
    }

    @Test void deniedManagerWriteHasZeroDomainMutationAcrossAllCollaborators() {
        var requests=mock(LeaveRequestRepository.class);var lines=mock(LeaveRequestBalanceLineRepository.class);var balances=mock(LeaveBalanceRepository.class);var movements=mock(LeaveBalanceMovementRepository.class);var slots=mock(LeaveRequestSlotRepository.class);var history=mock(LeaveRequestStatusHistoryRepository.class);var audits=mock(AuditEventRepository.class);
        var service=new ManagerDecisionService(requests,lines,balances,movements,slots,history,audits,mock(LeavePolicyQueryService.class),mock(ManagerLeaveRequestQueryService.class));
        var actor=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("MANAGER"),"Manager");var id=UUID.randomUUID();when(requests.lockDirectReport(id,actor.employeeId())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.decide(actor,id,0,null,false)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
        verify(requests).lockDirectReport(id,actor.employeeId());verifyNoInteractions(lines,balances,movements,slots,history,audits);
    }

    private static ManagerDecisionService managerDecision(LeaveRequestRepository requests){return new ManagerDecisionService(requests,mock(LeaveRequestBalanceLineRepository.class),mock(LeaveBalanceRepository.class),mock(LeaveBalanceMovementRepository.class),mock(LeaveRequestSlotRepository.class),mock(LeaveRequestStatusHistoryRepository.class),mock(AuditEventRepository.class),mock(LeavePolicyQueryService.class),mock(ManagerLeaveRequestQueryService.class));}
}
