/**
 * fetch para o nexo-server (BFF): cookie de sessão sempre junto, `X-XSRF-TOKEN` nas rotas que
 * alteram estado e erros em Problem Details (RFC 9457).
 */

export interface Problem {
  type?: string
  title?: string
  status: number
  detail?: string
}

export class ApiError extends Error {
  constructor(readonly problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`)
  }

  get status(): number {
    return this.problem.status
  }
}

type UnauthorizedListener = () => void

const listeners = new Set<UnauthorizedListener>()

/**
 * Sessão caiu (401): o app volta para o login.
 */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  listeners.add(listener)

  return () => listeners.delete(listener)
}

export function csrfToken(cookies: string = document.cookie): string | undefined {
  const match = cookies.split(';').map((part) => part.trim()).find((part) => part.startsWith('XSRF-TOKEN='))

  return match ? decodeURIComponent(match.slice('XSRF-TOKEN='.length)) : undefined
}

export async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = csrfToken()

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (method !== 'GET' && token) {
    headers['X-XSRF-TOKEN'] = token
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401) {
    listeners.forEach((listener) => listener())
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Partial<Problem>

    throw new ApiError({ ...problem, status: response.status })
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}

/**
 * O primeiro GET já devolve o cookie XSRF-TOKEN; um POST antes disso precisa dele.
 */
export async function ensureCsrf(): Promise<void> {
  if (!csrfToken()) {
    await fetch('/api/me', { credentials: 'include' }).catch(() => undefined)
  }
}
