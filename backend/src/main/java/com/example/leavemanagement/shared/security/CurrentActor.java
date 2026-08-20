package com.example.leavemanagement.shared.security;
import java.util.Set;
import java.util.UUID;
public record CurrentActor(UUID userId,UUID employeeId,UUID managerId,Set<String> roles,String displayName) {
    private static final Set<String> ALLOWED_ROLES=Set.of("EMPLOYEE","MANAGER","ADMINISTRATOR");
    public CurrentActor {
        roles=Set.copyOf(roles);
        if(roles.isEmpty()||!ALLOWED_ROLES.containsAll(roles))throw new IllegalArgumentException("principal roles must be a non-empty subset of the closed role set");
    }
    public boolean hasRole(String role){return roles.contains(role);}
}
