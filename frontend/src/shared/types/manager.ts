import type { LeaveBalance, LeaveRequestDetail, LeaveRequestSummary, Page } from './leave'
export interface ManagerRequestDetail extends LeaveRequestDetail { relevantBalance: LeaveBalance | null }
export interface ManagerDashboard { pendingRequests: LeaveRequestSummary[]; approvedUpcomingLeave: LeaveRequestSummary[]; pendingCount: number }
export type ManagerRequestPage = Page<LeaveRequestSummary>
