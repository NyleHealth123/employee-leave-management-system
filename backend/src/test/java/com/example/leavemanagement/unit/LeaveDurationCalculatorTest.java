package com.example.leavemanagement.unit;

import com.example.leavemanagement.request.domain.*;
import com.example.leavemanagement.request.application.LeaveCalculationService;
import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.calendar.application.HolidayQueryService;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.policy.persistence.LeavePolicyVersionEntity;
import com.example.leavemanagement.shared.api.DomainException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveDurationCalculatorTest {
    private final LeaveDurationCalculator calculator=new LeaveDurationCalculator();
    @Test void excludesConfiguredWeeklyOffsAndHolidays(){var result=calculator.calculate(LocalDate.of(2026,8,21),LocalDate.of(2026,8,24),DurationMode.FULL_DAY,true,true,Set.of(6,7),true,Set.of(LocalDate.of(2026,8,24)));assertThat(result.chargeableUnits().days()).isEqualTo(1);assertThat(result.excludedDates()).extracting(LeaveCalculation.ExcludedDate::reason).containsExactly(LeaveCalculation.ExclusionReason.WEEKLY_OFF,LeaveCalculation.ExclusionReason.WEEKLY_OFF,LeaveCalculation.ExclusionReason.HOLIDAY);}
    @Test void fullDaysMaterializeBothSlotsForEveryChargeableDate(){var result=calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,20),DurationMode.FULL_DAY,true,false,Set.of(),false,Set.of());assertThat(result.chargeableUnits().days()).isEqualTo(2);assertThat(result.slots()).extracting(LeaveCalculation.CalculatedSlot::slot).containsExactly("AM","PM","AM","PM");}
    @ParameterizedTest @EnumSource(value=DurationMode.class,names={"HALF_DAY_AM","HALF_DAY_PM"}) void supportsPermittedHalfDays(DurationMode mode){var result=calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,19),mode,true,false,Set.of(),false,Set.of());assertThat(result.chargeableUnits().days()).isEqualTo(.5);assertThat(result.slots()).singleElement().extracting(LeaveCalculation.CalculatedSlot::slot).isEqualTo(mode==DurationMode.HALF_DAY_AM?"AM":"PM");}
    @Test void rejectsInvertedRange(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,20),LocalDate.of(2026,8,19),DurationMode.FULL_DAY,true,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("VALIDATION_FAILED");}
    @Test void rejectsDisallowedHalfDay(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,19),DurationMode.HALF_DAY_AM,false,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class);}
    @Test void rejectsHalfDayDateRange(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,20),DurationMode.HALF_DAY_PM,true,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("VALIDATION_FAILED");}
    @Test void rejectsZeroChargeableDays(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,22),LocalDate.of(2026,8,23),DurationMode.FULL_DAY,true,true,Set.of(6,7),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("NO_CHARGEABLE_DAYS");}
    @Test void includedWeeklyOffsAndHolidaysRemainChargeable(){var holiday=LocalDate.of(2026,8,22);var result=calculator.calculate(holiday,holiday,DurationMode.FULL_DAY,true,false,Set.of(6),false,Set.of(holiday));assertThat(result.chargeableUnits().days()).isEqualTo(1);assertThat(result.excludedDates()).isEmpty();}
    @Test void rejectsNullOrEmptyInput(){assertThatThrownBy(()->calculator.calculate(null,LocalDate.of(2026,8,19),DurationMode.FULL_DAY,true,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("VALIDATION_FAILED");}

    @Test void calculationCannotCrossEffectivePolicyBoundary(){
        var policies=mock(LeavePolicyQueryService.class);var holidays=mock(HolidayQueryService.class);var balances=mock(LeaveBalanceService.class);
        var first=mock(LeavePolicyVersionEntity.class);var second=mock(LeavePolicyVersionEntity.class);var typeId=UUID.randomUUID();var start=LocalDate.of(2026,8,31);var end=start.plusDays(1);
        when(first.getId()).thenReturn(UUID.randomUUID());when(second.getId()).thenReturn(UUID.randomUUID());when(policies.effective(typeId,start)).thenReturn(first);when(policies.effective(typeId,end)).thenReturn(second);
        var service=new LeaveCalculationService(policies,holidays,balances,calculator);
        assertThatThrownBy(()->service.calculate(UUID.randomUUID(),new LeaveCalculationService.Input(typeId,start,end,DurationMode.FULL_DAY,"Rest"))).isInstanceOf(DomainException.class).extracting("code").isEqualTo("POLICY_CHANGED");
        verifyNoInteractions(holidays,balances);
    }
}
