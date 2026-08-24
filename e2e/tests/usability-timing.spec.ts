import { expect, test, type Page, type TestInfo } from '@playwright/test'

const EMPLOYEE_THRESHOLD_MS = 3 * 60 * 1000
const MANAGER_THRESHOLD_MS = 2 * 60 * 1000

const required = (name: string): string => {
  const value = process.env[name]
  if (!value) throw new Error(`T128 timing verification requires ${name}`)
  return value
}

const employee = {
  login: required('E2E_EMPLOYEE_LOGIN'),
  password: required('E2E_EMPLOYEE_PASSWORD'),
}
const manager = {
  login: required('E2E_MANAGER_LOGIN'),
  password: required('E2E_MANAGER_PASSWORD'),
}
const requestDate = process.env.E2E_USABILITY_REQUEST_DATE ?? '2026-12-15'

async function signIn(page: Page, credentials: { login: string; password: string }) {
  await page.goto('/login')
  await page.getByLabel('Login').fill(credentials.login)
  await page.getByLabel('Password').fill(credentials.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('navigation', { name: 'Leave navigation' })).toBeVisible()
}

async function attachTimings(testInfo: TestInfo, employeeMs: number, managerMs: number) {
  await testInfo.attach('t128-system-timings.json', {
    body: JSON.stringify({
      sc001EmployeeWorkflowMs: employeeMs,
      sc001ThresholdMs: EMPLOYEE_THRESHOLD_MS,
      sc005ManagerWorkflowMs: managerMs,
      sc005ThresholdMs: MANAGER_THRESHOLD_MS,
    }, null, 2),
    contentType: 'application/json',
  })
}

test('T128 real-stack employee and manager workflow timing guard', async ({ page }, testInfo) => {
  await signIn(page, employee)

  const employeeStartedAt = performance.now()
  await page.getByRole('link', { name: 'Request leave' }).click()
  await expect(page.getByRole('heading', { name: 'Request leave' })).toBeVisible()
  await page.getByLabel('Leave type').selectOption({ label: 'Annual Leave' })
  await page.getByLabel('Start date').fill(requestDate)
  await page.getByLabel('End date').fill(requestDate)
  await page.getByLabel('Reason').fill('T128 timed usability verification')
  await page.getByRole('button', { name: 'Calculate duration' }).click()
  await expect(page.getByRole('heading', { name: 'Authoritative calculation' })).toBeVisible()
  await expect(page.getByText('1 days', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Submit request' })).toBeEnabled()
  await page.getByRole('button', { name: 'Submit request' }).click()
  await expect(page).toHaveURL(/\/employee\/requests\/[0-9a-f-]+$/)
  await expect(page.getByText('PENDING', { exact: true }).first()).toBeVisible()
  await expect(page.getByText(/1 days/)).toBeVisible()
  const employeeElapsedMs = Math.round(performance.now() - employeeStartedAt)
  expect(employeeElapsedMs, 'SC-001 system workflow must remain under three minutes').toBeLessThan(EMPLOYEE_THRESHOLD_MS)
  const requestId = page.url().split('/').at(-1)!

  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  await signIn(page, manager)

  const managerStartedAt = performance.now()
  await page.getByRole('link', { name: 'Approvals' }).click()
  await expect(page.getByRole('table', { name: 'Pending direct-report requests' })).toBeVisible()
  const requestLink = page.locator(`a[href="/manager/requests/${requestId}"]`)
  await expect(requestLink).toBeVisible()
  await requestLink.click()
  await expect(page.getByRole('heading', { name: /Demo Employee 01/ })).toBeVisible()
  await page.getByRole('button', { name: 'Approve' }).click()
  await expect(page.getByText('APPROVED', { exact: true }).first()).toBeVisible()
  const managerElapsedMs = Math.round(performance.now() - managerStartedAt)
  expect(managerElapsedMs, 'SC-005 system workflow must remain under two minutes').toBeLessThan(MANAGER_THRESHOLD_MS)

  console.log(`T128_SC001_EMPLOYEE_WORKFLOW_MS=${employeeElapsedMs}`)
  console.log(`T128_SC005_MANAGER_WORKFLOW_MS=${managerElapsedMs}`)
  await attachTimings(testInfo, employeeElapsedMs, managerElapsedMs)
})
