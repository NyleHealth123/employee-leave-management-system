package com.example.leavemanagement.request.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
public interface LeaveRequestSlotRepository extends JpaRepository<LeaveRequestSlotEntity, UUID> {
 boolean existsByEmployeeIdAndLeaveDateBetweenAndActiveTrue(UUID employeeId,LocalDate from,LocalDate to);
 List<LeaveRequestSlotEntity> findAllByEmployeeIdAndLeaveDateInAndActiveTrue(UUID employeeId,Collection<LocalDate> dates);
}

