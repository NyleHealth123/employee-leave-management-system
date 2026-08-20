package com.example.leavemanagement.calendar.application;
import com.example.leavemanagement.request.persistence.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;import java.util.*;
@Service @Transactional(readOnly=true)
public class ManagerTeamCalendarService {private final LeaveRequestRepository requests;public ManagerTeamCalendarService(LeaveRequestRepository requests){this.requests=requests;}public List<Entry> entries(UUID managerId,LocalDate from,LocalDate to){return requests.findManagerTeamCalendar(managerId,from,to).stream().map(r->new Entry(r.getEmployeeDisplayName(),r.getStartDate(),r.getEndDate(),r.getDurationMode(),r.getStatus(),r.getLeaveTypeName())).toList();}public record Entry(String employeeDisplayName,LocalDate startDate,LocalDate endDate,String durationMode,String status,String leaveTypeName){}}
