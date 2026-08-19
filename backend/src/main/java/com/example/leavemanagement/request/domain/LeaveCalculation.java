package com.example.leavemanagement.request.domain;
import java.time.LocalDate;
import java.util.List;
public record LeaveCalculation(LeaveUnits chargeableUnits,List<CalculatedSlot> slots,List<ExcludedDate> excludedDates) {
    public record CalculatedSlot(LocalDate date,String slot) {}
    public record ExcludedDate(LocalDate date,ExclusionReason reason) {}
    public enum ExclusionReason { WEEKLY_OFF, HOLIDAY }
    public List<LocalDate> chargeableDates(){return slots.stream().map(CalculatedSlot::date).distinct().toList();}
}

