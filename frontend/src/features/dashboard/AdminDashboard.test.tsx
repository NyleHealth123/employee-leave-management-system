import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { expect, it } from 'vitest'
import { AdminDashboardPage } from './AdminDashboardPage'
it('provides administrator configuration navigation', () => { render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>); expect(screen.getByRole('link', { name: 'Employees and managers' })).toHaveAttribute('href', '/admin/employees'); expect(screen.getByRole('link', { name: 'Leave types and policies' })).toHaveAttribute('href', '/admin/policies'); expect(screen.getByRole('link', { name: 'Company holidays' })).toHaveAttribute('href', '/admin/holidays'); expect(screen.getByRole('link', { name: 'Employee balances' })).toHaveAttribute('href', '/admin/balances') })
