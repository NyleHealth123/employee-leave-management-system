import { api } from '../../shared/api/apiClient'
import type { AuditEventPage, LeaveSummaryReport, OrganizationLeaveRequestPage } from '../../shared/types/reporting'
import type { LeaveStatus } from '../../shared/types/leave'

export interface ReportFilters { from: string; to: string; status?: LeaveStatus }
const query = (filters: ReportFilters) => { const p = new URLSearchParams({ from: filters.from, to: filters.to }); if (filters.status) p.set('status', filters.status); return p }
export const getOrganizationLeaveRequests = (filters: ReportFilters, page = 0, size = 20) => api<OrganizationLeaveRequestPage>(`/admin/leave-requests?${query(filters)}&page=${page}&size=${size}`)
export const getLeaveSummary = (filters: Pick<ReportFilters, 'from' | 'to'>) => api<LeaveSummaryReport>(`/admin/reports/leave-summary?from=${encodeURIComponent(filters.from)}&to=${encodeURIComponent(filters.to)}`)
export const getAuditEvents = (page = 0, size = 20, entityType?: string, entityId?: string) => { const p = new URLSearchParams({ page: String(page), size: String(size) }); if (entityType) p.set('entityType', entityType); if (entityId) p.set('entityId', entityId); return api<AuditEventPage>(`/admin/audit-events?${p}`) }
