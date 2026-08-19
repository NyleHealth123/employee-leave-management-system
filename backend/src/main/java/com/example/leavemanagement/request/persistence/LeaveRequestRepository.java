package com.example.leavemanagement.request.persistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, UUID> {
 Page<LeaveRequestEntity> findAllByEmployeeId(UUID employeeId, Pageable pageable);
 Optional<LeaveRequestEntity> findByIdAndEmployeeId(UUID id,UUID employeeId);
 Optional<LeaveRequestEntity> findByEmployeeIdAndIdempotencyKey(UUID employeeId,String key);
 java.util.List<LeaveRequestEntity> findTop20ByEmployeeIdAndStatusOrderByStartDateAsc(UUID employeeId,String status);
 @org.springframework.data.jpa.repository.Query(value="select e.display_name as employeeDisplayName, r.start_date as startDate, r.end_date as endDate, r.status as status from leave_request r join employee_profile e on e.id=r.employee_id join employee_profile viewer on viewer.id=:viewerId where r.status in ('PENDING','APPROVED') and (r.employee_id=:viewerId or (viewer.manager_id is not null and e.active=true and e.manager_id=viewer.manager_id)) order by r.start_date,e.display_name",nativeQuery=true)
 java.util.List<TeamCalendarRow> findEmployeeTeamCalendar(@org.springframework.data.repository.query.Param("viewerId") UUID viewerId);
 interface TeamCalendarRow {String getEmployeeDisplayName();java.time.LocalDate getStartDate();java.time.LocalDate getEndDate();String getStatus();}
}
