package com.example.leavemanagement.people.persistence;

import com.example.leavemanagement.auth.persistence.UserAccountEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="employee_profile")
public class EmployeeProfileEntity {
    @Id private UUID id;
    @Column(name="employee_number") private String employeeNumber;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_account_id") private UserAccountEntity userAccount;
    @Column(name="display_name") private String displayName;
    private String email;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="manager_id") private EmployeeProfileEntity manager;
    private boolean active;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="updated_at") private Instant updatedAt;
    @Version private long version;
    protected EmployeeProfileEntity() {}
    public UUID getId(){return id;} public String getDisplayName(){return displayName;} public EmployeeProfileEntity getManager(){return manager;}
    public UserAccountEntity getUserAccount(){return userAccount;} public boolean isActive(){return active;}
}

