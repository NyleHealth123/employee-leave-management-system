package com.example.leavemanagement.shared.security;

import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CurrentActorProvider {
    private final EmployeeProfileRepository employees;
    public CurrentActorProvider(EmployeeProfileRepository employees){this.employees=employees;}
    public CurrentActor require(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated()||"anonymousUser".equals(auth.getPrincipal()))throw new DomainException(401,"AUTHENTICATION_REQUIRED","Authentication is required");var userId=UUID.fromString(auth.getName());var employee=employees.findByUserAccountId(userId).orElseThrow(()->new DomainException(403,"ACCESS_DENIED","No active employee profile is available"));if(!employee.isActive())throw new DomainException(403,"ACCESS_DENIED","Employee profile is inactive");Set<String> roles=auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).map(a->a.substring("ROLE_".length())).collect(Collectors.toUnmodifiableSet());return new CurrentActor(userId,employee.getId(),employee.getManager()==null?null:employee.getManager().getId(),roles,employee.getDisplayName());}
}
