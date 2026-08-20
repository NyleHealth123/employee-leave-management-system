package com.example.leavemanagement.audit.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
    @Query("select e from AuditEventEntity e where (:entityType is null or e.entityType=:entityType) and (:entityId is null or e.entityId=:entityId)")
    Page<AuditEventEntity> search(@Param("entityType") String entityType, @Param("entityId") UUID entityId, Pageable pageable);
}
