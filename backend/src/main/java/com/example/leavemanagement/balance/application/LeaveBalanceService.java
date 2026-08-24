package com.example.leavemanagement.balance.application;

import com.example.leavemanagement.balance.persistence.*;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.domain.LeaveCalculation;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository balances;private final LeavePolicyQueryService policies;
    public LeaveBalanceService(LeaveBalanceRepository balances,LeavePolicyQueryService policies){this.balances=balances;this.policies=policies;}
    @Transactional(readOnly=true) public List<BalanceView> own(UUID employeeId){return balances.findAllByEmployeeIdOrderByPeriodStartAsc(employeeId).stream().map(this::view).toList();}
    @Transactional(readOnly=true) public double availableDays(UUID employeeId,UUID leaveTypeId){return balances.findAllByEmployeeIdOrderByPeriodStartAsc(employeeId).stream().filter(b->b.getLeaveTypeId().equals(leaveTypeId)).mapToInt(LeaveBalanceEntity::getAvailableUnits).sum()/2.0;}
    public List<Reservation> reserve(UUID employeeId,UUID leaveTypeId,List<LeaveCalculation.CalculatedSlot> slots){var from=slots.getFirst().date();var to=slots.getLast().date();var locked=balances.lockApplicable(employeeId,leaveTypeId,from,to);var units=new LinkedHashMap<LeaveBalanceEntity,Integer>();for(var slot:slots){var balance=locked.stream().filter(b->!slot.date().isBefore(b.getPeriodStart())&&!slot.date().isAfter(b.getPeriodEnd())).findFirst().orElseThrow(()->new DomainException(409,"INSUFFICIENT_BALANCE","No allocated balance covers every chargeable date"));units.merge(balance,1,Integer::sum);}if(units.entrySet().stream().anyMatch(e->e.getKey().getAvailableUnits()<e.getValue()))throw new DomainException(409,"INSUFFICIENT_BALANCE","Insufficient unreserved balance");units.forEach(LeaveBalanceEntity::reserve);return units.entrySet().stream().map(e->new Reservation(e.getKey().getId(),e.getValue())).toList();}
    private BalanceView view(LeaveBalanceEntity b){return new BalanceView(b.getId(),b.getLeaveTypeId(),policies.typeName(b.getLeaveTypeId()),b.getPeriodStart(),b.getPeriodEnd(),(b.getAllocatedUnits()+b.getAdjustmentUnits())/2.0,b.getReservedUnits()/2.0,b.getConsumedUnits()/2.0,b.getAvailableUnits()/2.0,b.getVersion());}
    public record Reservation(UUID balanceId,int units){}
    public record BalanceView(UUID id,UUID leaveTypeId,String leaveTypeName,java.time.LocalDate periodStart,java.time.LocalDate periodEnd,double entitledDays,double reservedDays,double consumedDays,double availableDays,long version){}
}
