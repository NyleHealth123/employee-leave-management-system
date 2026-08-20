package com.example.leavemanagement.request.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="leave_request_slot")
public class LeaveRequestSlotEntity {
    @Id private UUID id;
    @Column(name="request_id") private UUID requestId;
    @Column(name="employee_id") private UUID employeeId;
    @Column(name="leave_date") private LocalDate leaveDate;
    private String slot;
    private boolean active;
    protected LeaveRequestSlotEntity() {}
    public static LeaveRequestSlotEntity active(UUID requestId,UUID employeeId,LocalDate date,String slot){var e=new LeaveRequestSlotEntity();e.id=UUID.randomUUID();e.requestId=requestId;e.employeeId=employeeId;e.leaveDate=date;e.slot=slot;e.active=true;return e;}
    public LocalDate getLeaveDate(){return leaveDate;} public String getSlot(){return slot;} public boolean isActive(){return active;} public void deactivate(){active=false;}
}
