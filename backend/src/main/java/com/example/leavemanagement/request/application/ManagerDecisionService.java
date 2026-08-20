package com.example.leavemanagement.request.application;

import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ManagerDecisionService {
    private final LeaveRequestRepository requests; private final LeaveRequestBalanceLineRepository lines; private final LeaveBalanceRepository balances; private final LeaveBalanceMovementRepository movements; private final LeaveRequestSlotRepository slots; private final LeaveRequestStatusHistoryRepository history; private final AuditEventRepository audits; private final LeavePolicyQueryService policies; private final ManagerLeaveRequestQueryService queries;
    public ManagerDecisionService(LeaveRequestRepository requests,LeaveRequestBalanceLineRepository lines,LeaveBalanceRepository balances,LeaveBalanceMovementRepository movements,LeaveRequestSlotRepository slots,LeaveRequestStatusHistoryRepository history,AuditEventRepository audits,LeavePolicyQueryService policies,ManagerLeaveRequestQueryService queries){this.requests=requests;this.lines=lines;this.balances=balances;this.movements=movements;this.slots=slots;this.history=history;this.audits=audits;this.policies=policies;this.queries=queries;}
    @Transactional public LeaveRequestEntity decide(CurrentActor actor,UUID id,long expectedVersion,String comment,boolean approve){if(!actor.hasRole("MANAGER"))throw new DomainException(403,"ACCESS_DENIED","Manager access is required");var r=requests.lockDirectReport(id,actor.employeeId()).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found"));if(r.getEmployeeId().equals(actor.employeeId()))throw new DomainException(403,"ACCESS_DENIED","Managers cannot decide their own leave");if(r.getVersion()!=expectedVersion)throw new DomainException(409,"STALE_VERSION","The leave request changed; refresh and retry");if(!"PENDING".equals(r.getStatus()))throw new DomainException(409,"INVALID_STATUS_TRANSITION","Only pending requests can be decided");var policy=policies.effective(r.getLeaveTypeId(),r.getStartDate());if(!policy.getId().equals(r.getSubmittedPolicyVersionId()))throw new DomainException(409,"POLICY_CHANGED","The leave policy changed; the request must be resubmitted");if(!approve&&policy.isRejectionCommentRequired()&&(comment==null||comment.isBlank()))throw new DomainException(422,"REJECTION_COMMENT_REQUIRED","A rejection comment is required");try{for(var line:lines.findAllByRequestId(r.getId())){var b=balances.findById(line.getBalanceId()).orElseThrow();if("RESERVED".equals(line.getState())){if(approve){b.consumeReserved(line.getUnits());line.consume();movements.save(LeaveBalanceMovementEntity.decision(b.getId(),r.getId(),line.getUnits(),actor.userId(),"CONSUME_RESERVED"));}else{b.releaseReserved(line.getUnits());line.release();movements.save(LeaveBalanceMovementEntity.decision(b.getId(),r.getId(),line.getUnits(),actor.userId(),"RELEASE_RESERVED"));}}}
        if(!approve)slots.findAllByRequestId(r.getId()).forEach(LeaveRequestSlotEntity::deactivate);var old=r.getStatus();if(approve)r.approve(actor.userId(),blank(comment));else r.reject(actor.userId(),blank(comment));requests.saveAndFlush(r);lines.saveAll(lines.findAllByRequestId(r.getId()));history.save(LeaveRequestStatusHistoryEntity.decision(r.getId(),old,r.getStatus(),actor.userId(),blank(comment)));audits.save(AuditEventEntity.decision(actor.userId(),r.getId(),approve?"LEAVE_APPROVED":"LEAVE_REJECTED",old,r.getStatus(),blank(comment)));return r;}catch(OptimisticLockingFailureException ex){throw new DomainException(409,"STALE_VERSION","The leave request changed; refresh and retry");}}
    private static String blank(String s){return s==null||s.isBlank()?null:s;}
}
