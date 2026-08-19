package com.example.leavemanagement.policy.application;

import com.example.leavemanagement.policy.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service @Transactional(readOnly=true)
public class LeavePolicyQueryService {
    private final LeaveTypeRepository types;private final LeavePolicyVersionRepository policies;
    public LeavePolicyQueryService(LeaveTypeRepository types,LeavePolicyVersionRepository policies){this.types=types;this.policies=policies;}
    public List<LeaveTypeSummary> activeTypes(LocalDate on){return types.findAllByActiveTrueOrderByNameAsc().stream().map(t->{var p=effective(t.getId(),on);return new LeaveTypeSummary(t.getId(),t.getCode(),t.getName(),p.isTracksBalance(),p.isAllowsHalfDay(),p.getCancellationCutoffDays());}).toList();}
    public LeavePolicyVersionEntity effective(UUID typeId,LocalDate on){var type=types.findById(typeId).filter(LeaveTypeEntity::isActive).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave type is unavailable"));return policies.findEffective(type.getId(),on).orElseThrow(()->new DomainException(409,"POLICY_CHANGED","No policy applies to the requested date"));}
    public String typeName(UUID typeId){return types.findById(typeId).map(LeaveTypeEntity::getName).orElse("Leave");}
    public record LeaveTypeSummary(UUID id,String code,String name,boolean tracksBalance,boolean allowsHalfDay,int cancellationCutoffDays){}
}

