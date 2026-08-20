package com.example.leavemanagement.request.persistence;

import org.springframework.data.domain.*;import org.springframework.stereotype.Repository;import java.util.*;
/** Explicit manager projection boundary; all methods delegate to predicates on LeaveRequestRepository. */
@Repository public class ManagerLeaveRequestQueryRepository {private final LeaveRequestRepository requests;public ManagerLeaveRequestQueryRepository(LeaveRequestRepository requests){this.requests=requests;}public Page<LeaveRequestEntity> directReports(UUID managerId,Pageable page){return requests.findDirectReports(managerId,page);}public Page<LeaveRequestEntity> directReports(UUID managerId,String status,Pageable page){return requests.findDirectReportsByStatus(managerId,status,page);}public Optional<LeaveRequestEntity> detail(UUID managerId,UUID requestId){return requests.findDirectReport(requestId,managerId);}}
