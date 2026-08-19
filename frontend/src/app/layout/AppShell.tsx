import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../providers/AuthProvider'
export function AppShell(){const auth=useAuth();return <><header><strong>Leave Management</strong><nav aria-label="Employee navigation"><NavLink to="/employee">Dashboard</NavLink><NavLink to="/employee/request">Request leave</NavLink><NavLink to="/employee/requests">History</NavLink><NavLink to="/employee/balances">Balances</NavLink><NavLink to="/employee/holidays">Holidays</NavLink><NavLink to="/employee/calendar">Team calendar</NavLink></nav><span>{auth.principal?.displayName}</span><button onClick={()=>void auth.logout()}>Sign out</button></header><main><Outlet/></main></>}

