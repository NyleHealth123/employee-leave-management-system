package com.example.leavemanagement.request.application;

import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.domain.CancellationPolicy;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class CancelLeaveRequestService {
    private final LeaveRequestRepository requests; private final LeaveRequestBalanceLineRepository lines;
    private final LeaveBalanceRepository balances; private final LeaveBalanceMovementRepository movements;
    private final LeaveRequestSlotRepository slots; private final LeaveRequestStatusHistoryRepository history;
    private final AuditEventRepository audits; private final LeavePolicyQueryService policies; private final Clock clock;
    public CancelLeaveRequestService(LeaveRequestRepository requests, LeaveRequestBalanceLineRepository lines, LeaveBalanceRepository balances, LeaveBalanceMovementRepository movements, LeaveRequestSlotRepository slots, LeaveRequestStatusHistoryRepository history, AuditEventRepository audits, LeavePolicyQueryService policies, Clock clock){this.requests=requests;this.lines=lines;this.balances=balances;this.movements=movements;this.slots=slots;this.history=history;this.audits=audits;this.policies=policies;this.clock=clock;}
    @Transactional
    public LeaveRequestEntity cancel(CurrentActor actor, UUID id, long expectedVersion, String comment){
        var request=requests.lockOwned(id,actor.employeeId()).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found"));
        if(request.getVersion()!=expectedVersion) throw stale();
        var policy=policies.effective(request.getLeaveTypeId(),request.getStartDate());
        var eligibility=CancellationPolicy.evaluate(request.getEmployeeId().equals(actor.employeeId()),request.getStatus(),
                request.getStartDate(),policy.getCancellationCutoffDays(),Instant.now(clock),clock.getZone());
        if(!eligibility.allowed()) throw new DomainException("APPROVED".equals(request.getStatus())?422:409,"APPROVED".equals(request.getStatus())?"CANCELLATION_CUTOFF_PASSED":"INVALID_STATUS_TRANSITION",eligibility.blockedReason());
        var requestLines=lines.findAllByRequestId(request.getId()).stream().sorted(Comparator.comparing(x->x.getBalanceId().toString())).toList();
        var lockedBalances=new LinkedHashMap<UUID,LeaveBalanceEntity>();
        for(var line:requestLines){
            if(!Set.of("RESERVED","CONSUMED").contains(line.getState())) throw new DomainException(409,"INVALID_STATUS_TRANSITION","The request has no cancellable balance state");
            lockedBalances.put(line.getBalanceId(),balances.findLockedById(line.getBalanceId()).orElseThrow(()->new DomainException(409,"BALANCE_STATE_INVALID","The request balance could not be locked")));
        }
        var old=request.getStatus();
        for(var line:requestLines){var balance=lockedBalances.get(line.getBalanceId());try{
            if("RESERVED".equals(line.getState())){balance.releaseReserved(line.getUnits());line.release();movements.save(LeaveBalanceMovementEntity.correction(balance.getId(),request.getId(),line.getUnits(),actor.userId(),"RELEASE_RESERVED",blank(comment)));}
            else {balance.restoreConsumed(line.getUnits());line.restore();movements.save(LeaveBalanceMovementEntity.correction(balance.getId(),request.getId(),line.getUnits(),actor.userId(),"RESTORE_CONSUMED",blank(comment)));}
        }catch(IllegalStateException ex){throw new DomainException(409,"BALANCE_STATE_INVALID","The request balance could not be restored");}}
        slots.findAllByRequestId(request.getId()).forEach(LeaveRequestSlotEntity::deactivate);
        request.cancel(actor.userId(),blank(comment));
        requests.saveAndFlush(request); lines.saveAll(requestLines);
        history.save(LeaveRequestStatusHistoryEntity.decision(request.getId(),old,"CANCELLED",actor.userId(),blank(comment)));
        audits.save(AuditEventEntity.decision(actor.userId(),request.getId(),"LEAVE_CANCELLED",old,"CANCELLED",blank(comment)));
        return request;
    }
    private static DomainException stale(){return new DomainException(409,"STALE_VERSION","The leave request changed; refresh and retry");}
    private static String blank(String s){return s==null||s.isBlank()?null:s.strip();}
}
