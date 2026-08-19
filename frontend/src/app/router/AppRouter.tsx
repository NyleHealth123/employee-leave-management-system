import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'
import { AppShell } from '../layout/AppShell'
import { Loading } from '../../shared/components/Ui'
import { LoginPage } from '../../features/auth/LoginPage'
import { EmployeeDashboardPage } from '../../features/dashboard/EmployeeDashboardPage'
import { LeaveRequestFormPage } from '../../features/leave-requests/LeaveRequestFormPage'
import { LeaveRequestHistoryPage, LeaveRequestDetailPage } from '../../features/leave-requests/LeaveRequestHistoryPage'
import { EmployeeTeamCalendarPage } from '../../features/calendar/EmployeeTeamCalendarPage'
import { BalancePage, HolidayPage } from '../../features/leave-requests/ReferencePages'
function Protected(){const auth=useAuth();const location=useLocation();if(auth.loading)return <Loading/>;if(!auth.principal)return <Navigate to="/login" state={{from:location}} replace/>;if(!auth.principal.roles.includes('EMPLOYEE'))return <p role="alert">Employee access is required.</p>;return <AppShell/>}
export function AppRouter(){return <BrowserRouter><Routes><Route path="/login" element={<LoginPage/>}/><Route element={<Protected/>}><Route path="/employee" element={<EmployeeDashboardPage/>}/><Route path="/employee/request" element={<LeaveRequestFormPage/>}/><Route path="/employee/requests" element={<LeaveRequestHistoryPage/>}/><Route path="/employee/requests/:id" element={<LeaveRequestDetailPage/>}/><Route path="/employee/balances" element={<BalancePage/>}/><Route path="/employee/holidays" element={<HolidayPage/>}/><Route path="/employee/calendar" element={<EmployeeTeamCalendarPage/>}/></Route><Route path="*" element={<Navigate to="/employee" replace/>}/></Routes></BrowserRouter>}

