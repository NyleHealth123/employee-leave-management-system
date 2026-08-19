package com.example.leavemanagement.balance.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="leave_balance_movement")
public class LeaveBalanceMovementEntity {
    @Id private UUID id;
    @Column(name="balance_id") private UUID balanceId;
    @Column(name="request_id") private UUID requestId;
    @Column(name="movement_type") private String movementType;
    private int units;
    private String reason;
    @Column(name="actor_user_id") private UUID actorUserId;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="idempotency_key") private String idempotencyKey;
    protected LeaveBalanceMovementEntity() {}
    public static LeaveBalanceMovementEntity reservation(UUID balanceId,UUID requestId,int units,UUID actorId){var e=new LeaveBalanceMovementEntity();e.id=UUID.randomUUID();e.balanceId=balanceId;e.requestId=requestId;e.movementType="RESERVE";e.units=units;e.actorUserId=actorId;e.createdAt=Instant.now();return e;}
}

