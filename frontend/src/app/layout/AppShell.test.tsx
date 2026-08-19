import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi, expect, it } from 'vitest'
vi.mock('../providers/AuthProvider',()=>({useAuth:()=>({principal:{displayName:'Employee',roles:['EMPLOYEE']},logout:vi.fn()})}))
import { AppShell } from './AppShell'
it('shows only phase-one employee navigation',()=>{render(<MemoryRouter><Routes><Route element={<AppShell/>}><Route index element={<p>Dashboard content</p>}/></Route></Routes></MemoryRouter>);expect(screen.getByRole('link',{name:'Request leave'})).toBeInTheDocument();expect(screen.queryByRole('link',{name:/manager/i})).not.toBeInTheDocument();expect(screen.queryByRole('link',{name:/admin/i})).not.toBeInTheDocument()})
