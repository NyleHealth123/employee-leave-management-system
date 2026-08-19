package com.example.leavemanagement.shared.security;
import java.util.Set;
import java.util.UUID;
public record CurrentActor(UUID userId,UUID employeeId,UUID managerId,Set<String> roles,String displayName) { public boolean hasRole(String role){return roles.contains(role);} }

