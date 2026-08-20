import { api } from '../../shared/api/apiClient'
import type { ManagerDashboard, ManagerRequestDetail, ManagerRequestPage } from '../../shared/types/manager'
import type { LeaveRequestDetail, ManagerCalendarEntry } from '../../shared/types/leave'
export const getManagerDashboard = () => api<ManagerDashboard>('/manager/dashboard')
export const getManagerRequests = (page = 0, status = 'PENDING') => api<ManagerRequestPage>(`/manager/leave-requests?page=${page}&size=20&status=${status}`)
export const getManagerRequest = (id: string) => api<ManagerRequestDetail>(`/manager/leave-requests/${id}`)
export const approveManagerRequest = (id: string, expectedVersion: number, comment = '') => api<LeaveRequestDetail>(`/manager/leave-requests/${id}/approve`, { method: 'POST', body: JSON.stringify({ expectedVersion, comment }) })
export const rejectManagerRequest = (id: string, expectedVersion: number, comment: string) => api<LeaveRequestDetail>(`/manager/leave-requests/${id}/reject`, { method: 'POST', body: JSON.stringify({ expectedVersion, comment }) })
export const getManagerCalendar = (from: string, to: string) => api<ManagerCalendarEntry[]>(`/manager/team-calendar?from=${from}&to=${to}`)
