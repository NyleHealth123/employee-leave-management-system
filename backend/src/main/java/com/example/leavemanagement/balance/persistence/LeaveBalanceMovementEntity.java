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
    public static LeaveBalanceMovementEntity administration(UUID balanceId,int units,String type,String reason,UUID actorId,String idempotency){var e=new LeaveBalanceMovementEntity();e.id=UUID.randomUUID();e.balanceId=balanceId;e.movementType=type;e.units=units;e.reason=reason;e.actorUserId=actorId;e.idempotencyKey=idempotency;e.createdAt=Instant.now();return e;}
    public static LeaveBalanceMovementEntity reservation(UUID balanceId,UUID requestId,int units,UUID actorId){var e=new LeaveBalanceMovementEntity();e.id=UUID.randomUUID();e.balanceId=balanceId;e.requestId=requestId;e.movementType="RESERVE";e.units=units;e.actorUserId=actorId;e.createdAt=Instant.now();return e;}
    public static LeaveBalanceMovementEntity decision(UUID balanceId,UUID requestId,int units,UUID actorId,String type){var e=new LeaveBalanceMovementEntity();e.id=UUID.randomUUID();e.balanceId=balanceId;e.requestId=requestId;e.movementType=type;e.units=type.equals("RELEASE_RESERVED")?-units:units;e.actorUserId=actorId;e.createdAt=Instant.now();return e;}
    public static LeaveBalanceMovementEntity correction(UUID balanceId,UUID requestId,int units,UUID actorId,String type,String reason){var e=decision(balanceId,requestId,units,actorId,type);if(type.equals("RESTORE_CONSUMED")||type.equals("RELEASE_RESERVED"))e.units=-units;e.reason=reason;return e;}
}
