import { request, type FullConfig } from '@playwright/test'

type CsrfResponse = { token: string; headerName: string }
type ResetResponse = {
  employees: number
  administrators: number
  managers: number
  employeeAccounts: number
  managerRelationships: number
  requestStatuses: Record<string, number>
}

const required = (name: string): string => {
  const value = process.env[name]
  if (!value) throw new Error(`Local-demo E2E setup requires ${name}`)
  return value
}

export default async function globalSetup(_config: FullConfig) {
  const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080'
  const adminLogin = required('E2E_ADMIN_LOGIN')
  const adminPassword = required('E2E_ADMIN_PASSWORD')
  const context = await request.newContext({ baseURL: apiBaseUrl })

  try {
    const csrfResponse = await context.get('/api/auth/csrf')
    if (!csrfResponse.ok()) throw new Error(`Local-demo CSRF setup failed (${csrfResponse.status()})`)
    const csrf = await csrfResponse.json() as CsrfResponse
    const headers = { [csrf.headerName]: csrf.token }

    const loginResponse = await context.post('/api/auth/login', {
      headers,
      data: { login: adminLogin, password: adminPassword },
    })
    if (!loginResponse.ok()) throw new Error(`Local-demo administrator authentication failed (${loginResponse.status()})`)

    const resetResponse = await context.post('/api/admin/local-demo/reset', { headers })
    if (!resetResponse.ok()) throw new Error(`Local-demo reset was refused or failed (${resetResponse.status()})`)
    const result = await resetResponse.json() as ResetResponse
    const statuses = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED']
    if (result.employees < 50 || result.administrators < 1 || result.managers < 2 || result.employeeAccounts < 50
      || result.managerRelationships < 50 || statuses.some(status => !result.requestStatuses[status])) {
      throw new Error('Local-demo reset returned an incomplete authoritative dataset')
    }
  } finally {
    await context.dispose()
  }
}
