import type { ConversationMode, WorkspacePageId } from '../types'
import type { LoomRouteState, UiRightPanelTab } from '../frontendTypes'

function normalizeTraceTab(value: string | null): UiRightPanelTab {
  return value === 'commands' ? 'commands' : 'tasks'
}

function normalizeMode(value: string | null): ConversationMode | null {
  if (value === 'chat' || value === 'plan' || value === 'action' || value === 'review') {
    return value
  }
  return null
}

function normalizePage(value: string): WorkspacePageId | null {
  if (
    value === 'conversation' ||
    value === 'capabilities' ||
    value === 'openclaw' ||
    value === 'files' ||
    value === 'memory' ||
    value === 'settings'
  ) {
    return value
  }
  return null
}

export function parseLoomLocation(
  pathname: string,
  search: string,
  fallbackProjectId: string,
  fallbackConversationId: string | null,
): LoomRouteState {
  const params = new URLSearchParams(search)
  const segments = pathname.split('/').filter(Boolean)

  if (segments.length === 0) {
    return {
      layout: 'app',
      page: 'conversation',
      projectId: fallbackProjectId,
      conversationId: fallbackConversationId,
      mode: normalizeMode(params.get('mode')),
      traceTab: normalizeTraceTab(params.get('traceTab')),
      settingsSection: null,
      callbackKind: null,
    }
  }

  if (segments[0] === 'welcome') {
    return {
      layout: 'app',
      page: 'conversation',
      projectId: fallbackProjectId,
      conversationId: fallbackConversationId,
      mode: normalizeMode(params.get('mode')),
      traceTab: normalizeTraceTab(params.get('traceTab')),
      settingsSection: null,
      callbackKind: null,
    }
  }

  if (segments[0] === 'callback') {
    return {
      layout: 'callback',
      page: 'callback',
      projectId: null,
      conversationId: null,
      mode: null,
      traceTab: 'tasks',
      settingsSection: null,
      callbackKind: segments[1] === 'feishu' ? 'feishu' : 'openclaw',
    }
  }

  if (segments[0] === 'app' && segments[1] === 'projects') {
    const projectId = segments[2] ?? fallbackProjectId
    if (!segments[3] || segments[3] === 'chat') {
      return {
        layout: 'app',
        page: 'conversation',
        projectId,
        conversationId: segments[4] ?? fallbackConversationId,
        mode: normalizeMode(params.get('mode')),
        traceTab: normalizeTraceTab(params.get('traceTab')),
        settingsSection: null,
        callbackKind: null,
      }
    }

    const page = normalizePage(segments[3]) ?? 'conversation'
    return {
      layout: 'app',
      page,
      projectId,
      conversationId: fallbackConversationId,
      mode: normalizeMode(params.get('mode')),
      traceTab: normalizeTraceTab(params.get('traceTab')),
      settingsSection: page === 'settings' ? segments[4] ?? 'Models' : null,
      callbackKind: null,
    }
  }

  return {
    layout: 'app',
    page: 'conversation',
    projectId: fallbackProjectId,
    conversationId: fallbackConversationId,
    mode: normalizeMode(params.get('mode')),
    traceTab: normalizeTraceTab(params.get('traceTab')),
    settingsSection: null,
    callbackKind: null,
  }
}

export function buildWorkspacePath(route: LoomRouteState): string {
  if (route.layout === 'callback') {
    return `/callback/${route.callbackKind ?? 'openclaw'}`
  }

  const projectId = route.projectId ?? 'project-loom'
  const params = new URLSearchParams()
  if (route.mode) {
    params.set('mode', route.mode)
  }
  if (route.traceTab) {
    params.set('traceTab', route.traceTab)
  }
  const suffix = params.toString() ? `?${params.toString()}` : ''

  switch (route.page) {
    case 'capabilities':
    case 'openclaw':
    case 'files':
    case 'memory':
      return `/app/projects/${projectId}/${route.page}${suffix}`
    case 'settings':
      return `/app/projects/${projectId}/settings/${route.settingsSection ?? 'Models'}${suffix}`
    case 'conversation':
    default:
      return `/app/projects/${projectId}/chat/${route.conversationId ?? ''}${suffix}`
  }
}
