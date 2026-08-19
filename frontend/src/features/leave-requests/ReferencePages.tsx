import { useEffect, useState } from 'react'
import { getBalances, getHolidays } from './api'
import type { Holiday, LeaveBalance } from '../../shared/types/leave'
import { Card, DateDisplay, Empty, Loading } from '../../shared/components/Ui'
export function BalancePage(){const [items,setItems]=useState<LeaveBalance[]>();useEffect(()=>{getBalances().then(setItems)},[]);if(!items)return <Loading/>;return <><h1>Leave balances</h1><div className="grid">{items.map(b=><Card key={b.id} title={b.leaveTypeName}><p>{b.availableDays} days available</p><p>{b.reservedDays} reserved · {b.consumedDays} consumed</p></Card>)}</div></>}
export function HolidayPage(){const [items,setItems]=useState<Holiday[]>();useEffect(()=>{const from=new Date().toISOString().slice(0,10);const end=new Date();end.setFullYear(end.getFullYear()+1);getHolidays(from,end.toISOString().slice(0,10)).then(setItems)},[]);if(!items)return <Loading/>;return <><h1>Company holidays</h1>{items.length?<ul>{items.map(h=><li key={h.id}><DateDisplay value={h.date}/> — {h.name}</li>)}</ul>:<Empty/>}</>}

