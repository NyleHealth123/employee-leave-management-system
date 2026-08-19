package com.example.leavemanagement.policy.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
public interface LeavePolicyVersionRepository extends JpaRepository<LeavePolicyVersionEntity, UUID> {
 @Query("select p from LeavePolicyVersionEntity p where p.leaveType.id=:typeId and p.effectiveFrom<=:date and (p.effectiveTo is null or p.effectiveTo>=:date)") Optional<LeavePolicyVersionEntity> findEffective(@Param("typeId") UUID typeId,@Param("date") LocalDate date);
}

