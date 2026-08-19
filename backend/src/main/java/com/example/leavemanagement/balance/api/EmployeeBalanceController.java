package com.example.leavemanagement.balance.api;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/employee/leave-balances")
public class EmployeeBalanceController {
 private final LeaveBalanceService balances;private final CurrentActorProvider actors;public EmployeeBalanceController(LeaveBalanceService balances,CurrentActorProvider actors){this.balances=balances;this.actors=actors;}
 @GetMapping public List<LeaveBalanceService.BalanceView> own(){return balances.own(actors.require().employeeId());}
}

