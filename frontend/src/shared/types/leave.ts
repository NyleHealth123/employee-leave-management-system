export type Role = 'EMPLOYEE' | 'MANAGER' | 'ADMINISTRATOR'
export type DurationMode = 'FULL_DAY' | 'HALF_DAY_AM' | 'HALF_DAY_PM'
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export interface Principal { userId: string; employeeId: string; displayName: string; roles: Role[] }
export interface LeaveType { id: string; code: string; name: string; tracksBalance: boolean; allowsHalfDay: boolean; cancellationCutoffDays: number }
export interface Holiday { id: string; date: string; name: string; active: boolean; version: number }
export interface LeaveBalance { id: string; leaveTypeId: string; leaveTypeName: string; periodStart: string; periodEnd: string; entitledDays: number; reservedDays: number; consumedDays: number; availableDays: number; version: number }
export interface LeaveRequestInput { leaveTypeId: string; startDate: string; endDate: string; durationMode: DurationMode; reason: string }
export interface ExcludedDate { date: string; reason: 'WEEKLY_OFF' | 'HOLIDAY' }
export interface LeaveCalculation { chargeableDays: number; chargeableDates: string[]; excludedDates: ExcludedDate[]; tracksBalance: boolean; availableDays?: number; canSubmit: boolean; messages: string[] }
export interface LeaveRequestSummary { id: string; employeeId: string; employeeName: string; leaveTypeId: string; leaveTypeName: string; startDate: string; endDate: string; durationMode: DurationMode; chargeableDays: number; status: LeaveStatus; submittedAt: string; version: number }
export interface StatusHistory { fromStatus: LeaveStatus | null; toStatus: LeaveStatus; actorDisplayName: string; comment: string | null; occurredAt: string }
export interface LeaveRequestDetail extends LeaveRequestSummary { reason: string; decisionComment: string | null; canCancel: boolean; cancellationBlockedReason: string | null; statusHistory: StatusHistory[] }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface EmployeeDashboard { balances: LeaveBalance[]; pendingRequests: LeaveRequestSummary[]; approvedUpcomingLeave: LeaveRequestSummary[]; upcomingHolidays: Holiday[] }
export interface TeamCalendarEntry { employeeDisplayName: string; startDate: string; endDate: string; status: 'PENDING' | 'APPROVED' }
export interface ManagerCalendarEntry extends TeamCalendarEntry { durationMode: DurationMode; leaveTypeName: string }
