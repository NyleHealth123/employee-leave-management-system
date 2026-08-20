package com.example.leavemanagement.people.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfileEntity, UUID> {
    Optional<EmployeeProfileEntity> findByUserAccountId(UUID accountId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<EmployeeProfileEntity> findLockedById(UUID id);
    Page<EmployeeProfileEntity> findAllByOrderByDisplayNameAsc(Pageable pageable);
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmailIgnoreCase(String email);
}
