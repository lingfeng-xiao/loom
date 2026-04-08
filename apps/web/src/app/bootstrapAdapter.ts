import type { ComposerDraftState, UiDomainState, WorkbenchDomainState } from '../domains/workbenchTypes'
import type { BootstrapSourceViewModel } from './bootstrapSource'
import { buildCapabilitiesState } from '../services/capabilitiesService'
import { buildContextState } from '../services/contextService'
import { buildConversationState } from '../services/conversationService'
import { buildOpenClawState } from '../services/openClawService'
import { buildProjectState } from '../services/projectService'
import { buildSettingsState } from '../services/settingsService'
import { buildTraceState } from '../services/traceService'
import type { LoomBootstrapPayload } from '../types'
import type { LoomRouteState } from './routeTypes'

interface BootstrapAdapterOptions {
  route: LoomRouteState
  loading: boolean
  error: string | null
  bootstrapSource: BootstrapSourceViewModel
  draftsByConversation: Record<string, ComposerDraftState>
  scrollPositions: Record<string, number>
  globalSearchOpen: boolean
  commandPaletteOpen: boolean
  leftSidebarCollapsed: boolean
}

function defaultDraftState(payload: LoomBootstrapPayload): ComposerDraftState {
  return {
    draftText: '',
    attachments: [],
    allowActions: payload.composer.toggles.find((toggle) => toggle.label.toLowerCase().includes('action'))?.enabled ?? true,
    allowMemory: payload.composer.toggles.find((toggle) => toggle.label.toLowerCase().includes('memory'))?.enabled ?? true,
    submitting: false,
  }
}

function buildUiState(options: BootstrapAdapterOptions): UiDomainState {
  return {
    loading: options.loading,
    error: options.error,
    bootstrapSource: options.bootstrapSource,
    globalSearchOpen: options.globalSearchOpen,
    commandPaletteOpen: options.commandPaletteOpen,
    leftSidebarCollapsed: options.leftSidebarCollapsed,
    rightPanelTab: options.route.traceTab,
    scrollPositions: options.scrollPositions,
  }
}

function buildHeaderCopy(payload: LoomBootstrapPayload, route: LoomRouteState) {
  const currentPage = route.page === 'welcome' || route.page === 'callback' ? 'conversation' : route.page
  const selectedConversationId = route.conversationId ?? payload.pinnedConversations[0]?.id ?? payload.recentConversations[0]?.id ?? null
  const currentConversation = [...payload.pinnedConversations, ...payload.recentConversations].find((item) => item.id === selectedConversationId) ?? null

  if (currentPage === 'conversation') {
    return {
      title: currentConversation?.title ?? payload.conversationTitle,
      meta: `${payload.project.name} · ${currentConversation?.lastUpdatedLabel ?? payload.project.lastUpdatedLabel} · ${payload.project.openClawStatus}`,
      actions: [
        { id: 'workspace', label: '工作区', icon: 'switch' as const },
        { id: 'submit', label: '提交', icon: 'submit' as const, tone: 'primary' as const },
        { id: 'more', label: '更多', icon: 'more' as const },
      ],
    }
  }

  const pageTitleMap: Record<string, { title: string; meta: string }> = {
    capabilities: { title: '技能和应用', meta: '模型、MCP、Skills 与执行器能力基线' },
    openclaw: { title: '自动化', meta: '执行器、路由与回调运行基线' },
    files: { title: 'Files', meta: '项目文件与引用资产' },
    memory: { title: 'Memory', meta: '分层长期记忆与上下文策略' },
    settings: { title: '设置', meta: '模型、路由、记忆与工作台配置' },
  }

  const pageCopy = pageTitleMap[currentPage] ?? pageTitleMap.capabilities
  return {
    title: pageCopy.title,
    meta: pageCopy.meta,
    actions: [
      { id: 'workspace', label: '回到会话', icon: 'chat' as const },
      { id: 'submit', label: '打开会话', icon: 'thread' as const, tone: 'primary' as const },
      { id: 'more', label: '更多', icon: 'more' as const },
    ],
  }
}

export function adaptBootstrapToWorkbench(payload: LoomBootstrapPayload, options: BootstrapAdapterOptions): WorkbenchDomainState {
  const conversation = buildConversationState(payload, options.route)
  const header = buildHeaderCopy(payload, options.route)
  const activeConversationId = conversation.activeConversationId ?? 'global'
  const fallbackDraft = defaultDraftState(payload)
  const currentDraft = options.draftsByConversation[activeConversationId] ?? fallbackDraft

  return {
    project: buildProjectState(payload, header.title, header.meta, header.actions),
    conversation,
    composer: {
      snapshot: payload.composer,
      currentDraft,
      draftsByConversation: options.draftsByConversation,
    },
    trace: buildTraceState(payload, options.route.traceTab),
    context: buildContextState(payload),
    capabilities: buildCapabilitiesState(payload),
    openClaw: buildOpenClawState(payload),
    settings: buildSettingsState(payload, options.route.settingsSection ?? payload.settings.tabs[0] ?? 'Models'),
    ui: buildUiState(options),
  }
}
