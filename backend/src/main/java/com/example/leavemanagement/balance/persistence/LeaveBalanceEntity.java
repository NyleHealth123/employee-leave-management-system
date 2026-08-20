package com.example.leavemanagement.balance.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="leave_balance")
public class LeaveBalanceEntity {
    @Id private UUID id;
    @Column(name="employee_id") private UUID employeeId;
    @Column(name="leave_type_id") private UUID leaveTypeId;
    @Column(name="period_start") private LocalDate periodStart;
    @Column(name="period_end") private LocalDate periodEnd;
    @Column(name="allocated_units") private int allocatedUnits;
    @Column(name="adjustment_units") private int adjustmentUnits;
    @Column(name="reserved_units") private int reservedUnits;
    @Column(name="consumed_units") private int consumedUnits;
    @Version private long version;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="updated_at") private Instant updatedAt;
    protected LeaveBalanceEntity() {}
    public UUID getId(){return id;} public UUID getEmployeeId(){return employeeId;} public UUID getLeaveTypeId(){return leaveTypeId;}
    public LocalDate getPeriodStart(){return periodStart;} public LocalDate getPeriodEnd(){return periodEnd;} public int getAllocatedUnits(){return allocatedUnits;}
    public int getReservedUnits(){return reservedUnits;} public int getConsumedUnits(){return consumedUnits;} public int getAvailableUnits(){return allocatedUnits+adjustmentUnits-reservedUnits-consumedUnits;}
    public long getVersion(){return version;}
    public void reserve(int units){if(units<=0||getAvailableUnits()<units)throw new IllegalStateException("insufficient balance");reservedUnits+=units;updatedAt=Instant.now();}
    public void consumeReserved(int units){if(units<=0||reservedUnits<units)throw new IllegalStateException("invalid reservation");reservedUnits-=units;consumedUnits+=units;updatedAt=Instant.now();}
    public void releaseReserved(int units){if(units<=0||reservedUnits<units)throw new IllegalStateException("invalid reservation");reservedUnits-=units;updatedAt=Instant.now();}
}
