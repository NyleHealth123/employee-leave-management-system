package com.example.leavemanagement.calendar.application;
import com.example.leavemanagement.request.persistence.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service @Transactional(readOnly=true)
public class EmployeeTeamCalendarService {
 private final LeaveRequestRepository requests;public EmployeeTeamCalendarService(LeaveRequestRepository requests){this.requests=requests;}
 public List<Entry> entries(UUID viewerId,LocalDate from,LocalDate to){return requests.findEmployeeTeamCalendar(viewerId).stream().filter(r->!r.getEndDate().isBefore(from)&&!r.getStartDate().isAfter(to)).map(r->new Entry(r.getEmployeeDisplayName(),r.getStartDate(),r.getEndDate(),r.getStatus())).toList();}
 public record Entry(String employeeDisplayName,LocalDate startDate,LocalDate endDate,String status){}
}

