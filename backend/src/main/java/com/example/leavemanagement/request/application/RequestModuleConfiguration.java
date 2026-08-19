package com.example.leavemanagement.request.application;

import com.example.leavemanagement.request.domain.LeaveDurationCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RequestModuleConfiguration {
    @Bean LeaveDurationCalculator leaveDurationCalculator() {
        return new LeaveDurationCalculator();
    }
}
