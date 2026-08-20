package com.example.leavemanagement.request.application;

import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.request.domain.DurationMode;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ExceptionalCorrectionService {
    private final LeaveRequestRepository requests; private final LeaveRequestBalanceLineRepository lines; private final LeaveBalanceRepository balances; private final LeaveBalanceMovementRepository movements; private final LeaveRequestSlotRepository slots; private final LeaveRequestStatusHistoryRepository history; private final AuditEventRepository audits; private final LeaveCalculationService calculations; private final LeaveBalanceService balanceService;
    public ExceptionalCorrectionService(LeaveRequestRepository requests,LeaveRequestBalanceLineRepository lines,LeaveBalanceRepository balances,LeaveBalanceMovementRepository movements,LeaveRequestSlotRepository slots,LeaveRequestStatusHistoryRepository history,AuditEventRepository audits,LeaveCalculationService calculations,LeaveBalanceService balanceService){this.requests=requests;this.lines=lines;this.balances=balances;this.movements=movements;this.slots=slots;this.history=history;this.audits=audits;this.calculations=calculations;this.balanceService=balanceService;}

    @Transactional
    public LeaveRequestEntity correct(CurrentActor actor,UUID id,String action,String reason,long expectedVersion){
        if(!actor.hasRole("ADMINISTRATOR")) throw new DomainException(403,"ACCESS_DENIED","Administrator role is required");
        if(reason==null||reason.isBlank()) throw new DomainException(400,"VALIDATION_FAILED","Correction reason is required");
        var request=requests.lockById(id).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found"));
        if(request.getVersion()!=expectedVersion) throw new DomainException(409,"STALE_VERSION","The leave request changed; refresh and retry");
        var source=switch(action){case "CANCEL_PENDING"->"PENDING";case "CANCEL_APPROVED"->"APPROVED";case "REOPEN_REJECTED"->"REJECTED";default->null;};
        if(source==null) throw new DomainException(400,"INVALID_CORRECTION_ACTION","Correction action is not permitted");
        if(!source.equals(request.getStatus())) throw new DomainException(409,"INVALID_STATUS_TRANSITION","Correction action does not match the current status");
        var old=request.getStatus();
        if("REOPEN_REJECTED".equals(action)) reopen(request,actor,reason); else cancel(request,actor,reason);
        requests.saveAndFlush(request);
        history.save(LeaveRequestStatusHistoryEntity.decision(request.getId(),old,request.getStatus(),actor.userId(),reason));
        audits.save(AuditEventEntity.decision(actor.userId(),request.getId(),"ADMIN_"+action,old,request.getStatus(),reason));
        return request;
    }

    private void cancel(LeaveRequestEntity request,CurrentActor actor,String reason){
        var requestLines=lines.findAllByRequestId(request.getId()).stream().sorted(Comparator.comparing(x->x.getBalanceId().toString())).toList();
        var locked=new LinkedHashMap<UUID,LeaveBalanceEntity>();
        for(var line:requestLines){if(!Set.of("RESERVED","CONSUMED").contains(line.getState())) throw new DomainException(409,"INVALID_STATUS_TRANSITION","The request has no cancellable balance state");locked.put(line.getBalanceId(),balances.findLockedById(line.getBalanceId()).orElseThrow(()->new DomainException(409,"BALANCE_STATE_INVALID","The request balance could not be locked")));}
        for(var line:requestLines){var balance=locked.get(line.getBalanceId());try{if("RESERVED".equals(line.getState())){balance.releaseReserved(line.getUnits());line.release();movements.save(LeaveBalanceMovementEntity.correction(balance.getId(),request.getId(),line.getUnits(),actor.userId(),"RELEASE_RESERVED",reason));}else{balance.restoreConsumed(line.getUnits());line.restore();movements.save(LeaveBalanceMovementEntity.correction(balance.getId(),request.getId(),line.getUnits(),actor.userId(),"RESTORE_CONSUMED",reason));}}catch(IllegalStateException ex){throw new DomainException(409,"BALANCE_STATE_INVALID","The request balance could not be restored");}}
        slots.findAllByRequestId(request.getId()).forEach(LeaveRequestSlotEntity::deactivate); request.cancel(actor.userId(),reason); lines.saveAll(requestLines);
    }

    private void reopen(LeaveRequestEntity request,CurrentActor actor,String reason){
        var input=new LeaveCalculationService.Input(request.getLeaveTypeId(),request.getStartDate(),request.getEndDate(),DurationMode.valueOf(request.getDurationMode()),request.getReason());
        var result=calculations.calculate(request.getEmployeeId(),input); // current policy, date, duration, weekly-off, holiday, and balance validation
        var desired=result.calculation().slots();
        var activeConflicts=slots.findAllByEmployeeIdAndLeaveDateInAndActiveTrue(request.getEmployeeId(),result.calculation().chargeableDates());
        if(activeConflicts.stream().anyMatch(s->!request.getId().equals(s.getRequestId())&&desired.stream().anyMatch(d->d.date().equals(s.getLeaveDate())&&d.slot().equals(s.getSlot())))) throw new DomainException(409,"LEAVE_OVERLAP","The request overlaps active leave");
        var reservations=result.tracksBalance()?balanceService.reserve(request.getEmployeeId(),request.getLeaveTypeId(),desired):List.<LeaveBalanceService.Reservation>of();
        var oldLines=lines.findAllByRequestId(request.getId());
        for(var reservation:reservations){var existing=oldLines.stream().filter(l->l.getBalanceId().equals(reservation.balanceId())).findFirst();if(existing.isPresent()){if(!Set.of("RELEASED","RESTORED").contains(existing.get().getState())) throw new DomainException(409,"BALANCE_STATE_INVALID","The rejected request balance line is not reusable");existing.get().reserve();}else lines.save(LeaveRequestBalanceLineEntity.reserved(request.getId(),reservation.balanceId(),reservation.units()));movements.save(LeaveBalanceMovementEntity.correction(reservation.balanceId(),request.getId(),reservation.units(),actor.userId(),"RESERVE",reason));}
        var existingSlots=slots.findAllByRequestId(request.getId()); existingSlots.forEach(LeaveRequestSlotEntity::deactivate);
        for(var desiredSlot:desired){var existing=existingSlots.stream().filter(s->s.getLeaveDate().equals(desiredSlot.date())&&s.getSlot().equals(desiredSlot.slot())).findFirst();if(existing.isPresent())existing.get().activate();else slots.save(LeaveRequestSlotEntity.active(request.getId(),request.getEmployeeId(),desiredSlot.date(),desiredSlot.slot()));}
        request.reopen(); lines.saveAll(oldLines);
    }
}
