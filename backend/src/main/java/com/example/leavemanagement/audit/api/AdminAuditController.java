package com.example.leavemanagement.audit.api;

import com.example.leavemanagement.audit.application.AuditQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-events")
public class AdminAuditController {
    private final AuditQueryService audits;
    public AdminAuditController(AuditQueryService audits) { this.audits = audits; }
    @GetMapping
    public AuditQueryService.PageView list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String entityType, @RequestParam(required = false) UUID entityId) {
        if (page < 0 || size < 1 || size > 100) throw new com.example.leavemanagement.shared.api.DomainException(400, "VALIDATION_FAILED", "Page must be non-negative and size must be between 1 and 100");
        return audits.list(page, size, entityType, entityId);
    }
}
