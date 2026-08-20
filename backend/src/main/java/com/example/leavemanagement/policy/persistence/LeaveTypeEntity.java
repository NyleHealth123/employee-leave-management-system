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
    public static LeaveTypeEntity create(UUID id,String code,String name,String description,boolean active){var e=new LeaveTypeEntity();e.id=id;e.code=code;e.name=name;e.description=description;e.active=active;return e;}
    public void update(String code,String name,String description,boolean active){this.code=code;this.name=name;this.description=description;this.active=active;}
    public long getVersion(){return version;}
    public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public boolean isActive(){return active;}
}
