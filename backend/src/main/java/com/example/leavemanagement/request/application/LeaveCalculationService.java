package com.example.leavemanagement.request.application;

import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.calendar.application.HolidayQueryService;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.domain.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.UUID;

@Service @Transactional(readOnly=true)
public class LeaveCalculationService {
    private final LeavePolicyQueryService policies;private final HolidayQueryService holidays;private final LeaveBalanceService balances;private final LeaveDurationCalculator calculator;
    public LeaveCalculationService(LeavePolicyQueryService policies,HolidayQueryService holidays,LeaveBalanceService balances,LeaveDurationCalculator calculator){this.policies=policies;this.holidays=holidays;this.balances=balances;this.calculator=calculator;}
    public CalculationResult calculate(UUID employeeId,Input input){var policy=policies.effective(input.leaveTypeId(),input.startDate());for(var date=input.startDate();!date.isAfter(input.endDate());date=date.plusDays(1)){if(!policies.effective(input.leaveTypeId(),date).getId().equals(policy.getId()))throw new DomainException(409,"POLICY_CHANGED","A request cannot cross a policy-version boundary");}var calculation=calculator.calculate(input.startDate(),input.endDate(),input.durationMode(),policy.isAllowsHalfDay(),policy.excludesWeeklyOffs(),policy.getWeeklyOffDays(),policy.excludesHolidays(),holidays.activeDates(input.startDate(),input.endDate()));var available=policy.isTracksBalance()?balances.availableDays(employeeId,input.leaveTypeId()):null;var weeklyOffs=policy.getWeeklyOffDays().stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));var snapshot="{\"policyVersionId\":\""+policy.getId()+"\",\"tracksBalance\":"+policy.isTracksBalance()+",\"allowsHalfDay\":"+policy.isAllowsHalfDay()+",\"weeklyOffTreatment\":\""+policy.getWeeklyOffTreatment()+"\",\"holidayTreatment\":\""+policy.getHolidayTreatment()+"\",\"weeklyOffDays\":["+weeklyOffs+"],\"rejectionCommentRequired\":"+policy.isRejectionCommentRequired()+",\"cancellationCutoffDays\":"+policy.getCancellationCutoffDays()+"}";return new CalculationResult(policy.getId(),policy.isTracksBalance(),snapshot,calculation,available,available==null||available>=calculation.chargeableUnits().days());}
    public record Input(UUID leaveTypeId,LocalDate startDate,LocalDate endDate,DurationMode durationMode,String reason){}
    public record CalculationResult(UUID policyId,boolean tracksBalance,String policySnapshot,LeaveCalculation calculation,Double availableDays,boolean canSubmit){}
}
