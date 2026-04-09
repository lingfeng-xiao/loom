import type {
  ApiEnvelope,
  CapabilityOverviewView,
  ContextPanelView,
  CreateConversationRequest,
  CreateProjectRequest,
  CursorPage,
  FileAssetSummary,
  ConversationListItem,
  ConversationView,
  LlmConnectionTestView,
  LoomBootstrapPayload,
  MemoryItemView,
  MessageView,
  ProjectListItem,
  ProjectView,
  SettingsOverviewView,
  SubmitMessageRequest,
  SubmitMessageResponse,
  UpdateConversationRequest,
  TracePanelView,
  UpdateLlmConfigRequest,
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

    let response: Response
    try {
      response = await fetch(`${this.options.baseUrl}${path}`, {
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
    } catch (error) {
      throw new LoomApiError('NETWORK_ERROR', `无法连接到 Loom API（${path}）。请确认后端服务已启动且可以访问。`, error)
    }

    let json: ApiEnvelope<T> | ApiErrorEnvelope
    try {
      json = (await response.json()) as ApiEnvelope<T> | ApiErrorEnvelope
    } catch (error) {
      throw new LoomApiError(
        'INVALID_RESPONSE',
        `Loom API 在 ${path} 返回了无法识别的响应。请确认该接口返回的是标准 JSON。`,
        error,
      )
    }

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

  patch<T>(path: string, body?: unknown, signal?: AbortSignal) {
    return this.request<T>(path, 'PATCH', body, signal)
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

  listProjects(signal?: AbortSignal) {
    return this.http.get<CursorPage<ProjectListItem>>('/api/projects', signal)
  }

  createProject(request?: CreateProjectRequest, signal?: AbortSignal) {
    return this.http.post<ProjectView>('/api/projects', request, signal)
  }

  getProject(projectId: string, signal?: AbortSignal) {
    return this.http.get<ProjectView>(`/api/projects/${projectId}`, signal)
  }

  listConversations(projectId: string, signal?: AbortSignal) {
    return this.http.get<CursorPage<ConversationListItem>>(`/api/projects/${projectId}/conversations`, signal)
  }

  createConversation(projectId: string, request?: CreateConversationRequest, signal?: AbortSignal) {
    return this.http.post<ConversationView>(`/api/projects/${projectId}/conversations`, request, signal)
  }

  getConversation(projectId: string, conversationId: string, signal?: AbortSignal) {
    return this.http.get<ConversationView>(`/api/projects/${projectId}/conversations/${conversationId}`, signal)
  }

  updateConversation(projectId: string, conversationId: string, request: UpdateConversationRequest, signal?: AbortSignal) {
    return this.http.patch<ConversationView>(`/api/projects/${projectId}/conversations/${conversationId}`, request, signal)
  }

  listMessages(projectId: string, conversationId: string, signal?: AbortSignal) {
    return this.http.get<CursorPage<MessageView>>(`/api/projects/${projectId}/conversations/${conversationId}/messages`, signal)
  }

  getContext(projectId: string, conversationId: string, signal?: AbortSignal) {
    return this.http.get<ContextPanelView>(`/api/projects/${projectId}/conversations/${conversationId}/context`, signal)
  }

  getTrace(projectId: string, conversationId: string, signal?: AbortSignal) {
    return this.http.get<TracePanelView>(`/api/projects/${projectId}/conversations/${conversationId}/trace`, signal)
  }

  getSettingsOverview(scope = 'project', signal?: AbortSignal) {
    return this.http.get<SettingsOverviewView>(`/api/settings/overview?scope=${encodeURIComponent(scope)}`, signal)
  }

  updateLlmSettings(request: UpdateLlmConfigRequest, signal?: AbortSignal) {
    return this.http.post<SettingsOverviewView>('/api/settings/llm', request, signal)
  }

  testLlmSettings(request: UpdateLlmConfigRequest, signal?: AbortSignal) {
    return this.http.post<LlmConnectionTestView>('/api/settings/llm/test', request, signal)
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
