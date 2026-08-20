package com.example.leavemanagement.people.application;

import com.example.leavemanagement.auth.persistence.*;
import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.people.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActorProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service @Transactional
public class EmployeeAdministrationService {
 private static final Set<String> ROLES=Set.of("EMPLOYEE","MANAGER","ADMINISTRATOR");
 private final EmployeeProfileRepository employees; private final UserAccountRepository accounts; private final AuditEventRepository audits; private final PasswordEncoder encoder; private final CurrentActorProvider actors;
 public EmployeeAdministrationService(EmployeeProfileRepository e,UserAccountRepository a,AuditEventRepository au,PasswordEncoder p,CurrentActorProvider c){employees=e;accounts=a;audits=au;encoder=p;actors=c;}
 private void admin(){if(!actors.require().hasRole("ADMINISTRATOR"))throw new DomainException(403,"ACCESS_DENIED","Administrator role is required");}
 private Set<String> roles(Collection<String> input){if(input==null||input.isEmpty()||!ROLES.containsAll(input)||input.size()!=new HashSet<>(input).size())throw new DomainException(400,"VALIDATION_FAILED","roles must be a non-empty unique subset of the closed role set");return Set.copyOf(input);}
 private EmployeeProfileEntity manager(UUID id){if(id==null)return null;var m=employees.findById(id).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Manager was not found"));if(!m.isActive()||!m.getUserAccount().getRoles().contains("MANAGER"))throw new DomainException(400,"INVALID_MANAGER","Manager must be active and have MANAGER role");return m;}
 @Transactional public View create(CreateCommand c){admin();var rs=roles(c.roles());if(c.initialPassword()==null||c.initialPassword().length()<12)throw new DomainException(400,"VALIDATION_FAILED","initialPassword is required only for creation and must be at least 12 characters");var norm=c.login().strip().toLowerCase(Locale.ROOT);if(accounts.findByNormalizedLogin(norm).isPresent()||employees.existsByEmployeeNumber(c.employeeNumber())||employees.existsByEmailIgnoreCase(c.email()))throw new DomainException(409,"RESOURCE_CONFLICT","Employee identity or login already exists");var now=Instant.now();var a=UserAccountEntity.create(UUID.randomUUID(),c.login(),norm,encoder.encode(c.initialPassword()),rs,now);var e=EmployeeProfileEntity.create(UUID.randomUUID(),c.employeeNumber(),a,c.displayName(),c.email(),manager(c.managerId()),c.active(),now);accounts.save(a);employees.save(e);audits.save(AuditEventEntity.administration(actors.require().userId(),"EMPLOYEE_CREATED","EMPLOYEE_PROFILE",e.getId(),null,null,"{\"active\":"+e.isActive()+"}"));return view(e);}
 @Transactional public View update(UUID id,UpdateCommand c){admin();var e=employees.findLockedById(id).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Employee was not found"));if(e.getVersion()!=c.expectedVersion())throw new DomainException(409,"STALE_VERSION","The employee was changed; reload and retry");if(id.equals(c.managerId()))throw new DomainException(400,"INVALID_MANAGER","An employee cannot manage itself");var rs=roles(c.roles());var norm=c.login().strip().toLowerCase(Locale.ROOT);accounts.findByNormalizedLogin(norm).filter(a->!a.getId().equals(e.getUserAccount().getId())).ifPresent(a->{throw new DomainException(409,"RESOURCE_CONFLICT","Login already exists");});var m=manager(c.managerId());var before="{\"active\":"+e.isActive()+"}";e.update(c.employeeNumber(),c.displayName(),c.email(),m,c.active(),Instant.now());e.getUserAccount().update(c.login(),norm,rs,c.active(),Instant.now());audits.save(AuditEventEntity.administration(actors.require().userId(),c.active()?"EMPLOYEE_UPDATED":"EMPLOYEE_DEACTIVATED","EMPLOYEE_PROFILE",e.getId(),null,before,"{\"active\":"+c.active()+"}"));return view(e);}
 public View view(EmployeeProfileEntity e){var m=e.getManager();return new View(e.getId(),e.getEmployeeNumber(),e.getDisplayName(),e.getEmail(),m==null?null:m.getId(),m==null?null:m.getDisplayName(),e.getUserAccount().getRoles(),e.isActive(),e.getVersion());}
 public record CreateCommand(String employeeNumber,String displayName,String email,String login,String initialPassword,UUID managerId,Collection<String> roles,boolean active){}
 public record UpdateCommand(String employeeNumber,String displayName,String email,String login,UUID managerId,Collection<String> roles,boolean active,long expectedVersion){}
 public record View(UUID id,String employeeNumber,String displayName,String email,UUID managerId,String managerName,Set<String> roles,boolean active,long version){}
}
