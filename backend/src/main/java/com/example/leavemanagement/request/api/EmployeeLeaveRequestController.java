package com.example.leavemanagement.request.api;

import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.request.domain.DurationMode;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/employee/leave-requests")
public class EmployeeLeaveRequestController {
    private final LeaveCalculationService calculations;private final LeaveSubmissionService submissions;private final EmployeeLeaveRequestQueryService queries;private final CurrentActorProvider actors;
    public EmployeeLeaveRequestController(LeaveCalculationService calculations,LeaveSubmissionService submissions,EmployeeLeaveRequestQueryService queries,CurrentActorProvider actors){this.calculations=calculations;this.submissions=submissions;this.queries=queries;this.actors=actors;}
    @PostMapping("/calculate") public CalculationResponse calculate(@Valid @RequestBody RequestInput input){var actor=actors.require();return calculation(calculations.calculate(actor.employeeId(),input.toDomain()));}
    @PostMapping public ResponseEntity<EmployeeLeaveRequestQueryService.Detail> submit(@Valid @RequestBody RequestInput input,@RequestHeader(name="Idempotency-Key",required=false) String key){var actor=actors.require();var saved=submissions.submit(actor.userId(),actor.employeeId(),input.toDomain(),key);return ResponseEntity.status(HttpStatus.CREATED).body(queries.detail(saved));}
    @GetMapping public EmployeeLeaveRequestQueryService.PageView list(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) @Pattern(regexp="PENDING|APPROVED|REJECTED|CANCELLED") String status){var employeeId=actors.require().employeeId();return status==null?queries.list(employeeId,page,size):queries.list(employeeId,page,size,status);}
    @GetMapping("/{requestId}") public EmployeeLeaveRequestQueryService.Detail detail(@PathVariable UUID requestId){return queries.detail(actors.require().employeeId(),requestId);}
    private static CalculationResponse calculation(LeaveCalculationService.CalculationResult result){var c=result.calculation();return new CalculationResponse(c.chargeableUnits().days(),c.chargeableDates(),c.excludedDates().stream().map(e->new ExcludedDate(e.date(),e.reason().name())).toList(),result.tracksBalance(),result.availableDays(),result.canSubmit(),result.canSubmit()?List.of():List.of("Insufficient unreserved balance"));}
    public record RequestInput(@NotNull UUID leaveTypeId,@NotNull LocalDate startDate,@NotNull LocalDate endDate,@NotNull DurationMode durationMode,@NotBlank @Size(max=1000) String reason){LeaveCalculationService.Input toDomain(){return new LeaveCalculationService.Input(leaveTypeId,startDate,endDate,durationMode,reason);}}
    public record ExcludedDate(LocalDate date,String reason){}
    public record CalculationResponse(double chargeableDays,List<LocalDate> chargeableDates,List<ExcludedDate> excludedDates,boolean tracksBalance,Double availableDays,boolean canSubmit,List<String> messages){}
}
