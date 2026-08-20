import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getManagerDashboard } from '../team-approvals/api'
import type { ManagerDashboard } from '../../shared/types/manager'
import { Card, DateDisplay, Empty, ErrorSummary, Loading, StatusBadge } from '../../shared/components/Ui'
export function ManagerDashboardPage(){const [data,setData]=useState<ManagerDashboard>();const [error,setError]=useState('');useEffect(()=>{getManagerDashboard().then(setData).catch(e=>setError(String(e)))},[]);if(error)return <ErrorSummary message={error}/>;if(!data)return <Loading/>;return <><h1>Manager dashboard</h1><div className="grid"><Card title="Pending approvals">{data.pendingRequests.length?data.pendingRequests.map(r=><p key={r.id}><Link to={`/manager/requests/${r.id}`}>{r.employeeName} · {r.leaveTypeName}</Link> <StatusBadge status={r.status}/></p>):<Empty/>}</Card><Card title="Approved upcoming leave">{data.approvedUpcomingLeave.length?data.approvedUpcomingLeave.map(r=><p key={r.id}>{r.employeeName} · <DateDisplay value={r.startDate}/></p>):<Empty/>}</Card></div></>}
