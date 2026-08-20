package com.example.leavemanagement.request.persistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, UUID> {
 Page<LeaveRequestEntity> findAllByEmployeeId(UUID employeeId, Pageable pageable);
 Page<LeaveRequestEntity> findAllByEmployeeIdAndStatus(UUID employeeId,String status,Pageable pageable);
 Optional<LeaveRequestEntity> findByIdAndEmployeeId(UUID id,UUID employeeId);
 Optional<LeaveRequestEntity> findByEmployeeIdAndIdempotencyKey(UUID employeeId,String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from LeaveRequestEntity r where r.id=:id and r.employeeId=:employeeId")
 Optional<LeaveRequestEntity> lockOwned(@Param("id") UUID id,@Param("employeeId") UUID employeeId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from LeaveRequestEntity r where r.id=:id")
 Optional<LeaveRequestEntity> lockById(@Param("id") UUID id);
 java.util.List<LeaveRequestEntity> findTop20ByEmployeeIdAndStatusOrderByStartDateAsc(UUID employeeId,String status);
 @Query("select r from LeaveRequestEntity r where r.employeeId in (select e.id from EmployeeProfileEntity e where e.manager.id=:managerId) order by r.submittedAt desc")
 Page<LeaveRequestEntity> findDirectReports(@Param("managerId") UUID managerId, Pageable pageable);
 @Query("select r from LeaveRequestEntity r where r.employeeId in (select e.id from EmployeeProfileEntity e where e.manager.id=:managerId) and r.status=:status order by r.submittedAt desc")
 Page<LeaveRequestEntity> findDirectReportsByStatus(@Param("managerId") UUID managerId,@Param("status") String status,Pageable pageable);
 @Query("select r from LeaveRequestEntity r where r.id=:id and r.employeeId in (select e.id from EmployeeProfileEntity e where e.manager.id=:managerId)")
 java.util.Optional<LeaveRequestEntity> findDirectReport(@Param("id") UUID id,@Param("managerId") UUID managerId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from LeaveRequestEntity r where r.id=:id and r.employeeId in (select e.id from EmployeeProfileEntity e where e.manager.id=:managerId)")
 java.util.Optional<LeaveRequestEntity> lockDirectReport(@Param("id") UUID id,@Param("managerId") UUID managerId);
 @Query(value="select e.display_name as employeeDisplayName, r.start_date as startDate, r.end_date as endDate, r.duration_mode as durationMode, r.status as status, lt.name as leaveTypeName from leave_request r join employee_profile e on e.id=r.employee_id join leave_type lt on lt.id=r.leave_type_id where e.manager_id=:managerId and r.status in ('PENDING','APPROVED') and r.start_date<=:toDate and r.end_date>=:fromDate order by r.start_date,e.display_name",nativeQuery=true)
 java.util.List<ManagerCalendarRow> findManagerTeamCalendar(@Param("managerId") UUID managerId,@Param("fromDate") java.time.LocalDate from,@Param("toDate") java.time.LocalDate to);
 interface ManagerCalendarRow {String getEmployeeDisplayName();java.time.LocalDate getStartDate();java.time.LocalDate getEndDate();String getDurationMode();String getStatus();String getLeaveTypeName();}
 @org.springframework.data.jpa.repository.Query(value="select e.display_name as employeeDisplayName, r.start_date as startDate, r.end_date as endDate, r.status as status from leave_request r join employee_profile e on e.id=r.employee_id join employee_profile viewer on viewer.id=:viewerId where r.status in ('PENDING','APPROVED') and (r.employee_id=:viewerId or (viewer.manager_id is not null and e.active=true and e.manager_id=viewer.manager_id)) order by r.start_date,e.display_name",nativeQuery=true)
 java.util.List<TeamCalendarRow> findEmployeeTeamCalendar(@org.springframework.data.repository.query.Param("viewerId") UUID viewerId);
 interface TeamCalendarRow {String getEmployeeDisplayName();java.time.LocalDate getStartDate();java.time.LocalDate getEndDate();String getStatus();}
}
