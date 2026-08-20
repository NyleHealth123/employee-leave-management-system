package com.example.leavemanagement.balance.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface LeaveBalanceMovementRepository extends JpaRepository<LeaveBalanceMovementEntity, UUID> { boolean existsByActorUserIdAndIdempotencyKey(UUID actorUserId,String idempotencyKey); }
