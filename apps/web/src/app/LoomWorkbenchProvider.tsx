import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { BootstrapSourceViewModel } from './bootstrapSource'
import { adaptBootstrapToWorkbench } from './bootstrapAdapter'
import type { LoomRouteState, UiRightPanelTab } from './routeTypes'
import type { WorkbenchDomainState, ComposerDraftState } from '../domains/workbenchTypes'
import { createLoomSdk } from '../sdk/loomApiClient'
import { createBrowserStreamClient } from '../services/streamClient'
import type {
  CapabilitiesOverview,
  CapabilityOverviewView,
  ContextBlock,
  ContextPanelView,
  ConversationMessage,
  ConversationMode,
  ConversationListItem,
  ConversationSummary,
  ConversationView,
  ConversationStreamEvent,
  LoomBootstrapPayload,
  MessageView,
  ProjectSummary,
  ProjectView,
  SettingsOverview,
  SettingsOverviewView,
  RunStepView,
  StatusItem,
  TraceStep,
  TracePanelView,
  WorkspacePageId,
} from '../types'

interface LoomWorkbenchProviderProps {
  apiBaseUrl: string
  payload: LoomBootstrapPayload
  route: LoomRouteState
  loading: boolean
  error: string | null
  bootstrapSource: BootstrapSourceViewModel
  onCycleBootstrapSource: () => void
  onRefreshPayload: () => void
  navigate: (next: LoomRouteState, replace?: boolean) => void
  children: ReactNode
}

interface WorkbenchActions {
  openConversation: (conversationId: string) => void
  openPage: (page: WorkspacePageId) => void
  setMode: (mode: ConversationMode) => void
  setRightPanelTab: (tab: UiRightPanelTab) => void
  setSettingsSection: (section: string) => void
  updateDraft: (text: string) => void
  submitDraft: () => Promise<void>
  setScrollPosition: (conversationId: string, scrollTop: number) => void
  openGlobalSearch: () => void
  closeGlobalSearch: () => void
  toggleLeftSidebar: () => void
  cycleBootstrapSourceMode: () => void
  handlePrimaryAction: (actionId: string) => void
  handleHeaderAction: (actionId: string) => void
  handleSystemEntry: (entryId: string) => void
}

interface WorkbenchContextValue {
  state: WorkbenchDomainState
  actions: WorkbenchActions
}

const WorkbenchContext = createContext<WorkbenchContextValue | null>(null)

function defaultConversationId(payload: LoomBootstrapPayload): string | null {
  return payload.pinnedConversations[0]?.id ?? payload.recentConversations[0]?.id ?? null
}

function findConversationMode(payload: LoomBootstrapPayload, conversationId: string | null) {
  return [...payload.pinnedConversations, ...payload.recentConversations].find((item) => item.id === conversationId)?.mode ?? payload.activeMode
}

function toConversationLabel(message: MessageView): string {
  if (message.kind === 'user') {
    return '用户'
  }
  if (message.kind === 'thinking_summary') {
    return '思考摘要'
  }
  return '助手'
}

function toConversationMessage(message: MessageView): ConversationMessage {
  return {
    id: message.id,
    kind: message.kind,
    label: toConversationLabel(message),
    body: message.body,
    emphasis: message.summary,
    statusLabel: message.statusLabel,
  }
}

function toConversationSummary(item: ConversationListItem | ConversationView): ConversationSummary {
  return {
    id: item.id,
    title: item.title,
    summary: 'contextSummary' in item && item.contextSummary ? item.contextSummary : item.summary,
    lastUpdatedLabel: `最近更新 ${item.updatedAt}`,
    mode: item.mode,
    status: item.status,
    pinned: item.pinned,
  }
}

function toProjectSummary(base: ProjectSummary, project: ProjectView): ProjectSummary {
  return {
    ...base,
    id: project.id,
    name: project.name,
    eyebrow: project.status === 'active' ? '远端项目' : `远端项目 · ${project.status}`,
    description: project.description,
    workspaceLabel: `项目：${project.name} · ${project.conversationCount} 个会话`,
    lastUpdatedLabel: `最近更新 ${project.updatedAt}`,
  }
}

function toTraceStep(step: RunStepView): TraceStep {
  return {
    id: step.id,
    label: step.title,
    detail: step.detail,
    status: step.status,
  }
}

function sortByUpdatedAtDesc<T extends { updatedAt: string }>(items: T[]): T[] {
  return [...items].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
}

function partitionConversations(items: ConversationListItem[]): {
  pinnedConversations: ConversationSummary[]
  recentConversations: ConversationSummary[]
} {
  const summaries = items.map(toConversationSummary)
  return {
    pinnedConversations: sortByUpdatedAtDesc(summaries.filter((item) => item.pinned)),
    recentConversations: sortByUpdatedAtDesc(summaries.filter((item) => !item.pinned)),
  }
}

function toContextBlocks(context: ContextPanelView): ContextBlock[] {
  return [
    { id: 'context-goal', label: '当前目标', value: context.activeGoals[0] ?? '等待下一步目标' },
    { id: 'context-constraints', label: '约束条件', value: context.constraints.join(' | ') || '暂无明确约束' },
    { id: 'context-summary', label: '摘要', value: context.conversationSummary },
    { id: 'context-active', label: '进行中的事项', value: context.decisions.join(' | ') || '暂无进行中的事项' },
    { id: 'context-files', label: '参考输入', value: context.references[0]?.summary ?? '暂无引用输入' },
    { id: 'context-open', label: '未闭环事项', value: context.openLoops.join(' | ') || '暂无未闭环事项' },
  ]
}

function toSettingsOverview(settings: SettingsOverviewView): SettingsOverview {
  const activeModel = settings.modelProfiles[0]

  return {
    summary: '设置页已切到真实读模型，展示当前作用域的模型、Skills、MCP、Memory 与 Routing 概览。',
    tabs: settings.tabs,
    profile: activeModel
      ? [
          { label: '配置名称', value: activeModel.name },
          { label: '提供方', value: activeModel.provider },
          { label: '模型 ID', value: activeModel.modelId },
          {
            label: '能力',
            value: [
              activeModel.supportsStreaming ? 'Streaming' : null,
              activeModel.supportsImages ? 'Images' : null,
              activeModel.supportsTools ? 'Tools' : null,
              activeModel.supportsLongContext ? 'Long Context' : null,
            ]
              .filter(Boolean)
              .join(' | '),
          },
          { label: '超时', value: `${activeModel.timeoutMs} ms` },
        ]
      : [],
    guidance: [
      `当前作用域：${settings.activeScope}`,
      `启用 Skills：${settings.skills.filter((skill) => skill.enabled).map((skill) => skill.name).join(' / ') || '无'}`,
      `默认运行时：${settings.routingPolicy?.defaultRuntime ?? 'internal'}`,
    ],
    riskNotes: [
      settings.routingPolicy?.allowExternalExecutors ? '外部执行器仍需受控开启。' : '当前未开放外部执行器直连。',
      settings.memoryPolicy?.allowSystemWrites ? 'System 写入已开启，变更前需要同步测试记录。' : 'System 写入未开启。',
      '任何配置变更都必须同步到运行和测试文档里。',
    ],
  }
}

function toCapabilitiesOverview(capabilities: CapabilityOverviewView): CapabilitiesOverview {
  return {
    summary: capabilities.summary,
    cards: capabilities.cards,
    bindingRules: capabilities.bindingRules as StatusItem[],
  }
}

interface StreamOverlayState {
  conversationId: string
  messages: ConversationMessage[]
  traceSteps: TraceStep[]
  contextBlocks: ContextBlock[]
}

function createOverlayState(payload: LoomBootstrapPayload, conversationId: string): StreamOverlayState {
  return {
    conversationId,
    messages: payload.messages,
    traceSteps: payload.traceSteps,
    contextBlocks: payload.contextBlocks,
  }
}

function applyStreamEvent(overlay: StreamOverlayState, event: ConversationStreamEvent): StreamOverlayState {
  switch (event.event) {
    case 'thinking.summary.delta':
    case 'message.delta': {
      const messageId = event.messageId
      const nextMessages = [...overlay.messages]
      const existingIndex = nextMessages.findIndex((item) => item.id === messageId)
      if (existingIndex >= 0) {
        nextMessages[existingIndex] = {
          ...nextMessages[existingIndex],
          body: `${nextMessages[existingIndex].body}${event.chunk}`,
        }
      } else {
        nextMessages.push({
          id: messageId,
          kind: event.event === 'thinking.summary.delta' ? 'thinking_summary' : 'assistant',
          label: event.event === 'thinking.summary.delta' ? '思考摘要' : '助手',
          body: event.chunk,
        })
      }
      return {
        ...overlay,
        messages: nextMessages,
      }
    }
    case 'thinking.summary.done':
    case 'message.done': {
      const nextMessage = toConversationMessage(event.message)
      const nextMessages = overlay.messages.filter((item) => item.id !== nextMessage.id)
      nextMessages.push(nextMessage)
      return {
        ...overlay,
        messages: nextMessages,
      }
    }
    case 'trace.step.created':
    case 'trace.step.updated':
    case 'trace.step.completed': {
      const nextStep = toTraceStep(event.step)
      const nextSteps = overlay.traceSteps.filter((item) => item.id !== nextStep.id)
      nextSteps.push(nextStep)
      return {
        ...overlay,
        traceSteps: nextSteps,
      }
    }
    case 'context.updated':
      return {
        ...overlay,
        contextBlocks: toContextBlocks(event.context),
      }
    default:
      return overlay
  }
}

export function LoomWorkbenchProvider({
  apiBaseUrl,
  payload,
  route,
  loading,
  error,
  bootstrapSource,
  onCycleBootstrapSource,
  onRefreshPayload,
  navigate,
  children,
}: LoomWorkbenchProviderProps) {
  const sdk = useMemo(() => createLoomSdk({ baseUrl: apiBaseUrl }), [apiBaseUrl])
  const streamClient = useMemo(() => createBrowserStreamClient(apiBaseUrl), [apiBaseUrl])
  const [globalSearchOpen, setGlobalSearchOpen] = useState(false)
  const [commandPaletteOpen] = useState(false)
  const [leftSidebarCollapsed, setLeftSidebarCollapsed] = useState(false)
  const [runtimeError, setRuntimeError] = useState<string | null>(null)
  const [streamOverlay, setStreamOverlay] = useState<StreamOverlayState | null>(null)
  const [remoteProject, setRemoteProject] = useState<ProjectView | null>(null)
  const [remoteConversations, setRemoteConversations] = useState<ConversationListItem[] | null>(null)
  const [remoteConversation, setRemoteConversation] = useState<ConversationView | null>(null)
  const [remoteMessages, setRemoteMessages] = useState<MessageView[] | null>(null)
  const [remoteTrace, setRemoteTrace] = useState<TracePanelView | null>(null)
  const [remoteContext, setRemoteContext] = useState<ContextPanelView | null>(null)
  const [remoteSettings, setRemoteSettings] = useState<SettingsOverviewView | null>(null)
  const [remoteCapabilities, setRemoteCapabilities] = useState<CapabilityOverviewView | null>(null)
  const [draftsByConversation, setDraftsByConversation] = useState<Record<string, ComposerDraftState>>({})
  const [scrollPositions, setScrollPositions] = useState<Record<string, number>>({})
  const streamUnsubscribeRef = useRef<null | (() => void)>(null)
  const combinedError = runtimeError ?? error
  const activeConversationId = route.conversationId ?? defaultConversationId(payload)

  const effectivePayload = useMemo(() => {
    const nextPayload =
      streamOverlay && streamOverlay.conversationId === activeConversationId
        ? {
            ...payload,
            messages: streamOverlay.messages,
            traceSteps: streamOverlay.traceSteps,
            contextBlocks: streamOverlay.contextBlocks,
          }
        : payload

    const remoteConversationGroups = remoteConversations ? partitionConversations(remoteConversations) : null
    const pinnedConversations = remoteConversationGroups?.pinnedConversations ?? nextPayload.pinnedConversations
    const recentConversations = remoteConversationGroups?.recentConversations ?? nextPayload.recentConversations
    const activeConversation =
      remoteConversation ?? [...pinnedConversations, ...recentConversations].find((item) => item.id === activeConversationId) ?? null
    const nextProject = remoteProject ? toProjectSummary(nextPayload.project, remoteProject) : nextPayload.project
    const nextMessages = remoteMessages ? remoteMessages.map(toConversationMessage) : nextPayload.messages
    const nextTraceSteps = remoteTrace ? remoteTrace.steps.map(toTraceStep) : nextPayload.traceSteps
    const nextContextBlocks = remoteContext ? toContextBlocks(remoteContext) : nextPayload.contextBlocks
    const nextSettings = remoteSettings ? toSettingsOverview(remoteSettings) : nextPayload.settings
    const nextCapabilities = remoteCapabilities ? toCapabilitiesOverview(remoteCapabilities) : nextPayload.capabilities
    const conversationStatusLabel =
      remoteConversation == null
        ? ''
        : remoteConversation.status === 'active'
          ? '进行中'
          : remoteConversation.status
    const conversationSummary = remoteConversation?.contextSummary || remoteConversation?.summary || nextPayload.conversationMeta

    return {
      ...nextPayload,
      project: nextProject,
      pinnedConversations,
      recentConversations,
      activeMode: route.mode ?? activeConversation?.mode ?? nextPayload.activeMode,
      conversationTitle: remoteConversation?.title ?? activeConversation?.title ?? nextPayload.conversationTitle,
      conversationMeta: remoteConversation != null ? `${nextProject.name} · ${conversationStatusLabel} · ${conversationSummary}` : nextPayload.conversationMeta,
      messages: nextMessages,
      traceSummary: remoteTrace?.reasoningSummary ?? nextPayload.traceSummary,
      traceSteps: nextTraceSteps,
      contextBlocks: nextContextBlocks,
      settings: nextSettings,
      capabilities: nextCapabilities,
    }
  }, [
    activeConversationId,
    payload,
    remoteCapabilities,
    remoteContext,
    remoteConversation,
    remoteConversations,
    remoteMessages,
    remoteProject,
    remoteSettings,
    remoteTrace,
    route.mode,
    streamOverlay,
  ])

  useEffect(() => {
    setStreamOverlay(null)
  }, [activeConversationId, payload])

  useEffect(() => {
    if (!payload.project.id) {
      setRemoteProject(null)
      setRemoteConversations(null)
      setRemoteSettings(null)
      setRemoteCapabilities(null)
      return
    }

    const controller = new AbortController()
    setRemoteProject(null)
    setRemoteConversations(null)
    setRemoteSettings(null)
    setRemoteCapabilities(null)

    void Promise.all([
      sdk.workspace.getProject(payload.project.id, controller.signal),
      sdk.workspace.listConversations(payload.project.id, controller.signal),
      sdk.workspace.getSettingsOverview('project', controller.signal),
      sdk.workspace.getCapabilitiesOverview('project', controller.signal),
    ])
      .then(([project, conversations, settings, capabilities]) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteProject(project)
        setRemoteConversations(conversations.items)
        setRemoteSettings(settings)
        setRemoteCapabilities(capabilities)
      })
      .catch(() => undefined)

    return () => controller.abort()
  }, [payload.project.id, sdk])

  useEffect(() => {
    if (!payload.project.id || !activeConversationId) {
      setRemoteConversation(null)
      setRemoteMessages(null)
      setRemoteTrace(null)
      setRemoteContext(null)
      return
    }

    const controller = new AbortController()
    setRemoteConversation(null)
    setRemoteMessages(null)
    setRemoteTrace(null)
    setRemoteContext(null)

    void Promise.all([
      sdk.workspace.getConversation(payload.project.id, activeConversationId, controller.signal),
      sdk.workspace.listMessages(payload.project.id, activeConversationId, controller.signal),
      sdk.workspace.getTrace(payload.project.id, activeConversationId, controller.signal),
      sdk.workspace.getContext(payload.project.id, activeConversationId, controller.signal),
    ])
      .then(([conversation, messages, trace, context]) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteConversation(conversation)
        setRemoteMessages(messages.items)
        setRemoteTrace(trace)
        setRemoteContext(context)
      })
      .catch(() => undefined)

    return () => controller.abort()
  }, [activeConversationId, payload.messages, payload.project.id, payload.traceSteps, sdk])

  useEffect(() => () => {
    streamUnsubscribeRef.current?.()
  }, [])

  const openConversation = (conversationId: string) => {
    navigate({
      ...route,
      layout: 'app',
      page: 'conversation',
      projectId: effectivePayload.project.id,
      conversationId,
      mode: findConversationMode(effectivePayload, conversationId),
      callbackKind: null,
    })
  }

  const openPage = (page: WorkspacePageId) => {
    navigate({
      ...route,
      layout: 'app',
      page,
      projectId: effectivePayload.project.id,
      conversationId: activeConversationId,
      settingsSection: page === 'settings' ? route.settingsSection ?? effectivePayload.settings.tabs[0] ?? 'Models' : route.settingsSection,
      callbackKind: null,
    })
  }

  const actions: WorkbenchActions = {
    openConversation,
    openPage,
    setMode(mode) {
      navigate({
        ...route,
        layout: 'app',
        page: 'conversation',
        projectId: effectivePayload.project.id,
        conversationId: activeConversationId,
        mode,
        callbackKind: null,
      })
    },
    setRightPanelTab(tab) {
      navigate({
        ...route,
        layout: 'app',
        traceTab: tab,
        callbackKind: null,
      })
    },
    setSettingsSection(section) {
      navigate({
        ...route,
        layout: 'app',
        page: 'settings',
        projectId: effectivePayload.project.id,
        settingsSection: section,
        callbackKind: null,
      })
    },
    updateDraft(text) {
      const key = route.conversationId ?? 'global'
      setDraftsByConversation((current) => {
        const existing = current[key]
        return {
          ...current,
          [key]: {
            draftText: text,
            attachments: existing?.attachments ?? [],
            allowActions: existing?.allowActions ?? true,
            allowMemory: existing?.allowMemory ?? true,
            submitting: existing?.submitting ?? false,
          },
        }
      })
    },
    async submitDraft() {
      const conversationId = activeConversationId
      if (!conversationId) {
        return
      }

      const key = conversationId
      const fallbackDraft: ComposerDraftState = draftsByConversation[key] ?? {
        draftText: '',
        attachments: [],
        allowActions: true,
        allowMemory: true,
        submitting: false,
      }
      const draft = draftsByConversation[key] ?? fallbackDraft
      const body = draft.draftText.trim()
      if (!body) {
        return
      }

      setRuntimeError(null)
      setDraftsByConversation((current) => ({
        ...current,
        [key]: {
          ...(current[key] ?? fallbackDraft),
          submitting: true,
        },
      }))

      try {
        const response = await sdk.workspace.submitMessage(payload.project.id, conversationId, {
          body,
          requestedMode: route.mode ?? findConversationMode(effectivePayload, conversationId),
          allowActions: draft.allowActions,
          allowMemory: draft.allowMemory,
        })

        streamUnsubscribeRef.current?.()
        setStreamOverlay(createOverlayState(payload, conversationId))
        streamUnsubscribeRef.current = streamClient.subscribe(
          response.streamPath,
          (event) => {
            setStreamOverlay((current) => applyStreamEvent(current ?? createOverlayState(payload, conversationId), event))
          },
          () => {
            streamUnsubscribeRef.current = null
            onRefreshPayload()
          },
        )

        setDraftsByConversation((current) => ({
          ...current,
          [key]: {
            ...(current[key] ?? fallbackDraft),
            draftText: '',
            submitting: false,
          },
        }))
      } catch (submitError) {
        setRuntimeError(submitError instanceof Error ? submitError.message : 'Failed to submit message')
        setDraftsByConversation((current) => ({
          ...current,
          [key]: {
            ...(current[key] ?? fallbackDraft),
            submitting: false,
          },
        }))
      }
    },
    setScrollPosition(conversationId, scrollTop) {
      setScrollPositions((current) => ({
        ...current,
        [conversationId]: scrollTop,
      }))
    },
    openGlobalSearch() {
      setGlobalSearchOpen(true)
    },
    closeGlobalSearch() {
      setGlobalSearchOpen(false)
    },
    toggleLeftSidebar() {
      setLeftSidebarCollapsed((current) => !current)
    },
    cycleBootstrapSourceMode() {
      onCycleBootstrapSource()
    },
    handlePrimaryAction(actionId) {
      if (actionId === 'new-thread') {
        openConversation(activeConversationId ?? 'conversation-shell')
        return
      }

      if (actionId === 'capabilities') {
        openPage('capabilities')
        return
      }

      if (actionId === 'automation') {
        openPage('openclaw')
        return
      }

      openConversation(effectivePayload.recentConversations[2]?.id ?? activeConversationId ?? 'conversation-shell')
    },
    handleHeaderAction(actionId) {
      if (actionId === 'workspace') {
        openConversation(activeConversationId ?? 'conversation-shell')
        return
      }

      if (actionId === 'submit') {
        navigate({
          ...route,
          layout: 'app',
          page: 'conversation',
          projectId: effectivePayload.project.id,
          conversationId: activeConversationId,
          mode: 'action',
          callbackKind: null,
        })
        return
      }

      openPage('settings')
    },
    handleSystemEntry(entryId) {
      if (entryId === 'settings') {
        openPage('settings')
        return
      }

      if (entryId === 'files') {
        openPage('files')
        return
      }

      openPage('memory')
    },
  }

  const state = useMemo(
    () =>
      adaptBootstrapToWorkbench(effectivePayload, {
        route,
        loading,
        error: combinedError,
        bootstrapSource,
        draftsByConversation,
        scrollPositions,
        globalSearchOpen,
        commandPaletteOpen,
        leftSidebarCollapsed,
      }),
    [bootstrapSource, combinedError, commandPaletteOpen, draftsByConversation, effectivePayload, globalSearchOpen, leftSidebarCollapsed, loading, route, scrollPositions],
  )

  return <WorkbenchContext.Provider value={{ state, actions }}>{children}</WorkbenchContext.Provider>
}

export function useWorkbenchContext() {
  const context = useContext(WorkbenchContext)
  if (!context) {
    throw new Error('useWorkbenchContext must be used inside LoomWorkbenchProvider')
  }
  return context
}
