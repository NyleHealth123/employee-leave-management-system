import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { AppShell } from '../app/layout/AppShell'
import { AdminCorrectionDialog } from '../features/admin/corrections/AdminCorrectionDialog'
import { EmployeeTeamCalendarPage } from '../features/calendar/EmployeeTeamCalendarPage'
import { LeaveRequestFormPage } from '../features/leave-requests/LeaveRequestFormPage'
import { Card, DataTable, StatusBadge } from '../shared/components/Ui'
import type { LeaveRequestDetail } from '../shared/types/leave'
import { server } from './setup'

const auth = vi.hoisted(() => ({
  logout: vi.fn(),
  principal: {
    displayName: 'Multi Role User',
    roles: ['EMPLOYEE', 'MANAGER', 'ADMINISTRATOR'],
  },
}))

vi.mock('../app/providers/AuthProvider', () => ({ useAuth: () => auth }))

beforeEach(() => {
  auth.logout.mockReset()
  window.innerWidth = 1024
})

it('supports keyboard navigation across every assigned role', async () => {
  const user = userEvent.setup()
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<p>Employee home</p>} />
          <Route path="manager" element={<p>Manager home</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )

  const navigation = screen.getByRole('navigation', { name: 'Leave navigation' })
  expect(within(navigation).getByRole('link', { name: 'Request leave' })).toBeVisible()
  expect(within(navigation).getByRole('link', { name: 'Manager dashboard' })).toBeVisible()
  expect(within(navigation).getByRole('link', { name: 'Administrator' })).toBeVisible()

  for (let index = 0; index < 7; index += 1) await user.tab()
  expect(screen.getByRole('link', { name: 'Manager dashboard' })).toHaveFocus()
  await user.keyboard('{Enter}')
  expect(screen.getByText('Manager home')).toBeVisible()
})

it('keeps the approved leave form labeled, required, and keyboard operable', async () => {
  const leaveTypeId = crypto.randomUUID()
  server.use(
    http.get('/api/leave-types', () => HttpResponse.json([{ id: leaveTypeId, code: 'ANNUAL', name: 'Annual leave', tracksBalance: true, allowsHalfDay: true, cancellationCutoffDays: 1 }])),
    http.get('/api/employee/leave-balances', () => HttpResponse.json([])),
    http.get('/api/auth/csrf', () => HttpResponse.json({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })),
    http.post('/api/employee/leave-requests/calculate', () => HttpResponse.json({ chargeableDays: 1, chargeableDates: ['2026-09-01'], excludedDates: [], tracksBalance: true, availableDays: 10, canSubmit: true, messages: [] })),
  )
  const user = userEvent.setup()
  render(<LeaveRequestFormPage />)

  await screen.findByRole('option', { name: 'Annual leave' })
  expect(screen.getByLabelText('Start date')).toBeRequired()
  expect(screen.getByLabelText('End date')).toBeRequired()
  expect(screen.getByLabelText('Reason')).toBeRequired()
  await user.type(screen.getByLabelText('Start date'), '2026-09-01')
  await user.type(screen.getByLabelText('End date'), '2026-09-01')
  await user.type(screen.getByLabelText('Reason'), 'Rest')
  screen.getByRole('button', { name: 'Calculate duration' }).focus()
  await user.keyboard('{Enter}')
  expect(await screen.findByText('1 days')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Submit request' })).toBeEnabled()
})

it('moves focus into the correction dialog and supports keyboard submission', async () => {
  const request: LeaveRequestDetail = {
    id: 'request-1', employeeId: 'employee-1', employeeName: 'Employee', leaveTypeId: 'type-1', leaveTypeName: 'Annual leave',
    startDate: '2026-09-01', endDate: '2026-09-01', durationMode: 'FULL_DAY', chargeableDays: 1, status: 'PENDING',
    submittedAt: '2026-08-20T00:00:00Z', version: 2, reason: 'Rest', decisionComment: null, canCancel: false,
    cancellationBlockedReason: null, statusHistory: [],
  }
  const updated = vi.fn()
  server.use(
    http.get('/api/auth/csrf', () => HttpResponse.json({ token: 'csrf', headerName: 'X-XSRF-TOKEN' })),
    http.post('/api/admin/leave-requests/request-1/corrections', () => HttpResponse.json({ ...request, status: 'CANCELLED', version: 3 })),
  )
  const user = userEvent.setup()
  render(<AdminCorrectionDialog request={request} onUpdated={updated} />)

  const dialog = screen.getByRole('dialog', { name: 'Administrator correction' })
  const reason = within(dialog).getByLabelText('Reason')
  expect(reason).toHaveFocus()
  await user.type(reason, 'Policy correction')
  await user.tab()
  expect(within(dialog).getByRole('button', { name: 'Apply correction' })).toHaveFocus()
  await user.keyboard('{Enter}')
  expect(updated).toHaveBeenCalledWith(expect.objectContaining({ status: 'CANCELLED', version: 3 }))
})

it('retains table, card, and textual status semantics at a narrow viewport', () => {
  const { container } = render(
    <>
      <DataTable label="Responsive requests">
        <thead><tr><th scope="col">Request</th><th scope="col">Status</th></tr></thead>
        <tbody><tr><td>Annual leave</td><td><StatusBadge status="PENDING" /></td></tr></tbody>
      </DataTable>
      <div className="grid"><Card title="Balance"><StatusBadge status="APPROVED" /></Card></div>
    </>,
  )

  window.innerWidth = 480
  window.dispatchEvent(new Event('resize'))
  expect(screen.getByRole('table', { name: 'Responsive requests' })).toBeVisible()
  expect(container.querySelector('.table-wrap')).toBeInTheDocument()
  expect(screen.getByRole('heading', { name: 'Balance' })).toBeVisible()
  expect(screen.getByText('PENDING')).toBeVisible()
  expect(screen.getByText('APPROVED')).toBeVisible()
})

it('renders the employee calendar as a responsive privacy-safe agenda with status text', async () => {
  server.use(http.get('/api/employee/team-calendar', () => HttpResponse.json([{ employeeDisplayName: 'Alex Employee', startDate: '2026-09-02', endDate: '2026-09-03', status: 'APPROVED' }])))
  window.innerWidth = 480
  render(<EmployeeTeamCalendarPage />)

  expect(await screen.findByRole('heading', { name: 'Alex Employee' })).toBeVisible()
  expect(screen.getByText('APPROVED')).toBeVisible()
  expect(screen.getByText(/Only names, leave dates, and status/)).toBeVisible()
  expect(document.querySelector('article.card')).toBeInTheDocument()
})
