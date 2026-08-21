package com.example.leavemanagement.contract;

import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.balance.api.AdminBalanceController;
import com.example.leavemanagement.balance.application.*;
import com.example.leavemanagement.people.api.AdminEmployeeController;
import com.example.leavemanagement.people.application.*;
import com.example.leavemanagement.policy.api.*;
import com.example.leavemanagement.policy.application.*;
import com.example.leavemanagement.request.api.*;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ManagerLeaveRequestController.class, LeaveCancellationController.class, AdminCorrectionController.class,
        AdminEmployeeController.class, AdminPolicyController.class, AdminHolidayController.class, AdminBalanceController.class})
@Import(SecurityConfiguration.class)
class ExpectedVersionContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean ManagerLeaveRequestQueryService managerQueries; @MockitoBean ManagerDecisionService decisions;
    @MockitoBean CancelLeaveRequestService cancellations; @MockitoBean ExceptionalCorrectionService corrections;
    @MockitoBean EmployeeLeaveRequestQueryService employeeQueries; @MockitoBean EmployeeAdministrationService employees;
    @MockitoBean EmployeeAdministrationQueryService employeeQuery; @MockitoBean LeavePolicyAdministrationService policies;
    @MockitoBean HolidayAdministrationService holidays; @MockitoBean BalanceAllocationService allocations;
    @MockitoBean BalanceAdjustmentService adjustments; @MockitoBean CurrentActorProvider actors;
    @MockitoBean AuthenticationManager authentication; @MockitoBean AccountUserDetailsService users;
    private final UUID id=UUID.randomUUID(), employeeId=UUID.randomUUID(), userId=UUID.randomUUID();

    @BeforeEach void actor(){when(actors.require()).thenReturn(new CurrentActor(userId,employeeId,null,Set.of("EMPLOYEE","MANAGER","ADMINISTRATOR"),"Multi role"));}

    @Test void everyApprovedVersionSensitiveHttpCommandRejectsAMissingExpectedVersion() throws Exception {
        for (var command : missingVersionCommands())
            mvc.perform(command.request().with(user("multi").roles(command.role())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(command.body()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(decisions,cancellations,corrections,employees,policies,holidays,adjustments);
    }

    @Test void staleVersionIs409SafeAndDoesNotDiscloseCurrentVersion() throws Exception {
        when(decisions.decide(any(),eq(id),eq(1L),any(),eq(true))).thenThrow(stale());
        var result=mvc.perform(post("/api/manager/leave-requests/{id}/approve",id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STALE_VERSION"))
                .andExpect(jsonPath("$.currentVersion").doesNotExist()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("expectedVersion", "current version", "version\":");
        verify(decisions).decide(any(),eq(id),eq(1L),isNull(),eq(true));
        verifyNoInteractions(cancellations,corrections,employees,policies,holidays,adjustments);
    }

    @Test void staleVersionResponseMatrixCoversEveryApprovedVersionSensitiveContract() throws Exception {
        when(decisions.decide(any(),eq(id),eq(1L),any(),anyBoolean())).thenThrow(stale());
        when(cancellations.cancel(any(),eq(id),eq(1L),any())).thenThrow(stale());
        when(corrections.correct(any(),eq(id),anyString(),anyString(),eq(1L))).thenThrow(stale());
        when(employees.update(eq(id),any())).thenThrow(stale());
        when(policies.updateType(eq(id),any())).thenThrow(stale());
        when(policies.createPolicy(eq(id),any())).thenThrow(stale());
        when(holidays.update(eq(id),any())).thenThrow(stale());
        when(adjustments.adjust(eq(id),any(),any())).thenThrow(stale());
        assertStale(post("/api/manager/leave-requests/{id}/approve",id),"MANAGER","{\"expectedVersion\":1}");
        assertStale(post("/api/manager/leave-requests/{id}/reject",id),"MANAGER","{\"expectedVersion\":1}");
        assertStale(post("/api/employee/leave-requests/{id}/cancel",id),"EMPLOYEE","{\"expectedVersion\":1}");
        assertStale(post("/api/admin/leave-requests/{id}/corrections",id),"ADMINISTRATOR","{\"action\":\"CANCEL_PENDING\",\"reason\":\"fix\",\"expectedVersion\":1}");
        assertStale(patch("/api/admin/employees/{id}",id),"ADMINISTRATOR",employeeUpdate(1));
        assertStale(patch("/api/admin/leave-types/{id}",id),"ADMINISTRATOR","{\"code\":\"A\",\"name\":\"Annual\",\"active\":true,\"expectedVersion\":1}");
        assertStale(post("/api/admin/leave-types/{id}/policies",id),"ADMINISTRATOR","{\"effectiveFrom\":\"2026-01-01\",\"tracksBalance\":true,\"allowsHalfDay\":true,\"weeklyOffTreatment\":\"EXCLUDE\",\"holidayTreatment\":\"EXCLUDE\",\"rejectionCommentRequired\":false,\"cancellationCutoffDays\":1,\"weeklyOffDays\":[6,7],\"expectedVersion\":1}");
        assertStale(patch("/api/admin/holidays/{id}",id),"ADMINISTRATOR","{\"date\":\"2026-12-25\",\"name\":\"Holiday\",\"active\":false,\"expectedVersion\":1}");
        assertStale(post("/api/admin/employees/{employeeId}/leave-balances/{id}/adjustments",employeeId,id),"ADMINISTRATOR","{\"adjustmentDays\":1,\"reason\":\"fix\",\"expectedVersion\":1}");
    }

    @Test void authorizationIsEnforcedBeforeOptimisticConcurrency() throws Exception {
        mvc.perform(post("/api/manager/leave-requests/{id}/reject",id).with(user("employee").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(patch("/api/admin/employees/{id}",id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(employeeUpdate(0)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(decisions,employees);
    }

    @Test void controllerVersionFieldsAreNullableAndValidationRequiredRatherThanPrimitiveDefaults() {
        for (var type : List.of(ManagerLeaveRequestController.DecisionCommand.class, LeaveCancellationController.CommentCommand.class,
                AdminCorrectionController.CorrectionCommand.class, AdminEmployeeController.Update.class,
                AdminPolicyController.TypeUpdate.class, AdminPolicyController.Policy.class,
                AdminHolidayController.Update.class, AdminBalanceController.Adjustment.class)) {
            var component=Arrays.stream(type.getRecordComponents()).filter(c->c.getName().equals("expectedVersion")).findFirst().orElseThrow();
            assertThat(component.getType()).as(type.getSimpleName()).isEqualTo(Long.class);
        }
    }

    private List<Command> missingVersionCommands(){return List.of(
            new Command(post("/api/manager/leave-requests/{id}/approve",id),"MANAGER","{}"),
            new Command(post("/api/manager/leave-requests/{id}/reject",id),"MANAGER","{}"),
            new Command(post("/api/employee/leave-requests/{id}/cancel",id),"EMPLOYEE","{}"),
            new Command(post("/api/admin/leave-requests/{id}/corrections",id),"ADMINISTRATOR","{\"action\":\"CANCEL_PENDING\",\"reason\":\"fix\"}"),
            new Command(patch("/api/admin/employees/{id}",id),"ADMINISTRATOR",employeeUpdate(null)),
            new Command(patch("/api/admin/leave-types/{id}",id),"ADMINISTRATOR","{\"code\":\"A\",\"name\":\"Annual\",\"active\":true}"),
            new Command(post("/api/admin/leave-types/{id}/policies",id),"ADMINISTRATOR","{\"effectiveFrom\":\"2026-01-01\",\"tracksBalance\":true,\"allowsHalfDay\":true,\"weeklyOffTreatment\":\"EXCLUDE\",\"holidayTreatment\":\"EXCLUDE\",\"rejectionCommentRequired\":false,\"cancellationCutoffDays\":1,\"weeklyOffDays\":[6,7]}"),
            new Command(patch("/api/admin/holidays/{id}",id),"ADMINISTRATOR","{\"date\":\"2026-12-25\",\"name\":\"Holiday\",\"active\":false}"),
            new Command(post("/api/admin/employees/{employeeId}/leave-balances/{id}/adjustments",employeeId,id),"ADMINISTRATOR","{\"adjustmentDays\":1,\"reason\":\"fix\"}"));}
    private String employeeUpdate(Object version){return "{\"employeeNumber\":\"E1\",\"displayName\":\"Employee\",\"email\":\"e@example.com\",\"login\":\"e\",\"roles\":[\"EMPLOYEE\"],\"active\":true"+(version==null?"":",\"expectedVersion\":"+version)+"}";}
    private void assertStale(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,String role,String body)throws Exception{var response=mvc.perform(request.with(user("u").roles(role)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STALE_VERSION")).andExpect(jsonPath("$.currentVersion").doesNotExist()).andReturn().getResponse().getContentAsString();assertThat(response).doesNotContain("expectedVersion","currentVersion");}
    private static DomainException stale(){return new DomainException(409,"STALE_VERSION","The resource changed; refresh and retry");}
    private record Command(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,String role,String body){}
}
