package com.example.leavemanagement.security;

import com.example.leavemanagement.auth.api.AuthController;
import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.balance.api.AdminBalanceController;
import com.example.leavemanagement.balance.application.*;
import com.example.leavemanagement.people.api.AdminEmployeeController;
import com.example.leavemanagement.people.application.*;
import com.example.leavemanagement.policy.api.*;
import com.example.leavemanagement.policy.application.*;
import com.example.leavemanagement.request.api.*;
import com.example.leavemanagement.request.application.*;
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

import java.time.Clock;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class,EmployeeLeaveRequestController.class,LeaveCancellationController.class,
        ManagerLeaveRequestController.class,AdminEmployeeController.class,AdminPolicyController.class,
        AdminHolidayController.class,AdminBalanceController.class,AdminCorrectionController.class})
@Import(SecurityConfiguration.class)
class CsrfContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthenticationManager authentication; @MockitoBean AccountUserDetailsService users; @MockitoBean CurrentActorProvider actors;
    @MockitoBean LeaveCalculationService calculations; @MockitoBean LeaveSubmissionService submissions; @MockitoBean EmployeeLeaveRequestQueryService employeeQueries;
    @MockitoBean CancelLeaveRequestService cancellations; @MockitoBean ManagerLeaveRequestQueryService managerQueries; @MockitoBean ManagerDecisionService decisions;
    @MockitoBean EmployeeAdministrationService employees; @MockitoBean EmployeeAdministrationQueryService employeeQuery;
    @MockitoBean LeavePolicyAdministrationService policies; @MockitoBean HolidayAdministrationService holidays;
    @MockitoBean BalanceAllocationService allocations; @MockitoBean BalanceAdjustmentService adjustments; @MockitoBean ExceptionalCorrectionService corrections;
    @MockitoBean Clock clock;
    private final UUID id=UUID.randomUUID(), employee=UUID.randomUUID(), userId=UUID.randomUUID(), type=UUID.randomUUID();

    @BeforeEach void actor(){when(actors.require()).thenReturn(new CurrentActor(userId,employee,null,Set.of("EMPLOYEE","MANAGER","ADMINISTRATOR"),"Multi role"));}

    @Test void everyApprovedUnsafeOperationRejectsMissingAndInvalidCsrfBeforeDomainMutation() throws Exception {
        for(var command:unsafeCommands()){
            var missing=command.request();if(command.role()!=null)missing.with(user("u").roles(command.role()));
            mvc.perform(missing.contentType(MediaType.APPLICATION_JSON).content(command.body())).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            var invalid=command.request();if(command.role()!=null)invalid.with(user("u").roles(command.role()));
            mvc.perform(invalid.with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(command.body())).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        verifyNoInteractions(authentication,calculations,submissions,cancellations,decisions,employees,policies,holidays,allocations,adjustments,corrections);
    }

    @Test void authenticatedValidCsrfReachesTheApprovedMutationAndReadOnlyGetNeedsNoToken() throws Exception {
        var view=new EmployeeAdministrationService.View(id,"E1","Employee","e@example.com",null,null,Set.of("EMPLOYEE"),true,0);
        when(employees.create(any())).thenReturn(view);
        mvc.perform(post("/api/admin/employees").with(user("admin").roles("ADMINISTRATOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeNumber\":\"E1\",\"displayName\":\"Employee\",\"email\":\"e@example.com\",\"login\":\"e\",\"initialPassword\":\"correct horse battery\",\"roles\":[\"EMPLOYEE\"],\"active\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(employees).create(any());
        mvc.perform(get("/api/auth/me").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").exists());
    }

    private List<Unsafe> unsafeCommands(){return List.of(
            new Unsafe(()->post("/api/auth/login"),null,"{\"login\":\"e\",\"password\":\"secret\"}"),
            new Unsafe(()->post("/api/auth/logout"),"EMPLOYEE",""),
            new Unsafe(()->post("/api/employee/leave-requests/calculate"),"EMPLOYEE",requestBody()),
            new Unsafe(()->post("/api/employee/leave-requests"),"EMPLOYEE",requestBody()),
            new Unsafe(()->post("/api/employee/leave-requests/{id}/cancel",id),"EMPLOYEE","{\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/manager/leave-requests/{id}/approve",id),"MANAGER","{\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/manager/leave-requests/{id}/reject",id),"MANAGER","{\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/admin/employees"),"ADMINISTRATOR","{\"employeeNumber\":\"E1\",\"displayName\":\"E\",\"email\":\"e@example.com\",\"login\":\"e\",\"initialPassword\":\"correct horse battery\",\"roles\":[\"EMPLOYEE\"],\"active\":true}"),
            new Unsafe(()->patch("/api/admin/employees/{id}",id),"ADMINISTRATOR",employeeUpdate()),
            new Unsafe(()->post("/api/admin/leave-types"),"ADMINISTRATOR","{\"code\":\"A\",\"name\":\"Annual\",\"active\":true}"),
            new Unsafe(()->patch("/api/admin/leave-types/{id}",id),"ADMINISTRATOR","{\"code\":\"A\",\"name\":\"Annual\",\"active\":true,\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/admin/leave-types/{id}/policies",id),"ADMINISTRATOR","{\"effectiveFrom\":\"2026-01-01\",\"tracksBalance\":true,\"allowsHalfDay\":true,\"weeklyOffTreatment\":\"EXCLUDE\",\"holidayTreatment\":\"EXCLUDE\",\"rejectionCommentRequired\":false,\"cancellationCutoffDays\":1,\"weeklyOffDays\":[6,7],\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/admin/holidays"),"ADMINISTRATOR","{\"date\":\"2026-12-25\",\"name\":\"Holiday\",\"active\":true}"),
            new Unsafe(()->patch("/api/admin/holidays/{id}",id),"ADMINISTRATOR","{\"date\":\"2026-12-25\",\"name\":\"Holiday\",\"active\":false,\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/admin/employees/{employee}/leave-balances",employee),"ADMINISTRATOR","{\"leaveTypeId\":\""+type+"\",\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-12-31\",\"allocatedDays\":10,\"reason\":\"allocation\"}"),
            new Unsafe(()->post("/api/admin/employees/{employee}/leave-balances/{id}/adjustments",employee,id),"ADMINISTRATOR","{\"adjustmentDays\":1,\"reason\":\"fix\",\"expectedVersion\":0}"),
            new Unsafe(()->post("/api/admin/leave-requests/{id}/corrections",id),"ADMINISTRATOR","{\"action\":\"CANCEL_PENDING\",\"reason\":\"fix\",\"expectedVersion\":0}"));}
    private String requestBody(){return "{\"leaveTypeId\":\""+type+"\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-01\",\"durationMode\":\"FULL_DAY\",\"reason\":\"Rest\"}";}
    private String employeeUpdate(){return "{\"employeeNumber\":\"E1\",\"displayName\":\"E\",\"email\":\"e@example.com\",\"login\":\"e\",\"roles\":[\"EMPLOYEE\"],\"active\":true,\"expectedVersion\":0}";}
    private record Unsafe(java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> factory,String role,String body){org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(){return factory.get();}}
}
