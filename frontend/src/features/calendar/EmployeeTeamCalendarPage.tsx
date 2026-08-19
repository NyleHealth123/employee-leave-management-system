import { useEffect, useState } from 'react'
import { getTeamCalendar } from '../leave-requests/api'
import type { TeamCalendarEntry } from '../../shared/types/leave'
import { DateDisplay, Empty, ErrorSummary, Loading, StatusBadge } from '../../shared/components/Ui'
export function EmployeeTeamCalendarPage(){const [entries,setEntries]=useState<TeamCalendarEntry[]>();const [error,setError]=useState('');useEffect(()=>{const from=new Date();from.setDate(1);const to=new Date(from);to.setMonth(to.getMonth()+3);getTeamCalendar(from.toISOString().slice(0,10),to.toISOString().slice(0,10)).then(setEntries).catch(e=>setError(String(e)))},[]);if(error)return <ErrorSummary message={error}/>;if(!entries)return <Loading/>;return <><h1>Team leave calendar</h1><p>Only names, leave dates, and status are shown.</p>{entries.length?<div className="grid">{entries.map((e,i)=><article className="card" key={`${e.employeeDisplayName}-${e.startDate}-${i}`}><h2>{e.employeeDisplayName}</h2><p><DateDisplay value={e.startDate}/> – <DateDisplay value={e.endDate}/></p><StatusBadge status={e.status}/></article>)}</div>:<Empty/>}</>}

