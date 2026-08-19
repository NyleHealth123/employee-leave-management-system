import { api } from '../../shared/api/apiClient'
import type { EmployeeDashboard } from '../../shared/types/leave'
export const getDashboard = () => api<EmployeeDashboard>('/employee/dashboard')

