package com.example.leavemanagement.policy.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
public interface CompanyHolidayRepository extends JpaRepository<CompanyHolidayEntity, UUID> {
 List<CompanyHolidayEntity> findAllByActiveTrueAndDateBetweenOrderByDate(LocalDate from,LocalDate to);
 Set<CompanyHolidayEntity> findAllByActiveTrueAndDateBetween(LocalDate from,LocalDate to);
 List<CompanyHolidayEntity> findAllByDateBetweenOrderByDate(LocalDate from,LocalDate to);
 boolean existsByDate(LocalDate date);
}
