package com.example.leavemanagement.auth.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "user_account")
public class UserAccountEntity {
    @Id private UUID id;
    private String login;
    @Column(name="normalized_login") private String normalizedLogin;
    @Column(name="password_hash") private String passwordHash;
    private boolean enabled;
    @Column(name="credentials_updated_at") private Instant credentialsUpdatedAt;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="updated_at") private Instant updatedAt;
    @Version private long version;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="user_account_role", joinColumns=@JoinColumn(name="account_id"))
    @Column(name="role_code")
    private Set<String> roles = new LinkedHashSet<>();

    protected UserAccountEntity() {}
    public static UserAccountEntity create(UUID id,String login,String normalizedLogin,String passwordHash,Set<String> roles,Instant now){var e=new UserAccountEntity();e.id=id;e.login=login;e.normalizedLogin=normalizedLogin;e.passwordHash=passwordHash;e.roles=new LinkedHashSet<>(roles);e.enabled=true;e.credentialsUpdatedAt=now;e.createdAt=now;e.updatedAt=now;return e;}
    public void update(String login,String normalizedLogin,Set<String> roles,boolean enabled,Instant now){this.login=login;this.normalizedLogin=normalizedLogin;this.roles=new LinkedHashSet<>(roles);this.enabled=enabled;this.updatedAt=now;}
    public long getVersion(){return version;}
    public UUID getId(){return id;} public String getLogin(){return login;} public String getNormalizedLogin(){return normalizedLogin;}
    public String getPasswordHash(){return passwordHash;} public boolean isEnabled(){return enabled;} public Set<String> getRoles(){return Set.copyOf(roles);}
}
