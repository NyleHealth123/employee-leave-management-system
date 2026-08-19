package com.example.leavemanagement.auth.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> { Optional<UserAccountEntity> findByNormalizedLogin(String normalizedLogin); }

