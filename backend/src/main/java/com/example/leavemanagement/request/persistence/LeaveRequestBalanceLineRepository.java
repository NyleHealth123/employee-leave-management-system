package com.example.leavemanagement.request.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface LeaveRequestBalanceLineRepository extends JpaRepository<LeaveRequestBalanceLineEntity, UUID> { java.util.List<LeaveRequestBalanceLineEntity> findAllByRequestId(UUID requestId); }
