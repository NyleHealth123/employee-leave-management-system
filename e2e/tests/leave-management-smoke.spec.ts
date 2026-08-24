import { expect, test, type Page } from '@playwright/test'

const required = (name: string): string => {
  const value = process.env[name]
  if (!value) throw new Error(`Playwright smoke test requires ${name}`)
  return value
}

const employee = { login: required('E2E_EMPLOYEE_LOGIN'), password: required('E2E_EMPLOYEE_PASSWORD') }
const manager = { login: required('E2E_MANAGER_LOGIN'), password: required('E2E_MANAGER_PASSWORD') }
const administrator = { login: required('E2E_ADMIN_LOGIN'), password: required('E2E_ADMIN_PASSWORD') }
const outOfScopeRequestId = required('E2E_OUT_OF_SCOPE_REQUEST_ID')
const approvalDate = process.env.E2E_APPROVAL_DATE ?? '2026-11-17'
const rejectionDate = process.env.E2E_REJECTION_DATE ?? '2026-11-19'

async function login(page: Page, credentials: { login: string; password: string }) {
  await page.goto('/login')
  await page.getByLabel('Login').fill(credentials.login)
  await page.getByLabel('Password').fill(credentials.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('navigation', { name: 'Leave navigation' })).toBeVisible()
}

async function logout(page: Page) {
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
}

async function annualBalance(page: Page) {
  await page.getByRole('link', { name: 'Balances' }).click()
  const card = page.getByRole('heading', { name: 'Annual Leave' }).locator('..')
  const text = await card.getByText(/days available/).textContent()
  const value = text?.match(/[\d.]+/)?.[0]
  if (!value) throw new Error('Annual leave balance was not displayed')
  return Number(value)
}

async function submitAnnualRequest(page: Page, date: string, reason: string) {
  await page.getByRole('link', { name: 'Request leave' }).click()
  await page.getByLabel('Leave type').selectOption({ label: 'Annual Leave' })
  await page.getByLabel('Start date').fill(date)
  await page.getByLabel('End date').fill(date)
  await page.getByLabel('Reason').fill(reason)
  await page.getByRole('button', { name: 'Calculate duration' }).click()
  await expect(page.getByRole('heading', { name: 'Authoritative calculation' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Submit request' })).toBeEnabled()
  await page.getByRole('button', { name: 'Submit request' }).click()
  await expect(page).toHaveURL(/\/employee\/requests\/[0-9a-f-]+$/)
  await expect(page.getByText('PENDING', { exact: true }).first()).toBeVisible()
  return page.url().split('/').at(-1)!
}

test('employee, manager, cancellation, balance, scope, and administrator audit smoke', async ({ page }) => {
  await login(page, employee)
  const startingBalance = await annualBalance(page)
  const approvalRequestId = await submitAnnualRequest(page, approvalDate, 'E2E request to approve')
  const rejectionRequestId = await submitAnnualRequest(page, rejectionDate, 'E2E request to reject')
  await logout(page)

  await login(page, manager)
  await page.getByRole('link', { name: 'Approvals' }).click()
  await expect(page.getByRole('table', { name: 'Pending direct-report requests' })).toBeVisible()
  const approvalLink = page.locator(`a[href="/manager/requests/${approvalRequestId}"]`)
  const rejectionLink = page.locator(`a[href="/manager/requests/${rejectionRequestId}"]`)
  await expect(approvalLink).toBeVisible()
  await expect(rejectionLink).toBeVisible()

  await approvalLink.click()
  await page.getByRole('button', { name: 'Approve' }).click()
  await expect(page.getByText('APPROVED', { exact: true }).first()).toBeVisible()

  await page.getByRole('link', { name: 'Approvals' }).click()
  await page.locator(`a[href="/manager/requests/${rejectionRequestId}"]`).click()
  await page.getByLabel('Decision comment').fill('E2E rejection verification')
  await page.getByRole('button', { name: 'Reject' }).click()
  await expect(page.getByText('REJECTED', { exact: true }).first()).toBeVisible()

  await page.goto(`/manager/requests/${outOfScopeRequestId}`)
  await expect(page.getByRole('alert')).toContainText('Leave request was not found')
  await logout(page)

  await login(page, employee)
  await page.goto(`/employee/requests/${approvalRequestId}`)
  await expect(page.getByText('APPROVED', { exact: true }).first()).toBeVisible()
  await expect(page.getByRole('status')).toContainText('eligible for cancellation')
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: 'Cancel request' }).click()
  await expect(page.getByText('CANCELLED', { exact: true }).first()).toBeVisible()
  expect(await annualBalance(page)).toBe(startingBalance)
  await logout(page)

  await login(page, administrator)
  await page.getByRole('link', { name: 'Audit history' }).click()
  await page.getByLabel('Entity type').fill('LEAVE_REQUEST')
  await page.getByLabel('Entity ID').fill(approvalRequestId)
  await page.getByRole('button', { name: 'Filter audit events' }).click()
  const auditTable = page.getByRole('table', { name: 'Immutable audit events' })
  await expect(auditTable).toContainText('LEAVE_SUBMITTED')
  await expect(auditTable).toContainText('LEAVE_APPROVED')
  await expect(auditTable).toContainText('LEAVE_CANCELLED')
})
