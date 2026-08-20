import type { LeaveStatus, Page } from './leave'

export interface OrganizationLeaveRequest { id: string; employeeId: string; employeeName: string; leaveTypeId: string; leaveTypeName: string; startDate: string; endDate: string; durationMode: string; chargeableDays: number; status: LeaveStatus; submittedAt: string; version: number }
export interface SummaryBucket { key: string; requestCount: number; chargeableDays: number }
export interface LeaveSummaryReport { from: string; to: string; byStatus: SummaryBucket[]; byLeaveType: SummaryBucket[] }
export interface AuditEvent { id: string; actorUserId: string; action: string; entityType: string; entityId: string; occurredAt: string; reason: string | null; beforeData: Record<string, unknown> | null; afterData: Record<string, unknown> | null; correlationId: string | null }
export type OrganizationLeaveRequestPage = Page<OrganizationLeaveRequest>
export type AuditEventPage = Page<AuditEvent>
