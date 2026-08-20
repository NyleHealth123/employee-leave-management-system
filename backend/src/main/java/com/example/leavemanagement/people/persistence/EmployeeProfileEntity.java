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
    public static EmployeeProfileEntity create(UUID id,String employeeNumber,UserAccountEntity account,String displayName,String email,EmployeeProfileEntity manager,boolean active,Instant now){var e=new EmployeeProfileEntity();e.id=id;e.employeeNumber=employeeNumber;e.userAccount=account;e.displayName=displayName;e.email=email;e.manager=manager;e.active=active;e.createdAt=now;e.updatedAt=now;return e;}
    public void update(String employeeNumber,String displayName,String email,EmployeeProfileEntity manager,boolean active,Instant now){this.employeeNumber=employeeNumber;this.displayName=displayName;this.email=email;this.manager=manager;this.active=active;this.updatedAt=now;}
    public String getEmployeeNumber(){return employeeNumber;} public String getEmail(){return email;} public long getVersion(){return version;}
    public UUID getId(){return id;} public String getDisplayName(){return displayName;} public EmployeeProfileEntity getManager(){return manager;}
    public UserAccountEntity getUserAccount(){return userAccount;} public boolean isActive(){return active;}
}
