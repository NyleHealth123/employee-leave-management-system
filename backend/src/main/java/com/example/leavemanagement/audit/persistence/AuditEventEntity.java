package com.example.leavemanagement.audit.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="audit_event")
public class AuditEventEntity {
    @Id private UUID id;
    @Column(name="actor_user_id") private UUID actorUserId;
    private String action;
    @Column(name="entity_type") private String entityType;
    @Column(name="entity_id") private UUID entityId;
    @Column(name="occurred_at") private Instant occurredAt;
    private String reason;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="before_data",columnDefinition="jsonb") private String beforeData;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="after_data",columnDefinition="jsonb") private String afterData;
    @Column(name="request_correlation_id") private String requestCorrelationId;
    protected AuditEventEntity() {}
    public static AuditEventEntity administration(UUID actorId,String action,String entityType,UUID entityId,String reason,String before,String after){var e=new AuditEventEntity();e.id=UUID.randomUUID();e.actorUserId=actorId;e.action=action;e.entityType=entityType;e.entityId=entityId;e.occurredAt=Instant.now();e.reason=reason;e.beforeData=before;e.afterData=after;return e;}
    public static AuditEventEntity submitted(UUID actorId,UUID requestId){var e=new AuditEventEntity();e.id=UUID.randomUUID();e.actorUserId=actorId;e.action="LEAVE_SUBMITTED";e.entityType="LEAVE_REQUEST";e.entityId=requestId;e.occurredAt=Instant.now();e.afterData="{\"status\":\"PENDING\"}";return e;}
    public static AuditEventEntity decision(UUID actorId,UUID requestId,String action,String from,String to,String reason){var e=new AuditEventEntity();e.id=UUID.randomUUID();e.actorUserId=actorId;e.action=action;e.entityType="LEAVE_REQUEST";e.entityId=requestId;e.occurredAt=Instant.now();e.reason=reason;e.beforeData="{\"status\":\""+from+"\"}";e.afterData="{\"status\":\""+to+"\"}";return e;}
}
