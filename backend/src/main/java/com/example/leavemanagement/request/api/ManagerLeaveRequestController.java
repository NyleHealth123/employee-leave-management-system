package com.example.leavemanagement.request.api;

import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/manager/leave-requests")
public class ManagerLeaveRequestController {
 private final ManagerLeaveRequestQueryService queries; private final ManagerDecisionService decisions; private final CurrentActorProvider actors;
 public ManagerLeaveRequestController(ManagerLeaveRequestQueryService queries,ManagerDecisionService decisions,CurrentActorProvider actors){this.queries=queries;this.decisions=decisions;this.actors=actors;}
 @GetMapping public ManagerLeaveRequestQueryService.PageView list(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) @Pattern(regexp="PENDING|APPROVED|REJECTED|CANCELLED") String status){var a=actors.require();return queries.list(a.employeeId(),page,size,status);}
 @GetMapping("/{id}") public ManagerLeaveRequestQueryService.Detail detail(@PathVariable UUID id){var a=actors.require();return queries.detail(a.employeeId(),id);}
 @PostMapping("/{id}/approve") public EmployeeLeaveRequestQueryService.Detail approve(@PathVariable UUID id,@Valid @RequestBody DecisionCommand command){var a=actors.require();return queries.decisionDetail(decisions.decide(a,id,command.expectedVersion(),command.comment(),true));}
 @PostMapping("/{id}/reject") public EmployeeLeaveRequestQueryService.Detail reject(@PathVariable UUID id,@Valid @RequestBody DecisionCommand command){var a=actors.require();return queries.decisionDetail(decisions.decide(a,id,command.expectedVersion(),command.comment(),false));}
 public record DecisionCommand(@NotNull @Min(0) Long expectedVersion,@Size(max=1000) String comment){}
}
