import { expect, test, type Page } from '@playwright/test'

type DemoRole = 'ADMINISTRATOR' | 'MANAGER' | 'EMPLOYEE'

type Principal = {
  userId: string
  employeeId: string
  displayName: string
  roles: DemoRole[]
}

const required = (name: string): string => {
  const value = process.env[name]
  if (!value) throw new Error(`Quickstart authentication verification requires ${name}`)
  return value
}

const actors: Array<{
  role: DemoRole
  login: string
  password: string
  forbiddenPath: string
}> = [
  {
    role: 'EMPLOYEE',
    login: required('E2E_EMPLOYEE_LOGIN'),
    password: required('E2E_EMPLOYEE_PASSWORD'),
    forbiddenPath: '/api/admin/employees',
  },
  {
    role: 'MANAGER',
    login: required('E2E_MANAGER_LOGIN'),
    password: required('E2E_MANAGER_PASSWORD'),
    forbiddenPath: '/api/admin/employees',
  },
  {
    role: 'ADMINISTRATOR',
    login: required('E2E_ADMIN_LOGIN'),
    password: required('E2E_ADMIN_PASSWORD'),
    forbiddenPath: '/api/manager/leave-requests',
  },
]

async function signIn(page: Page, login: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('Login').fill(login)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('navigation', { name: 'Leave navigation' })).toBeVisible()
}

test('demo roles expose only their assigned principal and retain backend boundaries', async ({ page }) => {
  const unauthenticated = await page.request.get('/api/auth/me')
  expect(unauthenticated.status()).toBe(401)
  expect(await unauthenticated.json()).toMatchObject({
    status: 401,
    code: 'AUTHENTICATION_REQUIRED',
  })

  for (const actor of actors) {
    await signIn(page, actor.login, actor.password)

    const me = await page.request.get('/api/auth/me')
    expect(me.status()).toBe(200)
    const principal = (await me.json()) as Principal
    expect(principal.userId).toBeTruthy()
    expect(principal.employeeId).toBeTruthy()
    expect(principal.displayName).toBeTruthy()
    expect(principal.roles).toEqual([actor.role])
    expect(JSON.stringify(principal)).not.toMatch(/password|credential|hash/i)

    const forbidden = await page.request.get(actor.forbiddenPath)
    expect(forbidden.status()).toBe(403)
    expect(await forbidden.json()).toMatchObject({
      status: 403,
      code: 'ACCESS_DENIED',
    })

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()

    const loggedOut = await page.request.get('/api/auth/me')
    expect(loggedOut.status()).toBe(401)
    expect(await loggedOut.json()).toMatchObject({
      status: 401,
      code: 'AUTHENTICATION_REQUIRED',
    })
  }
})
