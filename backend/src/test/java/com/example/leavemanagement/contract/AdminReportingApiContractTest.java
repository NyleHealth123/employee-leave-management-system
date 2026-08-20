package com.example.leavemanagement.contract;

import com.example.leavemanagement.audit.application.AuditQueryService;
import com.example.leavemanagement.reporting.application.LeaveReportingService;
import com.example.leavemanagement.shared.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({com.example.leavemanagement.reporting.api.AdminReportingController.class, com.example.leavemanagement.audit.api.AdminAuditController.class})
@Import(SecurityConfiguration.class)
class AdminReportingApiContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean LeaveReportingService reporting;
    @MockitoBean AuditQueryService audits;
    @MockitoBean com.example.leavemanagement.shared.security.CurrentActorProvider actors;
    @MockitoBean org.springframework.security.authentication.AuthenticationManager authentication;
    @MockitoBean com.example.leavemanagement.auth.application.AccountUserDetailsService users;
    @Test void administratorCanReadReportsAndAudit() throws Exception { when(reporting.list(any(),any(),any(),anyInt(),anyInt())).thenReturn(new LeaveReportingService.PageView(List.of(),0,20,0,0)); when(reporting.summary(any(),any())).thenReturn(new LeaveReportingService.SummaryReport(LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),List.of(),List.of())); when(audits.list(anyInt(),anyInt(),any(),any())).thenReturn(new AuditQueryService.PageView(List.of(),0,20,0,0)); mvc.perform(get("/api/admin/leave-requests?from=2026-01-01&to=2026-01-31").with(user("u").roles("ADMINISTRATOR"))).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray()); mvc.perform(get("/api/admin/reports/leave-summary?from=2026-01-01&to=2026-01-31").with(user("u").roles("ADMINISTRATOR"))).andExpect(status().isOk()).andExpect(jsonPath("$.byStatus").isArray()); mvc.perform(get("/api/admin/audit-events").with(user("u").roles("ADMINISTRATOR"))).andExpect(status().isOk()); }
    @Test void employeeAndManagerCannotReadOrganizationData() throws Exception { mvc.perform(get("/api/admin/leave-requests?from=2026-01-01&to=2026-01-31").with(user("u").roles("EMPLOYEE"))).andExpect(status().isForbidden()); mvc.perform(get("/api/admin/audit-events").with(user("u").roles("MANAGER"))).andExpect(status().isForbidden()); verifyNoInteractions(reporting,audits); }
    @Test void invalidPeriodAndPageAreBadRequest() throws Exception { when(reporting.list(any(),any(),any(),anyInt(),anyInt())).thenThrow(new com.example.leavemanagement.shared.api.DomainException(400,"VALIDATION_FAILED","bad")); mvc.perform(get("/api/admin/leave-requests?from=2026-02-01&to=2026-01-01").with(user("u").roles("ADMINISTRATOR"))).andExpect(status().isBadRequest()); mvc.perform(get("/api/admin/audit-events?page=-1").with(user("u").roles("ADMINISTRATOR"))).andExpect(status().isBadRequest()); }
}
