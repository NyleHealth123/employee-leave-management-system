package com.example.leavemanagement.request.application;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
@Component public class LeaveSubmissionExceptionTranslator {
 public DomainException translate(DataIntegrityViolationException ex){var message=String.valueOf(ex.getMostSpecificCause().getMessage());if(message.contains("uq_active_leave_slot"))return new DomainException(409,"LEAVE_OVERLAP","The request overlaps active leave");if(message.contains("ck_balance_available"))return new DomainException(409,"INSUFFICIENT_BALANCE","Insufficient unreserved balance");return new DomainException(409,"REQUEST_CONFLICT","The request conflicts with current data");}
}

