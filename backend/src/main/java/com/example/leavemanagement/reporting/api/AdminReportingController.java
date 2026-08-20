package com.example.leavemanagement.reporting.api;

import com.example.leavemanagement.reporting.application.LeaveReportingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminReportingController {
    private final LeaveReportingService reporting;
    public AdminReportingController(LeaveReportingService reporting) { this.reporting = reporting; }
    @GetMapping("/leave-requests")
    public LeaveReportingService.PageView requests(@RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return reporting.list(from, to, status, page, size); }
    @GetMapping("/reports/leave-summary")
    public LeaveReportingService.SummaryReport summary(@RequestParam LocalDate from, @RequestParam LocalDate to) { return reporting.summary(from, to); }
}
