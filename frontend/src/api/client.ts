export type ApiResponse<T> = {
  code: number
  data: T
  message: string
}

export const API_BASE_URL = '/api'

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    ...init,
  })

  const payload = await parseOptionalApiResponse<T>(response)

  if (!payload) {
    throw new Error(response.ok ? 'Invalid API response' : 'Request failed')
  }

  if (!response.ok || (payload.code !== 0 && payload.code !== 200)) {
    throw new Error(payload.message || 'Request failed')
  }

  return payload.data
}

export async function readErrorMessage(response: Response, fallback = 'Request failed') {
  const payload = await parseOptionalApiResponse<unknown>(response)
  return payload?.message || fallback
}

async function parseOptionalApiResponse<T>(response: Response): Promise<ApiResponse<T> | undefined> {
  const text = await response.text()
  if (!text) {
    return undefined
  }

  try {
    const payload = JSON.parse(text) as Partial<ApiResponse<T>>
    if (typeof payload.code !== 'number' || typeof payload.message !== 'string') {
      return undefined
    }
    return payload as ApiResponse<T>
  } catch {
    return undefined
  }
}
