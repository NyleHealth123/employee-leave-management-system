import { ApiError, type ApiProblem } from './problem'

let csrfToken: string | undefined
let expiredSessionHandler: (() => void) | undefined
export function onSessionExpired(handler: () => void) { expiredSessionHandler = handler }
export function resetApiClientForTests() { csrfToken = undefined; expiredSessionHandler = undefined }

async function ensureCsrf() {
  if (csrfToken) return csrfToken
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  if (!response.ok) throw await problem(response)
  const body = await response.json() as { token: string }
  csrfToken = body.token
  return csrfToken
}

async function problem(response: Response) {
  let body: ApiProblem
  try { body = await response.json() as ApiProblem } catch { body = { type: 'about:blank', title: response.statusText, status: response.status, code: 'REQUEST_FAILED', detail: 'The request could not be completed', correlationId: response.headers.get('X-Correlation-ID') ?? '' } }
  return new ApiError(body)
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers.set('X-XSRF-TOKEN', await ensureCsrf())
  const response = await fetch(`/api${path}`, { ...init, headers, credentials: 'include' })
  if (response.status === 401) expiredSessionHandler?.()
  if (!response.ok) throw await problem(response)
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

