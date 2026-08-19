package com.example.leavemanagement.request.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="leave_request")
public class LeaveRequestEntity {
    @Id private UUID id;
    @Column(name="employee_id") private UUID employeeId;
    @Column(name="leave_type_id") private UUID leaveTypeId;
    @Column(name="submitted_policy_version_id") private UUID submittedPolicyVersionId;
    @Column(name="start_date") private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    @Column(name="duration_mode") private String durationMode;
    @Column(name="chargeable_units") private int chargeableUnits;
    private String reason;
    private String status;
    @Column(name="submitted_at") private Instant submittedAt;
    @Column(name="decided_at") private Instant decidedAt;
    @Column(name="decided_by_user_id") private UUID decidedByUserId;
    @Column(name="decision_comment") private String decisionComment;
    @Column(name="cancelled_at") private Instant cancelledAt;
    @Column(name="cancelled_by_user_id") private UUID cancelledByUserId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="policy_snapshot",columnDefinition="jsonb") private String policySnapshot;
    @Column(name="idempotency_key") private String idempotencyKey;
    @Version private long version;
    protected LeaveRequestEntity() {}
    public static LeaveRequestEntity pending(UUID employeeId,UUID leaveTypeId,UUID policyId,LocalDate start,LocalDate end,String mode,int units,String reason,String policySnapshot,String key){var e=new LeaveRequestEntity();e.id=UUID.randomUUID();e.employeeId=employeeId;e.leaveTypeId=leaveTypeId;e.submittedPolicyVersionId=policyId;e.startDate=start;e.endDate=end;e.durationMode=mode;e.chargeableUnits=units;e.reason=reason;e.status="PENDING";e.submittedAt=Instant.now();e.policySnapshot=policySnapshot;e.idempotencyKey=key;return e;}
    public UUID getId(){return id;} public UUID getEmployeeId(){return employeeId;} public UUID getLeaveTypeId(){return leaveTypeId;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public String getDurationMode(){return durationMode;} public int getChargeableUnits(){return chargeableUnits;} public String getReason(){return reason;} public String getStatus(){return status;} public Instant getSubmittedAt(){return submittedAt;} public String getDecisionComment(){return decisionComment;} public long getVersion(){return version;}
}
