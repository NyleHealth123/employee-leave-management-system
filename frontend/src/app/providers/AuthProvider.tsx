import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, onSessionExpired } from '../../shared/api/apiClient'
import type { Principal } from '../../shared/types/leave'

interface AuthState { principal: Principal | null; loading: boolean; login(login: string, password: string): Promise<void>; logout(): Promise<void> }
const AuthContext = createContext<AuthState | null>(null)
export function AuthProvider({ children }: { children: ReactNode }) {
  const [principal, setPrincipal] = useState<Principal | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => { onSessionExpired(() => setPrincipal(null)); api<Principal>('/auth/me').then(setPrincipal).catch(() => setPrincipal(null)).finally(() => setLoading(false)) }, [])
  const value = useMemo<AuthState>(() => ({ principal, loading, async login(login, password) { setPrincipal(await api<Principal>('/auth/login', { method: 'POST', body: JSON.stringify({ login, password }) })) }, async logout() { await api<void>('/auth/logout', { method: 'POST' }); setPrincipal(null) } }), [principal, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider is required'); return value }

