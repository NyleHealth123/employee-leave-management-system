package com.example.leavemanagement.request.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="leave_request_status_history")
public class LeaveRequestStatusHistoryEntity {
    @Id private UUID id;
    @Column(name="request_id") private UUID requestId;
    @Column(name="from_status") private String fromStatus;
    @Column(name="to_status") private String toStatus;
    @Column(name="actor_user_id") private UUID actorUserId;
    private String comment;
    @Column(name="created_at") private Instant createdAt;
    protected LeaveRequestStatusHistoryEntity() {}
    public static LeaveRequestStatusHistoryEntity submitted(UUID requestId,UUID actorId){var e=new LeaveRequestStatusHistoryEntity();e.id=UUID.randomUUID();e.requestId=requestId;e.toStatus="PENDING";e.actorUserId=actorId;e.createdAt=Instant.now();return e;}
    public static LeaveRequestStatusHistoryEntity decision(UUID requestId,String from,String to,UUID actorId,String comment){var e=new LeaveRequestStatusHistoryEntity();e.id=UUID.randomUUID();e.requestId=requestId;e.fromStatus=from;e.toStatus=to;e.actorUserId=actorId;e.comment=comment;e.createdAt=Instant.now();return e;}
    public UUID getActorUserId(){return actorUserId;} public UUID getRequestId(){return requestId;}
    public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;} public String getComment(){return comment;} public Instant getCreatedAt(){return createdAt;}
}
