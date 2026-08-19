package com.example.leavemanagement.policy.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="leave_type")
public class LeaveTypeEntity {
    @Id private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean active;
    @Version private long version;
    protected LeaveTypeEntity() {}
    public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public boolean isActive(){return active;}
}

