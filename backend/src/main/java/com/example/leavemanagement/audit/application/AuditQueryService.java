package com.example.leavemanagement.audit.application;

import com.example.leavemanagement.audit.persistence.AuditEventEntity;
import com.example.leavemanagement.audit.persistence.AuditEventRepository;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditQueryService {
    private final AuditEventRepository events;
    private final CurrentActorProvider actors;
    private final ObjectMapper mapper;
    public AuditQueryService(AuditEventRepository events, CurrentActorProvider actors, ObjectMapper mapper) { this.events = events; this.actors = actors; this.mapper = mapper; }

    public PageView list(int page, int size, String entityType, UUID entityId) {
        if (!actors.require().hasRole("ADMINISTRATOR")) throw new DomainException(403, "ACCESS_DENIED", "Administrator role is required");
        if (page < 0 || size < 1 || size > 100) throw new DomainException(400, "VALIDATION_FAILED", "Page must be non-negative and size must be between 1 and 100");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        Page<AuditEventEntity> result = entityType == null && entityId == null ? events.findAll(pageable) : events.search(entityType, entityId, pageable);
        return new PageView(result.map(this::view).getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
    private View view(AuditEventEntity e) { return new View(e.getId(), e.getActorUserId(), e.getAction(), e.getEntityType(), e.getEntityId(), e.getOccurredAt(), e.getReason(), json(e.getBeforeData()), json(e.getAfterData()), e.getRequestCorrelationId()); }
    private java.util.Map<String,Object> json(String value) { if (value == null || value.isBlank()) return null; try { return sanitize(mapper.readValue(value, java.util.Map.class)); } catch (RuntimeException ex) { return java.util.Map.of(); } }
    private java.util.Map<String,Object> sanitize(java.util.Map<?,?> input) { var output = new java.util.LinkedHashMap<String,Object>(); input.forEach((key, value) -> { var name = String.valueOf(key).toLowerCase(java.util.Locale.ROOT); if (name.contains("password") || name.contains("session") || name.contains("secret") || name.contains("credential") || name.contains("token")) return; output.put(String.valueOf(key), value instanceof java.util.Map<?,?> nested ? sanitize(nested) : value); }); return output; }
    public record View(UUID id, UUID actorUserId, String action, String entityType, UUID entityId, java.time.Instant occurredAt, String reason, java.util.Map<String,Object> beforeData, java.util.Map<String,Object> afterData, String correlationId) {}
    public record PageView(java.util.List<View> content, int page, int size, long totalElements, int totalPages) {}
}
