package com.example.leavemanagement.request.persistence;
import org.springframework.data.repository.Repository;
import java.util.UUID;
public interface LeaveRequestStatusHistoryRepository extends Repository<LeaveRequestStatusHistoryEntity, UUID> { LeaveRequestStatusHistoryEntity save(LeaveRequestStatusHistoryEntity history); java.util.List<LeaveRequestStatusHistoryEntity> findAllByRequestIdOrderByCreatedAtAsc(UUID requestId); }

