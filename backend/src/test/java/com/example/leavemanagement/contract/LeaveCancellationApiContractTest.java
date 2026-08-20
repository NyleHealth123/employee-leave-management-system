package com.example.leavemanagement.contract;

import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.request.api.*;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.request.persistence.LeaveRequestEntity;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({LeaveCancellationController.class,AdminCorrectionController.class}) @Import(SecurityConfiguration.class)
class LeaveCancellationApiContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean CancelLeaveRequestService cancellations; @MockitoBean ExceptionalCorrectionService corrections; @MockitoBean EmployeeLeaveRequestQueryService queries; @MockitoBean CurrentActorProvider actors; @MockitoBean AuthenticationManager authentication; @MockitoBean AccountUserDetailsService users;
    private final UUID id=UUID.randomUUID();

    @Test void employeeCancellationSuccessPassesLoadedVersion() throws Exception {
        var actor=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("EMPLOYEE"),"Employee"); var request=LeaveRequestEntity.pending(actor.employeeId(),UUID.randomUUID(),UUID.randomUUID(),java.time.LocalDate.now(),java.time.LocalDate.now(),"FULL_DAY",2,"reason","{}","key");
        when(actors.require()).thenReturn(actor); when(cancellations.cancel(actor,id,3,"changed")).thenReturn(request); when(queries.detail(request)).thenReturn(null);
        mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":3,\"comment\":\"changed\"}" )).andExpect(status().isOk()); verify(cancellations).cancel(actor,id,3,"changed");
    }
    @Test void administratorCorrectionSuccessPassesClosedActionReasonAndVersion() throws Exception {
        var actor=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("ADMINISTRATOR"),"Admin"); var request=LeaveRequestEntity.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),java.time.LocalDate.now(),java.time.LocalDate.now(),"FULL_DAY",2,"reason","{}","key");
        when(actors.require()).thenReturn(actor); when(corrections.correct(actor,id,"CANCEL_PENDING","policy fix",4)).thenReturn(request); when(queries.detail(request)).thenReturn(null);
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("ADMINISTRATOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"CANCEL_PENDING\",\"reason\":\"policy fix\",\"expectedVersion\":4}" )).andExpect(status().isOk()); verify(corrections).correct(actor,id,"CANCEL_PENDING","policy fix",4);
    }
    @Test void missingExpectedVersionAndInvalidCorrectionAreValidationErrors() throws Exception {
        mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("ADMINISTRATOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"NOPE\",\"reason\":\"x\",\"expectedVersion\":0}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")); verifyNoInteractions(corrections);
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("ADMINISTRATOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"CANCEL_PENDING\",\"reason\":\"\",\"expectedVersion\":0}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
    @Test void employeeAndManagerCannotUseCorrectionEndpoint() throws Exception {
        var body="{\"action\":\"CANCEL_PENDING\",\"reason\":\"x\",\"expectedVersion\":0}";
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("MANAGER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden()); verifyNoInteractions(corrections);
    }
    @Test void staleAndForeignCancellationAreMappedWithoutMutation() throws Exception {
        var actor=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("EMPLOYEE"),"Employee"); when(actors.require()).thenReturn(actor); when(cancellations.cancel(eq(actor),eq(id),eq(2L),any())).thenThrow(new DomainException(409,"STALE_VERSION","The leave request changed; refresh and retry"));
        mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":2}" )).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STALE_VERSION"));
        when(cancellations.cancel(eq(actor),eq(id),eq(3L),any())).thenThrow(new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found")); mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":3}" )).andExpect(status().isNotFound()); verify(cancellations,times(2)).cancel(eq(actor),eq(id),anyLong(),any());
    }
    @Test void cutoffAndForbiddenTransitionProblemsAreStable() throws Exception {
        var actor=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("EMPLOYEE"),"Employee"); when(actors.require()).thenReturn(actor); when(cancellations.cancel(eq(actor),eq(id),eq(1L),any())).thenThrow(new DomainException(422,"CANCELLATION_CUTOFF_PASSED","The cancellation cutoff has passed"));
        mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}" )).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CANCELLATION_CUTOFF_PASSED"));
        var admin=new CurrentActor(UUID.randomUUID(),UUID.randomUUID(),null,Set.of("ADMINISTRATOR"),"Admin"); when(actors.require()).thenReturn(admin); when(corrections.correct(eq(admin),eq(id),eq("CANCEL_APPROVED"),eq("fix"),eq(1L))).thenThrow(new DomainException(409,"INVALID_STATUS_TRANSITION","Correction action does not match the current status"));
        mvc.perform(post("/api/admin/leave-requests/"+id+"/corrections").with(user("u").roles("ADMINISTRATOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"CANCEL_APPROVED\",\"reason\":\"fix\",\"expectedVersion\":1}" )).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }
    @Test void csrfIsRequiredForUnsafePhase6Operations() throws Exception {mvc.perform(post("/api/employee/leave-requests/"+id+"/cancel").with(user("u").roles("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}" )).andExpect(status().isForbidden());}
}
