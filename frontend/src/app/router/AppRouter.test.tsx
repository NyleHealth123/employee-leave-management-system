import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { server } from '../../test/setup'
import { AuthProvider } from '../providers/AuthProvider'
import { AppRouter } from './AppRouter'
import { beforeEach, expect, it } from 'vitest'
beforeEach(()=>{window.history.pushState({},'', '/employee');server.use(http.get('/api/auth/me',()=>HttpResponse.json({userId:crypto.randomUUID(),employeeId:crypto.randomUUID(),displayName:'Asha Employee',roles:['EMPLOYEE']})),http.get('/api/employee/dashboard',()=>HttpResponse.json({balances:[],pendingRequests:[],approvedUpcomingLeave:[],upcomingHolidays:[]})))})
it('renders authenticated employee navigation',async()=>{render(<AuthProvider><AppRouter/></AuthProvider>);expect(await screen.findByText('Asha Employee')).toBeInTheDocument();expect(screen.getByRole('link',{name:'Request leave'})).toBeInTheDocument()})

