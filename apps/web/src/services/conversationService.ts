import type { LoomRouteState } from '../app/routeTypes'
import type {
  ConversationThreadViewModel,
  SidebarPrimaryAction,
  SidebarSystemEntry,
  SidebarThreadGroup,
} from '../components/shell/shellTypes'
import type { ConversationDomainState } from '../domains/workbenchTypes'
import type { ConversationMode, ConversationSummary, LoomBootstrapPayload, WorkspacePageId } from '../types'

function findConversation(payload: LoomBootstrapPayload, conversationId: string | null): ConversationSummary | null {
  if (!conversationId) {
    return null
  }

  return [...payload.pinnedConversations, ...payload.recentConversations].find((item) => item.id === conversationId) ?? null
}

function mergeProjectConversations(payload: LoomBootstrapPayload): ConversationSummary[] {
  const seen = new Set<string>()
  const ordered = [...payload.pinnedConversations, ...payload.recentConversations]

  return ordered.filter((item) => {
    if (seen.has(item.id)) {
      return false
    }

    seen.add(item.id)
    return true
  })
}

function toThreadViewModel(items: ConversationSummary[], activeConversationId: string | null, group: string): ConversationThreadViewModel[] {
  return items.map((item) => ({
    ...item,
    group,
    lastActiveAt: item.lastUpdatedLabel,
    selected: item.id === activeConversationId,
  }))
}

function buildPrimaryActions(payload: LoomBootstrapPayload, page: WorkspacePageId, activeConversationId: string | null): SidebarPrimaryAction[] {
  const defaultConversationId = payload.pinnedConversations[0]?.id ?? payload.recentConversations[0]?.id ?? null
  const projectStatusId = payload.recentConversations[2]?.id ?? payload.recentConversations[0]?.id ?? defaultConversationId

  return [
    {
      id: 'new-thread',
      label: '新会话',
      hint: '开始新的协作会话',
      icon: 'compose',
      targetPage: 'conversation',
      active: page === 'conversation' && activeConversationId === defaultConversationId,
    },
    {
      id: 'capabilities',
      label: '技能和应用',
      hint: '模型、MCP、Skills',
      icon: 'spark',
      targetPage: 'capabilities',
      active: page === 'capabilities',
    },
    {
      id: 'automation',
      label: '自动化',
      hint: '执行器与外部路由',
      icon: 'automation',
      targetPage: 'openclaw',
      active: page === 'openclaw',
    },
    {
      id: 'analysis',
      label: '项目状态',
      hint: '当前进度与关键判断',
      icon: 'pulse',
      targetPage: 'conversation',
      active: page === 'conversation' && activeConversationId === projectStatusId,
    },
  ]
}

function buildSystemEntries(payload: LoomBootstrapPayload, page: WorkspacePageId): SidebarSystemEntry[] {
  return [
    {
      id: 'files',
      label: 'Files',
      meta: '项目文件池',
      icon: 'files',
      active: page === 'files',
    },
    {
      id: 'memory',
      label: 'Memory',
      meta: '分层长期记忆',
      icon: 'memory',
      active: page === 'memory',
    },
    {
      id: 'settings',
      label: '设置',
      meta: `main · ${payload.project.openClawStatus}`,
      icon: 'settings',
      active: page === 'settings',
    },
  ]
}

function buildSubbarMeta(payload: LoomBootstrapPayload, activeConversation: ConversationSummary | null, activeMode: ConversationMode): string {
  const base = activeConversation?.summary ?? payload.conversationMeta
  return `${base} · ${activeMode.toUpperCase()}`
}

export function buildConversationState(payload: LoomBootstrapPayload, route: LoomRouteState): ConversationDomainState {
  const currentPage = route.page === 'welcome' || route.page === 'callback' ? 'conversation' : route.page
  const defaultConversationId = payload.pinnedConversations[0]?.id ?? payload.recentConversations[0]?.id ?? null
  const activeConversationId = route.conversationId ?? defaultConversationId
  const activeConversation = findConversation(payload, activeConversationId)
  const activeMode = route.mode ?? activeConversation?.mode ?? payload.activeMode
  const mergedConversations = mergeProjectConversations(payload)

  const threadGroups: SidebarThreadGroup[] = [
    {
      id: payload.project.id,
      label: payload.project.name,
      threads: toThreadViewModel(mergedConversations, activeConversationId, payload.project.name),
    },
  ]

  return {
    pages: payload.pages,
    currentPage,
    activeConversationId,
    activeThread:
      activeConversation == null
        ? null
        : {
            ...activeConversation,
            group: payload.project.name,
            lastActiveAt: activeConversation.lastUpdatedLabel,
            selected: true,
          },
    activeMode,
    primaryActions: buildPrimaryActions(payload, currentPage, activeConversationId),
    threadGroups,
    systemEntries: buildSystemEntries(payload, currentPage),
    messages: payload.messages,
    conversationModes: payload.modes.map((mode) => ({ id: mode.id, label: mode.label })),
    subbarMeta: buildSubbarMeta(payload, activeConversation, activeMode),
  }
}
