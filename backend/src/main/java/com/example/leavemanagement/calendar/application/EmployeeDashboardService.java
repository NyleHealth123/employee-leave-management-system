package com.example.leavemanagement.calendar.application;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.request.application.EmployeeLeaveRequestQueryService;
import com.example.leavemanagement.request.persistence.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Clock;
import java.util.*;
@Service @Transactional(readOnly=true)
public class EmployeeDashboardService {
 private final LeaveBalanceService balances;private final LeaveRequestRepository requests;private final EmployeeLeaveRequestQueryService query;private final HolidayQueryService holidays;private final Clock clock;
 public EmployeeDashboardService(LeaveBalanceService balances,LeaveRequestRepository requests,EmployeeLeaveRequestQueryService query,HolidayQueryService holidays,Clock clock){this.balances=balances;this.requests=requests;this.query=query;this.holidays=holidays;this.clock=clock;}
 public Dashboard view(UUID employeeId){var today=LocalDate.now(clock);var pending=requests.findTop20ByEmployeeIdAndStatusOrderByStartDateAsc(employeeId,"PENDING").stream().map(query::summary).toList();var approved=requests.findTop20ByEmployeeIdAndStatusOrderByStartDateAsc(employeeId,"APPROVED").stream().filter(r->!r.getEndDate().isBefore(today)).map(query::summary).toList();return new Dashboard(balances.own(employeeId),pending,approved,holidays.list(today,today.plusYears(1)));}
 public record Dashboard(List<LeaveBalanceService.BalanceView> balances,List<EmployeeLeaveRequestQueryService.Summary> pendingRequests,List<EmployeeLeaveRequestQueryService.Summary> approvedUpcomingLeave,List<HolidayQueryService.HolidayView> upcomingHolidays){}
}
