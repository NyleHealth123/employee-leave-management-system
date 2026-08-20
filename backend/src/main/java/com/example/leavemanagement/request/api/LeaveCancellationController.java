package com.example.leavemanagement.request.api;
import com.example.leavemanagement.request.application.*;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/employee/leave-requests")
public class LeaveCancellationController {
    private final CancelLeaveRequestService cancellations; private final EmployeeLeaveRequestQueryService queries; private final CurrentActorProvider actors;
    public LeaveCancellationController(CancelLeaveRequestService c,EmployeeLeaveRequestQueryService q,CurrentActorProvider a){cancellations=c;queries=q;actors=a;}
    @PostMapping("/{id}/cancel") public EmployeeLeaveRequestQueryService.Detail cancel(@PathVariable UUID id,@Valid @RequestBody CommentCommand command){var a=actors.require();return queries.detail(cancellations.cancel(a,id,command.expectedVersion(),command.comment()));}
    public record CommentCommand(@NotNull @Min(0) Long expectedVersion,@Size(max=1000) String comment){}
}
