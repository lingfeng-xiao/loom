import type {
  ApiEnvelope,
  CapabilityOverviewView,
  ContextPanelView,
  CursorPage,
  FileAssetSummary,
  LoomBootstrapPayload,
  MemoryItemView,
  SettingsOverviewView,
  SubmitMessageRequest,
  SubmitMessageResponse,
} from '../types'

export type HttpMethod = 'GET' | 'POST' | 'PATCH' | 'DELETE'

export interface ApiErrorEnvelope {
  data: null
  error: {
    code: string
    message: string
    details?: unknown
  }
  meta?: {
    requestId?: string
    timestamp?: string
  }
}

export class LoomApiError extends Error {
  public readonly code: string
  public readonly details?: unknown

  constructor(code: string, message: string, details?: unknown) {
    super(message)
    this.name = 'LoomApiError'
    this.code = code
    this.details = details
  }
}

export interface HttpClientOptions {
  baseUrl: string
  getAccessToken?: () => string | null
  getProjectId?: () => string | null
}

export class LoomHttpClient {
  constructor(private readonly options: HttpClientOptions) {}

  async request<T>(
    path: string,
    method: HttpMethod,
    body?: unknown,
    signal?: AbortSignal,
    extraHeaders?: Record<string, string>,
  ): Promise<T> {
    const token = this.options.getAccessToken?.()
    const projectId = this.options.getProjectId?.()

    const response = await fetch(`${this.options.baseUrl}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(projectId ? { 'X-Project-Id': projectId } : {}),
        ...(extraHeaders ?? {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal,
    })

    const json = (await response.json()) as ApiEnvelope<T> | ApiErrorEnvelope
    if (!response.ok || ('error' in json && json.error)) {
      const err = (json as ApiErrorEnvelope).error
      throw new LoomApiError(err.code, err.message, err.details)
    }

    return (json as ApiEnvelope<T>).data
  }

  get<T>(path: string, signal?: AbortSignal) {
    return this.request<T>(path, 'GET', undefined, signal)
  }

  post<T>(path: string, body?: unknown, signal?: AbortSignal) {
    return this.request<T>(path, 'POST', body, signal)
  }
}

export class ShellApi {
  constructor(private readonly http: LoomHttpClient) {}

  bootstrap(signal?: AbortSignal) {
    return this.http.get<LoomBootstrapPayload>('/api/bootstrap', signal)
  }
}

export class WorkspaceApi {
  constructor(private readonly http: LoomHttpClient) {}

  getContext(projectId: string, conversationId: string, signal?: AbortSignal) {
    return this.http.get<ContextPanelView>(`/api/projects/${projectId}/conversations/${conversationId}/context`, signal)
  }

  getSettingsOverview(scope = 'project', signal?: AbortSignal) {
    return this.http.get<SettingsOverviewView>(`/api/settings/overview?scope=${encodeURIComponent(scope)}`, signal)
  }

  getCapabilitiesOverview(scope = 'project', signal?: AbortSignal) {
    return this.http.get<CapabilityOverviewView>(`/api/capabilities/overview?scope=${encodeURIComponent(scope)}`, signal)
  }

  getFiles(projectId: string, signal?: AbortSignal) {
    return this.http.get<CursorPage<FileAssetSummary>>(`/api/projects/${projectId}/files`, signal)
  }

  getMemory(projectId: string, signal?: AbortSignal) {
    return this.http.get<CursorPage<MemoryItemView>>(`/api/projects/${projectId}/memory`, signal)
  }

  submitMessage(projectId: string, conversationId: string, request: SubmitMessageRequest, signal?: AbortSignal) {
    return this.http.post<SubmitMessageResponse>(
      `/api/projects/${projectId}/conversations/${conversationId}/messages`,
      request,
      signal,
    )
  }
}

export function createLoomSdk(options: HttpClientOptions) {
  const http = new LoomHttpClient(options)
  return {
    shell: new ShellApi(http),
    workspace: new WorkspaceApi(http),
  }
}
