package com.example.leavemanagement.calendar.api;
import com.example.leavemanagement.calendar.application.EmployeeDashboardService;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/employee/dashboard")
public class EmployeeDashboardController {
 private final EmployeeDashboardService dashboards;private final CurrentActorProvider actors;public EmployeeDashboardController(EmployeeDashboardService dashboards,CurrentActorProvider actors){this.dashboards=dashboards;this.actors=actors;}
 @GetMapping public EmployeeDashboardService.Dashboard dashboard(){return dashboards.view(actors.require().employeeId());}
}

