import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'
import { server } from '../../test/setup'
import { AdminLeaveReportsPage } from './AdminLeaveReportsPage'
import { AdminAuditHistoryPage } from './AdminAuditHistoryPage'

const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
beforeEach(() => { server.use(http.get('/api/admin/leave-requests', () => HttpResponse.json(page)), http.get('/api/admin/reports/leave-summary', () => HttpResponse.json({ from: '2026-01-01', to: '2026-01-31', byStatus: [{ key: 'APPROVED', requestCount: 1, chargeableDays: 1 }], byLeaveType: [] })), http.get('/api/admin/audit-events', () => HttpResponse.json(page))) })

it('validates period and renders empty reports safely', async () => { render(<AdminLeaveReportsPage />); expect(await screen.findByText('No organization requests match these filters.')).toBeInTheDocument(); expect(screen.getByText('No leave types in this period.')).toBeInTheDocument(); const from = screen.getByLabelText('From'); await userEvent.clear(from); await userEvent.type(from, '2026-12-31'); expect(await screen.findByText('Choose a valid reporting period.')).toBeInTheDocument() })
it('supports administrator request filters and keyboard pagination controls', async () => { render(<AdminLeaveReportsPage />); await screen.findByText('No organization requests match these filters.'); const status = screen.getByLabelText('Status'); await userEvent.selectOptions(status, 'APPROVED'); expect(status).toHaveValue('APPROVED'); expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled() })
it('renders immutable audit browser filters and empty state', async () => { render(<AdminAuditHistoryPage />); expect(await screen.findByText('No audit events match these filters.')).toBeInTheDocument(); expect(screen.getByRole('table', { name: 'Immutable audit events' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Filter audit events' })).toBeInTheDocument() })
