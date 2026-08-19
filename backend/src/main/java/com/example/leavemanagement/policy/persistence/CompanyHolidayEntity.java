package com.example.leavemanagement.policy.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="company_holiday")
public class CompanyHolidayEntity {
    @Id private UUID id;
    @Column(name="holiday_date") private LocalDate date;
    private String name;
    private boolean active;
    @Version private long version;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="updated_at") private Instant updatedAt;
    protected CompanyHolidayEntity() {}
    public UUID getId(){return id;} public LocalDate getDate(){return date;} public String getName(){return name;} public boolean isActive(){return active;} public long getVersion(){return version;}
}
