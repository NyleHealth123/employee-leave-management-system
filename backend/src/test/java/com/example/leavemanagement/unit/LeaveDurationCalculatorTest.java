package com.example.leavemanagement.unit;

import com.example.leavemanagement.request.domain.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class LeaveDurationCalculatorTest {
    private final LeaveDurationCalculator calculator=new LeaveDurationCalculator();
    @Test void excludesConfiguredWeeklyOffsAndHolidays(){var result=calculator.calculate(LocalDate.of(2026,8,21),LocalDate.of(2026,8,24),DurationMode.FULL_DAY,true,true,Set.of(6,7),true,Set.of(LocalDate.of(2026,8,24)));assertThat(result.chargeableUnits().days()).isEqualTo(1);assertThat(result.excludedDates()).hasSize(3);}
    @ParameterizedTest @EnumSource(value=DurationMode.class,names={"HALF_DAY_AM","HALF_DAY_PM"}) void supportsPermittedHalfDays(DurationMode mode){assertThat(calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,19),mode,true,false,Set.of(),false,Set.of()).chargeableUnits().days()).isEqualTo(.5);}
    @Test void rejectsInvertedRange(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,20),LocalDate.of(2026,8,19),DurationMode.FULL_DAY,true,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("VALIDATION_FAILED");}
    @Test void rejectsDisallowedHalfDay(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,19),LocalDate.of(2026,8,19),DurationMode.HALF_DAY_AM,false,false,Set.of(),false,Set.of())).isInstanceOf(DomainException.class);}
    @Test void rejectsZeroChargeableDays(){assertThatThrownBy(()->calculator.calculate(LocalDate.of(2026,8,22),LocalDate.of(2026,8,23),DurationMode.FULL_DAY,true,true,Set.of(6,7),false,Set.of())).isInstanceOf(DomainException.class).extracting("code").isEqualTo("NO_CHARGEABLE_DAYS");}
}

