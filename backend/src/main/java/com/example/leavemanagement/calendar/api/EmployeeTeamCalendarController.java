package com.example.leavemanagement.calendar.api;
import com.example.leavemanagement.calendar.application.EmployeeTeamCalendarService;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/employee/team-calendar")
public class EmployeeTeamCalendarController {
 private final EmployeeTeamCalendarService calendars;private final CurrentActorProvider actors;public EmployeeTeamCalendarController(EmployeeTeamCalendarService calendars,CurrentActorProvider actors){this.calendars=calendars;this.actors=actors;}
 @GetMapping public List<EmployeeTeamCalendarService.Entry> calendar(@RequestParam LocalDate from,@RequestParam LocalDate to){return calendars.entries(actors.require().employeeId(),from,to);}
}

