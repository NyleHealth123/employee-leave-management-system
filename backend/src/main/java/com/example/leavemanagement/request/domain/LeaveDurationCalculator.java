package com.example.leavemanagement.request.domain;

import com.example.leavemanagement.shared.api.DomainException;
import java.time.LocalDate;
import java.util.*;

public class LeaveDurationCalculator {
    public LeaveCalculation calculate(LocalDate start,LocalDate end,DurationMode mode,boolean allowsHalfDay,boolean excludeWeeklyOffs,Set<Integer> weeklyOffDays,boolean excludeHolidays,Set<LocalDate> holidays){
        if(start==null||end==null||mode==null)throw new DomainException(400,"VALIDATION_FAILED","Dates and duration mode are required");
        if(start.isAfter(end))throw new DomainException(400,"VALIDATION_FAILED","Start date must not be after end date");
        if(mode!=DurationMode.FULL_DAY){if(!allowsHalfDay)throw new DomainException(400,"VALIDATION_FAILED","This leave type does not allow half days");if(!start.equals(end))throw new DomainException(400,"VALIDATION_FAILED","A half day must use one date");}
        var slots=new ArrayList<LeaveCalculation.CalculatedSlot>();var excluded=new ArrayList<LeaveCalculation.ExcludedDate>();
        for(var date=start;!date.isAfter(end);date=date.plusDays(1)){
            if(excludeWeeklyOffs&&weeklyOffDays.contains(date.getDayOfWeek().getValue())){excluded.add(new LeaveCalculation.ExcludedDate(date,LeaveCalculation.ExclusionReason.WEEKLY_OFF));continue;}
            if(excludeHolidays&&holidays.contains(date)){excluded.add(new LeaveCalculation.ExcludedDate(date,LeaveCalculation.ExclusionReason.HOLIDAY));continue;}
            if(mode==DurationMode.FULL_DAY){slots.add(new LeaveCalculation.CalculatedSlot(date,"AM"));slots.add(new LeaveCalculation.CalculatedSlot(date,"PM"));}
            else slots.add(new LeaveCalculation.CalculatedSlot(date,mode==DurationMode.HALF_DAY_AM?"AM":"PM"));
        }
        if(slots.isEmpty())throw new DomainException(422,"NO_CHARGEABLE_DAYS","Configured rules exclude every requested date");
        return new LeaveCalculation(new LeaveUnits(slots.size()),List.copyOf(slots),List.copyOf(excluded));
    }
}
