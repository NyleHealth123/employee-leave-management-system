package com.example.leavemanagement.request.application;

import com.example.leavemanagement.audit.persistence.*;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class LeaveSubmissionService {
    private final LeaveCalculationService calculations;private final LeaveBalanceService balances;private final EmployeeProfileRepository employees;private final LeaveRequestRepository requests;private final LeaveRequestSlotRepository slots;private final LeaveRequestBalanceLineRepository lines;private final LeaveBalanceMovementRepository movements;private final LeaveRequestStatusHistoryRepository history;private final AuditEventRepository audits;private final LeaveSubmissionExceptionTranslator conflicts;
    public LeaveSubmissionService(LeaveCalculationService calculations,LeaveBalanceService balances,EmployeeProfileRepository employees,LeaveRequestRepository requests,LeaveRequestSlotRepository slots,LeaveRequestBalanceLineRepository lines,LeaveBalanceMovementRepository movements,LeaveRequestStatusHistoryRepository history,AuditEventRepository audits,LeaveSubmissionExceptionTranslator conflicts){this.calculations=calculations;this.balances=balances;this.employees=employees;this.requests=requests;this.slots=slots;this.lines=lines;this.movements=movements;this.history=history;this.audits=audits;this.conflicts=conflicts;}
    @Transactional public LeaveRequestEntity submit(UUID actorUserId,UUID employeeId,LeaveCalculationService.Input input,String idempotencyKey){if(idempotencyKey!=null&&!idempotencyKey.isBlank()){employees.findLockedById(employeeId).orElseThrow(()->new DomainException(404,"EMPLOYEE_NOT_FOUND","Employee profile not found"));var existing=requests.findByEmployeeIdAndIdempotencyKey(employeeId,idempotencyKey);if(existing.isPresent())return existing.get();}var result=calculations.calculate(employeeId,input);var conflicting=slots.findAllByEmployeeIdAndLeaveDateInAndActiveTrue(employeeId,result.calculation().chargeableDates());if(conflicting.stream().anyMatch(s->result.calculation().slots().stream().anyMatch(c->c.date().equals(s.getLeaveDate())&&c.slot().equals(s.getSlot()))))throw new DomainException(409,"LEAVE_OVERLAP","The request overlaps active leave");var request=LeaveRequestEntity.pending(employeeId,input.leaveTypeId(),result.policyId(),input.startDate(),input.endDate(),input.durationMode().name(),result.calculation().chargeableUnits().value(),input.reason(),result.policySnapshot(),blankToNull(idempotencyKey));try{var reservations=result.tracksBalance()?balances.reserve(employeeId,input.leaveTypeId(),result.calculation().slots()):java.util.List.<LeaveBalanceService.Reservation>of();requests.saveAndFlush(request);for(var reservation:reservations){lines.save(LeaveRequestBalanceLineEntity.reserved(request.getId(),reservation.balanceId(),reservation.units()));movements.save(LeaveBalanceMovementEntity.reservation(reservation.balanceId(),request.getId(),reservation.units(),actorUserId));}slots.saveAll(result.calculation().slots().stream().map(s->LeaveRequestSlotEntity.active(request.getId(),employeeId,s.date(),s.slot())).toList());history.save(LeaveRequestStatusHistoryEntity.submitted(request.getId(),actorUserId));audits.save(AuditEventEntity.submitted(actorUserId,request.getId()));requests.flush();return request;}catch(DataIntegrityViolationException ex){throw conflicts.translate(ex);}}
    private static String blankToNull(String value){return value==null||value.isBlank()?null:value;}
}
