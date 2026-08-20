package com.example.leavemanagement.balance.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, UUID> {
 List<LeaveBalanceEntity> findAllByEmployeeIdOrderByPeriodStartAsc(UUID employeeId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select b from LeaveBalanceEntity b where b.employeeId=:employeeId and b.leaveTypeId=:leaveTypeId and b.periodEnd>=:from and b.periodStart<=:to order by b.periodStart,b.id")
 List<LeaveBalanceEntity> lockApplicable(@Param("employeeId") UUID employeeId,@Param("leaveTypeId") UUID leaveTypeId,@Param("from") LocalDate from,@Param("to") LocalDate to);
 @Lock(LockModeType.PESSIMISTIC_WRITE) java.util.Optional<LeaveBalanceEntity> findById(UUID id);
}
