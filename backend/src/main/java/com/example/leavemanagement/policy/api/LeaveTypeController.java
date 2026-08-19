package com.example.leavemanagement.policy.api;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Clock;
import java.util.List;
@RestController @RequestMapping("/api/leave-types")
public class LeaveTypeController {
 private final LeavePolicyQueryService policies;private final Clock clock;public LeaveTypeController(LeavePolicyQueryService policies,Clock clock){this.policies=policies;this.clock=clock;}
 @GetMapping public List<LeavePolicyQueryService.LeaveTypeSummary> list(){return policies.activeTypes(LocalDate.now(clock));}
}
