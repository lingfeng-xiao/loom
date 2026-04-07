const rawBase = import.meta.env.VITE_API_BASE
const API_BASE = rawBase === undefined ? '' : rawBase

type RequestOptions = RequestInit & { timeoutMs?: number }

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs = 30000, headers, ...rest } = options
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), timeoutMs)

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...rest,
      headers: {
        'Content-Type': 'application/json',
        ...(headers ?? {}),
      },
      signal: controller.signal,
    })

    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`)
    }

    const text = await response.text()
    return text ? (JSON.parse(text) as T) : (undefined as T)
  } finally {
    window.clearTimeout(timeout)
  }
}

export const apiClient = {
  get: <T>(path: string, options: RequestOptions = {}) => request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
    request<T>(path, {
      ...options,
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  put: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
    request<T>(path, {
      ...options,
      method: 'PUT',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  patch: <T>(path: string, body?: unknown, options: RequestOptions = {}) =>
    request<T>(path, {
      ...options,
      method: 'PATCH',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
}
