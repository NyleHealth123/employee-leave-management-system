package com.example.leavemanagement.contract;
import com.example.leavemanagement.auth.application.AccountUserDetailsService;
import com.example.leavemanagement.balance.api.EmployeeBalanceController;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.calendar.api.*;
import com.example.leavemanagement.calendar.application.*;
import com.example.leavemanagement.policy.api.LeaveTypeController;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.api.EmployeeLeaveRequestController;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest({EmployeeLeaveRequestController.class,EmployeeBalanceController.class,LeaveTypeController.class,HolidayController.class,EmployeeDashboardController.class,EmployeeTeamCalendarController.class}) @Import(SecurityConfiguration.class)
class EmployeeLeaveApiContractTest {
 @Autowired MockMvc mvc;@MockitoBean LeaveCalculationService calculations;@MockitoBean LeaveSubmissionService submissions;@MockitoBean EmployeeLeaveRequestQueryService queries;@MockitoBean LeaveBalanceService balances;@MockitoBean LeavePolicyQueryService policies;@MockitoBean HolidayQueryService holidays;@MockitoBean EmployeeDashboardService dashboards;@MockitoBean EmployeeTeamCalendarService calendars;@MockitoBean CurrentActorProvider actors;@MockitoBean AuthenticationManager manager;@MockitoBean AccountUserDetailsService users;@MockitoBean java.time.Clock clock;
 private final UUID userId=UUID.randomUUID(),employeeId=UUID.randomUUID(),typeId=UUID.randomUUID();
 @BeforeEach void actor(){when(actors.require()).thenReturn(new CurrentActor(userId,employeeId,null,Set.of("EMPLOYEE"),"Viewer"));}
 @Test void missingCsrfOnCalculationAndSubmissionIsForbiddenWithoutMutation()throws Exception{var body="{\"leaveTypeId\":\""+typeId+"\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-01\",\"durationMode\":\"FULL_DAY\",\"reason\":\"Rest\"}";mvc.perform(post("/api/employee/leave-requests/calculate").with(user(userId.toString()).roles("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));mvc.perform(post("/api/employee/leave-requests").with(user(userId.toString()).roles("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());verifyNoInteractions(calculations,submissions);}
 @Test void invalidCsrfOnCalculationAndSubmissionIsForbiddenWithoutMutation()throws Exception{var body="{\"leaveTypeId\":\""+typeId+"\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-01\",\"durationMode\":\"FULL_DAY\",\"reason\":\"Rest\"}";mvc.perform(post("/api/employee/leave-requests/calculate").header("X-XSRF-TOKEN","invalid").with(user(userId.toString()).roles("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));mvc.perform(post("/api/employee/leave-requests").header("X-XSRF-TOKEN","invalid").with(user(userId.toString()).roles("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());verifyNoInteractions(calculations,submissions);}
 @Test void teamCalendarUsesExactPrivacyProjection()throws Exception{when(calendars.entries(eq(employeeId),any(),any())).thenReturn(List.of(new EmployeeTeamCalendarService.Entry("Coworker",LocalDate.of(2026,9,1),LocalDate.of(2026,9,2),"PENDING")));mvc.perform(get("/api/employee/team-calendar?from=2026-09-01&to=2026-09-30").with(user(userId.toString()).roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].employeeDisplayName").value("Coworker")).andExpect(jsonPath("$[0].startDate").value("2026-09-01")).andExpect(jsonPath("$[0].status").value("PENDING")).andExpect(jsonPath("$[0].reason").doesNotExist()).andExpect(jsonPath("$[0].leaveTypeId").doesNotExist()).andExpect(jsonPath("$[0].employeeId").doesNotExist());}
 @Test void ownHistoryNeverAcceptsAnEmployeeIdentifier()throws Exception{when(queries.list(employeeId,0,20)).thenReturn(new EmployeeLeaveRequestQueryService.PageView(List.of(),0,20,0,0));mvc.perform(get("/api/employee/leave-requests?page=0&size=20&employeeId="+UUID.randomUUID()).with(user(userId.toString()).roles("EMPLOYEE"))).andExpect(status().isOk());verify(queries).list(employeeId,0,20);}
 @Test void openApiContainsEveryUs1OperationAndResolvableSchemaReference()throws Exception{var source=Files.readString(Path.of("..","specs","001-employee-leave-management","contracts","openapi.yaml"));Map<String,Object> document=new Yaml().load(source);assertThat(document.get("openapi")).isEqualTo("3.1.0");assertThat(source).contains("operationId: listAvailableLeaveTypes","operationId: listCompanyHolidays","operationId: listOwnLeaveBalances","operationId: calculateOwnLeaveRequest","operationId: submitOwnLeaveRequest","operationId: listOwnLeaveRequests","operationId: getOwnLeaveRequest","operationId: getEmployeeDashboard","operationId: getEmployeeTeamCalendar");var schemas=(Map<?,?>)((Map<?,?>)document.get("components")).get("schemas");var references=java.util.regex.Pattern.compile("#/components/schemas/([A-Za-z0-9]+)").matcher(source);while(references.find())assertThat(schemas.containsKey(references.group(1))).as("schema reference %s",references.group(1)).isTrue();}
}
