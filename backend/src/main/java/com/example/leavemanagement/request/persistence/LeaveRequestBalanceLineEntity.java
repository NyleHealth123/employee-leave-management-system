package com.example.leavemanagement.request.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="leave_request_balance_line")
public class LeaveRequestBalanceLineEntity {
    @Id private UUID id;
    @Column(name="request_id") private UUID requestId;
    @Column(name="balance_id") private UUID balanceId;
    private int units;
    private String state;
    @Column(name="updated_at") private Instant updatedAt;
    @Version private long version;
    protected LeaveRequestBalanceLineEntity() {}
    public static LeaveRequestBalanceLineEntity reserved(UUID requestId,UUID balanceId,int units){var e=new LeaveRequestBalanceLineEntity();e.id=UUID.randomUUID();e.requestId=requestId;e.balanceId=balanceId;e.units=units;e.state="RESERVED";e.updatedAt=Instant.now();return e;}
}

