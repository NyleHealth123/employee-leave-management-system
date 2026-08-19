package com.example.leavemanagement.policy.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name="leave_policy_version")
public class LeavePolicyVersionEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="leave_type_id") private LeaveTypeEntity leaveType;
    @Column(name="version_number") private int versionNumber;
    @Column(name="effective_from") private LocalDate effectiveFrom;
    @Column(name="effective_to") private LocalDate effectiveTo;
    @Column(name="tracks_balance") private boolean tracksBalance;
    @Column(name="allows_half_day") private boolean allowsHalfDay;
    @Column(name="weekly_off_treatment") private String weeklyOffTreatment;
    @Column(name="holiday_treatment") private String holidayTreatment;
    @Column(name="rejection_comment_required") private boolean rejectionCommentRequired;
    @Column(name="cancellation_cutoff_days") private int cancellationCutoffDays;
    @Column(name="created_at") private Instant createdAt;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="policy_weekly_off", joinColumns=@JoinColumn(name="policy_version_id"))
    @Column(name="iso_day") private Set<Integer> weeklyOffDays = new LinkedHashSet<>();
    protected LeavePolicyVersionEntity() {}
    public UUID getId(){return id;} public LeaveTypeEntity getLeaveType(){return leaveType;} public boolean isTracksBalance(){return tracksBalance;}
    public boolean isAllowsHalfDay(){return allowsHalfDay;} public boolean excludesWeeklyOffs(){return "EXCLUDE".equals(weeklyOffTreatment);}
    public boolean excludesHolidays(){return "EXCLUDE".equals(holidayTreatment);} public Set<Integer> getWeeklyOffDays(){return Set.copyOf(weeklyOffDays);} public int getCancellationCutoffDays(){return cancellationCutoffDays;}
    public String getWeeklyOffTreatment(){return weeklyOffTreatment;} public String getHolidayTreatment(){return holidayTreatment;} public boolean isRejectionCommentRequired(){return rejectionCommentRequired;}
}
