package com.example.leavemanagement.audit.persistence;
import org.springframework.data.repository.Repository;
import java.util.UUID;
public interface AuditEventRepository extends Repository<AuditEventEntity, UUID> { AuditEventEntity save(AuditEventEntity event); }

