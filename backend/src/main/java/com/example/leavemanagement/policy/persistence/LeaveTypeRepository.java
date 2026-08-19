package com.example.leavemanagement.policy.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface LeaveTypeRepository extends JpaRepository<LeaveTypeEntity, UUID> { List<LeaveTypeEntity> findAllByActiveTrueOrderByNameAsc(); }

