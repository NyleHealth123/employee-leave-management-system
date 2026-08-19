package com.example.leavemanagement.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfiguration {
    @Bean Clock organizationClock(@Value("${app.organization-time-zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
