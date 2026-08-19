package com.example.leavemanagement.request.persistence;
import org.springframework.data.repository.Repository;
import java.util.UUID;
public interface LeaveRequestBalanceLineRepository extends Repository<LeaveRequestBalanceLineEntity, UUID> { LeaveRequestBalanceLineEntity save(LeaveRequestBalanceLineEntity line); }

