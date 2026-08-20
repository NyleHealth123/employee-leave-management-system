import { api } from '../../shared/api/apiClient'
import type { LeaveBalance, LeaveCalculation, LeaveRequestDetail, LeaveRequestInput, LeaveRequestSummary, LeaveType, Page, TeamCalendarEntry, Holiday } from '../../shared/types/leave'
export const getLeaveTypes = () => api<LeaveType[]>('/leave-types')
export const getBalances = () => api<LeaveBalance[]>('/employee/leave-balances')
export const getHolidays = (from: string, to: string) => api<Holiday[]>(`/holidays?from=${from}&to=${to}`)
export const calculateLeave = (input: LeaveRequestInput) => api<LeaveCalculation>('/employee/leave-requests/calculate', { method: 'POST', body: JSON.stringify(input) })
export const submitLeave = (input: LeaveRequestInput, key: string) => api<LeaveRequestDetail>('/employee/leave-requests', { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(input) })
export const getRequests = (page = 0) => api<Page<LeaveRequestSummary>>(`/employee/leave-requests?page=${page}&size=20`)
export const getRequest = (id: string) => api<LeaveRequestDetail>(`/employee/leave-requests/${id}`)
export const cancelRequest = (id: string, expectedVersion: number, comment = '') => api<LeaveRequestDetail>(`/employee/leave-requests/${id}/cancel`, { method: 'POST', body: JSON.stringify({ expectedVersion, comment }) })
export const getTeamCalendar = (from: string, to: string) => api<TeamCalendarEntry[]>(`/employee/team-calendar?from=${from}&to=${to}`)
