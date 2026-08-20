package com.example.leavemanagement.request.application;

import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service @Transactional(readOnly=true)
public class EmployeeLeaveRequestQueryService {
    private final LeaveRequestRepository requests;private final LeaveRequestStatusHistoryRepository history;private final LeavePolicyQueryService policies;private final EmployeeProfileRepository employees;
    public EmployeeLeaveRequestQueryService(LeaveRequestRepository requests,LeaveRequestStatusHistoryRepository history,LeavePolicyQueryService policies,EmployeeProfileRepository employees){this.requests=requests;this.history=history;this.policies=policies;this.employees=employees;}
    public PageView list(UUID employeeId,int page,int size){var found=requests.findAllByEmployeeId(employeeId,PageRequest.of(page,Math.min(size,100),Sort.by(Sort.Direction.DESC,"submittedAt")));return new PageView(found.stream().map(this::summary).toList(),page,size,found.getTotalElements(),found.getTotalPages());}
    public PageView list(UUID employeeId,int page,int size,String status){var pageable=PageRequest.of(page,Math.min(size,100),Sort.by(Sort.Direction.DESC,"submittedAt"));var found=requests.findAllByEmployeeIdAndStatus(employeeId,status,pageable);return new PageView(found.stream().map(this::summary).toList(),page,size,found.getTotalElements(),found.getTotalPages());}
    public Detail detail(UUID employeeId,UUID requestId){return detail(requests.findByIdAndEmployeeId(requestId,employeeId).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found")));}
    public Detail detail(LeaveRequestEntity request){var employee=employees.findById(request.getEmployeeId()).orElseThrow();var entries=history.findAllByRequestIdOrderByCreatedAtAsc(request.getId()).stream().map(h->new HistoryEntry(h.getFromStatus(),h.getToStatus(),employee.getDisplayName(),h.getComment(),h.getCreatedAt())).toList();var s=summary(request);return new Detail(s.id(),s.employeeId(),s.employeeName(),s.leaveTypeId(),s.leaveTypeName(),s.startDate(),s.endDate(),s.durationMode(),s.chargeableDays(),s.status(),s.submittedAt(),s.version(),request.getReason(),request.getDecisionComment(),"PENDING".equals(request.getStatus()),null,entries);}
    public Summary summary(LeaveRequestEntity r){var employee=employees.findById(r.getEmployeeId()).orElseThrow();return new Summary(r.getId(),r.getEmployeeId(),employee.getDisplayName(),r.getLeaveTypeId(),policies.typeName(r.getLeaveTypeId()),r.getStartDate(),r.getEndDate(),r.getDurationMode(),r.getChargeableUnits()/2.0,r.getStatus(),r.getSubmittedAt(),r.getVersion());}
    public record Summary(UUID id,UUID employeeId,String employeeName,UUID leaveTypeId,String leaveTypeName,LocalDate startDate,LocalDate endDate,String durationMode,double chargeableDays,String status,Instant submittedAt,long version){}
    public record HistoryEntry(String fromStatus,String toStatus,String actorDisplayName,String comment,Instant occurredAt){}
    public record Detail(UUID id,UUID employeeId,String employeeName,UUID leaveTypeId,String leaveTypeName,LocalDate startDate,LocalDate endDate,String durationMode,double chargeableDays,String status,Instant submittedAt,long version,String reason,String decisionComment,boolean canCancel,String cancellationBlockedReason,List<HistoryEntry> statusHistory){}
    public record PageView(List<Summary> content,int page,int size,long totalElements,int totalPages){}
}
