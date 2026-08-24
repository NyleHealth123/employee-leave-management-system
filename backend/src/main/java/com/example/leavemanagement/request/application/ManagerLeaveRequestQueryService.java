package com.example.leavemanagement.request.application;

import com.example.leavemanagement.balance.application.LeaveBalanceService;
import com.example.leavemanagement.balance.persistence.LeaveBalanceRepository;
import com.example.leavemanagement.people.persistence.EmployeeProfileRepository;
import com.example.leavemanagement.policy.application.LeavePolicyQueryService;
import com.example.leavemanagement.request.persistence.*;
import com.example.leavemanagement.shared.api.DomainException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ManagerLeaveRequestQueryService {
    private final LeaveRequestRepository requests; private final LeaveRequestStatusHistoryRepository history;
    private final EmployeeProfileRepository employees; private final LeavePolicyQueryService policies;
    private final LeaveRequestBalanceLineRepository lines; private final LeaveBalanceRepository balances;
    public ManagerLeaveRequestQueryService(LeaveRequestRepository requests,LeaveRequestStatusHistoryRepository history,EmployeeProfileRepository employees,LeavePolicyQueryService policies,LeaveRequestBalanceLineRepository lines,LeaveBalanceRepository balances){this.requests=requests;this.history=history;this.employees=employees;this.policies=policies;this.lines=lines;this.balances=balances;}
    public PageView list(UUID managerId,int page,int size,String status){var p=PageRequest.of(page,Math.min(size,100),Sort.by(Sort.Direction.DESC,"submittedAt"));var found=status==null?requests.findDirectReports(managerId,p):requests.findDirectReportsByStatus(managerId,status,p);return new PageView(found.stream().map(this::summary).toList(),page,size,found.getTotalElements(),found.getTotalPages());}
    public Detail detail(UUID managerId,UUID id){var r=requests.findDirectReport(id,managerId).orElseThrow(()->new DomainException(404,"RESOURCE_NOT_FOUND","Leave request was not found"));return detail(r);}
    public Detail detail(LeaveRequestEntity r){var e=employees.findById(r.getEmployeeId()).orElseThrow();var s=summary(r);var hs=history.findAllByRequestIdOrderByCreatedAtAsc(r.getId()).stream().map(h->new HistoryEntry(h.getFromStatus(),h.getToStatus(),e.getDisplayName(),h.getComment(),h.getCreatedAt())).toList();LeaveBalanceService.BalanceView relevant=null;var l=lines.findAllByRequestId(r.getId()).stream().findFirst();if(l.isPresent()){var b=balances.findById(l.get().getBalanceId());if(b.isPresent()){var x=b.get();relevant=new LeaveBalanceService.BalanceView(x.getId(),x.getLeaveTypeId(),policies.typeName(x.getLeaveTypeId()),x.getPeriodStart(),x.getPeriodEnd(),(x.getAllocatedUnits()+x.getAdjustmentUnits())/2.0,x.getReservedUnits()/2.0,x.getConsumedUnits()/2.0,x.getAvailableUnits()/2.0,x.getVersion());}}return new Detail(s.id(),s.employeeId(),s.employeeName(),s.leaveTypeId(),s.leaveTypeName(),s.startDate(),s.endDate(),s.durationMode(),s.chargeableDays(),s.status(),s.submittedAt(),s.version(),r.getReason(),r.getDecisionComment(),relevant,hs);}
    public EmployeeLeaveRequestQueryService.Detail decisionDetail(LeaveRequestEntity r){var d=detail(r);var hs=d.statusHistory().stream().map(h->new EmployeeLeaveRequestQueryService.HistoryEntry(h.fromStatus(),h.toStatus(),h.actorDisplayName(),h.comment(),h.occurredAt())).toList();return new EmployeeLeaveRequestQueryService.Detail(d.id(),d.employeeId(),d.employeeName(),d.leaveTypeId(),d.leaveTypeName(),d.startDate(),d.endDate(),d.durationMode(),d.chargeableDays(),d.status(),d.submittedAt(),d.version(),d.reason(),d.decisionComment(),false,null,hs);}
    private Summary summary(LeaveRequestEntity r){var e=employees.findById(r.getEmployeeId()).orElseThrow();return new Summary(r.getId(),r.getEmployeeId(),e.getDisplayName(),r.getLeaveTypeId(),policies.typeName(r.getLeaveTypeId()),r.getStartDate(),r.getEndDate(),r.getDurationMode(),r.getChargeableUnits()/2.0,r.getStatus(),r.getSubmittedAt(),r.getVersion());}
    public record Summary(UUID id,UUID employeeId,String employeeName,UUID leaveTypeId,String leaveTypeName,LocalDate startDate,LocalDate endDate,String durationMode,double chargeableDays,String status,Instant submittedAt,long version){}
    public record HistoryEntry(String fromStatus,String toStatus,String actorDisplayName,String comment,Instant occurredAt){}
    public record Detail(UUID id,UUID employeeId,String employeeName,UUID leaveTypeId,String leaveTypeName,LocalDate startDate,LocalDate endDate,String durationMode,double chargeableDays,String status,Instant submittedAt,long version,String reason,String decisionComment,LeaveBalanceService.BalanceView relevantBalance,List<HistoryEntry> statusHistory){}
    public record PageView(List<Summary> content,int page,int size,long totalElements,int totalPages){}
}
