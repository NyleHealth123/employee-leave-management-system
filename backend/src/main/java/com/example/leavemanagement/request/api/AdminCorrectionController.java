package com.example.leavemanagement.request.api;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/admin/leave-requests")
public class AdminCorrectionController {
    private final ExceptionalCorrectionService corrections; private final EmployeeLeaveRequestQueryService queries; private final CurrentActorProvider actors;
    public AdminCorrectionController(ExceptionalCorrectionService c,EmployeeLeaveRequestQueryService q,CurrentActorProvider a){corrections=c;queries=q;actors=a;}
    @PostMapping("/{id}/corrections") public EmployeeLeaveRequestQueryService.Detail correct(@PathVariable UUID id,@Valid @RequestBody CorrectionCommand command){var a=actors.require();return queries.detail(corrections.correct(a,id,command.action(),command.reason(),command.expectedVersion()));}
    public record CorrectionCommand(@NotBlank @Pattern(regexp="CANCEL_PENDING|CANCEL_APPROVED|REOPEN_REJECTED") String action,@NotBlank @Size(max=1000) String reason,@NotNull @Min(0) Long expectedVersion){}
}
