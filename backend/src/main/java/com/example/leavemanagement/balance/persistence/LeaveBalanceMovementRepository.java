package com.example.leavemanagement.balance.persistence;
import org.springframework.data.repository.Repository;
import java.util.UUID;
public interface LeaveBalanceMovementRepository extends Repository<LeaveBalanceMovementEntity, UUID> { LeaveBalanceMovementEntity save(LeaveBalanceMovementEntity movement); }

