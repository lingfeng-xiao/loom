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
  CreateConversationRequest,
  CreateProjectRequest,
  ConversationMessage,
  ConversationMode,
  ConversationListItem,
  ConversationSummary,
  ConversationView,
  ConversationStreamEvent,
  LlmConnectionTestView,
  LoomBootstrapPayload,
  MemoryItemView,
  MemorySuggestionView,
  MessageView,
  ProjectListItem,
  ProjectSummary,
  ProjectView,
  SettingsOverview,
  SettingsOverviewView,
  RunStepView,
  StatusItem,
  TraceStep,
  TracePanelView,
  UpdateLlmConfigRequest,
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
  createProject: (request?: CreateProjectRequest) => Promise<ProjectView>
  createConversation: (projectId: string, request?: CreateConversationRequest) => Promise<ConversationView>
  moveConversation: (conversationId: string, nextProjectId: string) => Promise<ConversationView>
  updateLlmSettings: (request: UpdateLlmConfigRequest) => Promise<void>
  testLlmSettings: (request: UpdateLlmConfigRequest) => Promise<LlmConnectionTestView>
  handlePrimaryAction: (actionId: string) => void
  handleHeaderAction: (actionId: string) => void
  handleSystemEntry: (entryId: string) => void
}

interface WorkbenchContextValue {
  state: WorkbenchDomainState
  actions: WorkbenchActions
}

const WorkbenchContext = createContext<WorkbenchContextValue | null>(null)
const OPTIMISTIC_ASSISTANT_PREFIX = 'optimistic-assistant-'

function isOptimisticAssistantMessage(message: ConversationMessage) {
  return message.id.startsWith(OPTIMISTIC_ASSISTANT_PREFIX)
}

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
    label: toReadableConversationLabel(message),
    body: message.body,
    emphasis: message.summary,
    statusLabel: message.statusLabel,
    latencyMs: message.latencyMs,
    createdAt: message.createdAt,
    completedAt: message.completedAt,
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
    startedAt: step.startedAt,
    completedAt: step.completedAt,
    errorMessage: step.errorMessage,
  }
}

function partitionConversations(items: ConversationListItem[]): {
  pinnedConversations: ConversationSummary[]
  recentConversations: ConversationSummary[]
} {
  const sortedItems = [...items].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  return {
    pinnedConversations: sortedItems.filter((item) => item.pinned).map(toConversationSummary),
    recentConversations: sortedItems.filter((item) => !item.pinned).map(toConversationSummary),
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
    summary: '设置页已切到真实读模型，展示当前作用域的模型、技能、MCP、记忆与路由概览。',
    tabs: settings.tabs,
    profile: activeModel
      ? [
          { label: '配置名称', value: activeModel.name },
          { label: '提供方', value: activeModel.provider },
          { label: '模型 ID', value: activeModel.modelId },
          {
            label: '能力',
            value: [
              activeModel.supportsStreaming ? '流式输出' : null,
              activeModel.supportsImages ? '图像' : null,
              activeModel.supportsTools ? '工具调用' : null,
              activeModel.supportsLongContext ? '长上下文' : null,
            ]
              .filter(Boolean)
              .join(' | '),
          },
          { label: '超时', value: `${activeModel.timeoutMs} ms` },
        ]
      : [],
    guidance: [
      `当前作用域：${settings.activeScope}`,
      `启用技能：${settings.skills.filter((skill) => skill.enabled).map((skill) => skill.name).join(' / ') || '无'}`,
      `默认运行时：${settings.routingPolicy?.defaultRuntime ?? '内部运行时'}`,
    ],
    riskNotes: [
      settings.routingPolicy?.allowExternalExecutors ? '外部执行器仍需受控开启。' : '当前未开放外部执行器直连。',
      settings.memoryPolicy?.allowSystemWrites ? '系统写入已开启，变更前需要同步测试记录。' : '系统写入未开启。',
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

function toReadableConversationLabel(message: MessageView): string {
  if (message.kind === 'user') {
    return '用户'
  }
  if (message.kind === 'thinking_summary') {
    return '思考'
  }
  return '助手'
}

function toReadableConversationSummary(item: ConversationListItem | ConversationView): ConversationSummary {
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

function toReadableProjectSummary(base: ProjectSummary, project: ProjectView): ProjectSummary {
  return {
    ...base,
    id: project.id,
    name: project.name,
    eyebrow: project.status === 'active' ? '远端项目' : `远端项目 / ${project.status}`,
    description: project.description,
    workspaceLabel: `项目：${project.name} / ${project.conversationCount} 个会话`,
    lastUpdatedLabel: `最近更新 ${project.updatedAt}`,
  }
}

function toReadableProjectListItem(project: ProjectView): ProjectListItem {
  return {
    id: project.id,
    name: project.name,
    description: project.description,
    status: project.status,
    conversationCount: project.conversationCount,
    lastMessageAt: project.lastMessageAt,
    updatedAt: project.updatedAt,
  }
}

function partitionRemoteConversations(items: ConversationListItem[]): {
  pinnedConversations: ConversationSummary[]
  recentConversations: ConversationSummary[]
} {
  const sortedItems = [...items].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  return {
    pinnedConversations: sortedItems.filter((item) => item.pinned).map(toReadableConversationSummary),
    recentConversations: sortedItems.filter((item) => !item.pinned).map(toReadableConversationSummary),
  }
}

function toReadableContextBlocks(context: ContextPanelView): ContextBlock[] {
  return [
    { id: 'context-goal', label: '当前目标', value: context.activeGoals[0] ?? '等待新的目标。' },
    { id: 'context-constraints', label: '约束条件', value: context.constraints.join(' | ') || '当前没有额外约束。' },
    { id: 'context-summary', label: '会话摘要', value: context.conversationSummary },
    { id: 'context-active', label: '进行中的事项', value: context.decisions.join(' | ') || '当前没有进行中的事项。' },
    { id: 'context-files', label: '关联资料', value: context.references[0]?.summary ?? '暂未关联资料。' },
    { id: 'context-open', label: '未闭环事项', value: context.openLoops.join(' | ') || '当前没有未闭环事项。' },
  ]
}

function toDetailedSettingsOverview(settings: SettingsOverviewView): SettingsOverview {
  const activeModel = settings.modelProfiles.find((profile) => profile.active) ?? settings.modelProfiles[0]

  return {
    summary: '这里展示当前生效模型、可选预设，以及最近一次连通性测试结果。',
    tabs: settings.tabs,
    profile: activeModel
      ? [
          { label: '配置名称', value: activeModel.name },
          { label: '提供方', value: activeModel.provider },
          { label: '模型 ID', value: activeModel.modelId },
          {
            label: '能力',
            value: [
              activeModel.supportsStreaming ? '流式输出' : null,
              activeModel.supportsImages ? '图像' : null,
              activeModel.supportsTools ? '工具调用' : null,
              activeModel.supportsLongContext ? '长上下文' : null,
            ]
              .filter(Boolean)
              .join(' | '),
          },
          { label: '超时', value: `${activeModel.timeoutMs} ms` },
        ]
      : [],
    guidance: [
      `当前作用域：${settings.activeScope}`,
      `已启用技能：${settings.skills.filter((skill) => skill.enabled).map((skill) => skill.name).join(' / ') || '无'}`,
      `默认运行时：${settings.routingPolicy?.defaultRuntime ?? '内部运行时'}`,
    ],
    riskNotes: [
      settings.routingPolicy?.allowExternalExecutors ? '外部执行器仍然启用，建议继续保留准入控制。' : '外部执行器当前处于关闭状态。',
      settings.memoryPolicy?.allowSystemWrites ? '系统记忆写入已开启，配置变更后需要验证。' : '系统记忆写入当前关闭。',
      '每次配置调整后都应重新做一次在线连通性测试。',
    ],
    providerPresets: settings.providerPresets,
    llmConfigs: settings.llmConfigs,
    activeConfig: settings.activeLlmConfig,
    lastConnectionTest: settings.lastConnectionTest,
  }
}

interface StreamOverlayState {
  conversationId: string
  messages: ConversationMessage[]
  traceSummary: string
  traceSteps: TraceStep[]
  contextBlocks: ContextBlock[]
  memorySuggestions: MemorySuggestionView[]
}

function createOverlayState(
  conversationId: string,
  messages: ConversationMessage[],
  traceSummary: string,
  traceSteps: TraceStep[],
  contextBlocks: ContextBlock[],
  memorySuggestions: MemorySuggestionView[] = [],
): StreamOverlayState {
  return {
    conversationId,
    messages,
    traceSummary,
    traceSteps,
    contextBlocks,
    memorySuggestions,
  }
}

function toMemorySuggestion(event: Extract<ConversationStreamEvent, { event: 'memory.suggested' }>): MemorySuggestionView {
  return event.suggestion
}

function mergeMemorySuggestions(
  current: MemorySuggestionView[],
  next: MemorySuggestionView[],
): MemorySuggestionView[] {
  const merged = new Map<string, MemorySuggestionView>()
  for (const item of current) {
    merged.set(item.id, item)
  }
  for (const item of next) {
    merged.set(item.id, item)
  }
  return [...merged.values()].sort((left, right) => right.createdAt.localeCompare(left.createdAt))
}

function upsertStreamingMessage(
  messages: ConversationMessage[],
  event: Extract<ConversationStreamEvent, { event: 'message.delta' | 'thinking.summary.delta' }>,
): ConversationMessage[] {
  const kind = event.event === 'thinking.summary.delta' ? 'thinking_summary' : 'assistant'
  const label = kind === 'thinking_summary' ? '思考' : '助手'
  const nextMessages = event.event === 'message.delta' ? messages.filter((item) => !isOptimisticAssistantMessage(item)) : [...messages]
  const existingIndex = nextMessages.findIndex((item) => item.id === event.messageId)

  if (existingIndex >= 0) {
    nextMessages[existingIndex] = {
      ...nextMessages[existingIndex],
      body: `${nextMessages[existingIndex].body}${event.chunk}`,
      statusLabel: 'streaming',
      completedAt: null,
    }
    return nextMessages
  }

  nextMessages.push({
    id: event.messageId,
    kind,
    label,
    body: event.chunk,
    statusLabel: 'streaming',
    createdAt: event.emittedAt,
    completedAt: null,
  })
  return nextMessages
}

function applyStreamEvent(overlay: StreamOverlayState, event: ConversationStreamEvent): StreamOverlayState {
  switch (event.event) {
    case 'message.delta': {
      const messageId = event.messageId
      const nextMessages = overlay.messages.filter((item) => !(item.kind === 'assistant' && item.statusLabel === 'streaming' && !item.body))
      const existingIndex = nextMessages.findIndex((item) => item.id === messageId)
      if (existingIndex >= 0) {
        nextMessages[existingIndex] = {
          ...nextMessages[existingIndex],
          body: `${nextMessages[existingIndex].body}${event.chunk}`,
          statusLabel: 'streaming',
        }
      } else {
        nextMessages.push({
          id: messageId,
          kind: 'assistant',
          label: event.event === 'thinking.summary.delta' ? '思考摘要' : '助手',
          body: event.chunk,
          statusLabel: 'streaming',
        })
      }
      return {
        ...overlay,
        messages: nextMessages,
      }
    }
    case 'message.done': {
      const nextMessage = toConversationMessage(event.message)
      const nextMessages = overlay.messages.filter((item) => !(item.id === nextMessage.id || (item.kind === 'assistant' && item.statusLabel === 'streaming' && !item.body)))
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

function applyConversationStreamEvent(overlay: StreamOverlayState, event: ConversationStreamEvent): StreamOverlayState {
  switch (event.event) {
    case 'message.delta':
    case 'thinking.summary.delta':
      return {
        ...overlay,
        messages: upsertStreamingMessage(overlay.messages, event),
        traceSummary:
          event.event === 'thinking.summary.delta' ? `${overlay.traceSummary}${event.chunk}`.trim() || overlay.traceSummary : overlay.traceSummary,
      }
    case 'message.done': {
      const nextMessage = toConversationMessage(event.message)
      const nextMessages = overlay.messages.filter((item) => item.id !== nextMessage.id && !isOptimisticAssistantMessage(item))
      nextMessages.push(nextMessage)
      return {
        ...overlay,
        messages: nextMessages,
      }
    }
    case 'thinking.summary.done': {
      const nextMessage = toConversationMessage(event.message)
      const nextMessages = overlay.messages.filter((item) => item.id !== nextMessage.id)
      nextMessages.push(nextMessage)
      return {
        ...overlay,
        messages: nextMessages,
        traceSummary: nextMessage.body || overlay.traceSummary,
      }
    }
    case 'trace.step.created':
    case 'trace.step.updated':
    case 'trace.step.completed': {
      const nextStep = toTraceStep(event.step)
      const nextSteps = overlay.traceSteps.filter((item) => item.id !== nextStep.id)
      nextSteps.push(nextStep)
      nextSteps.sort((left, right) => left.id.localeCompare(right.id))
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
    case 'memory.suggested':
      return {
        ...overlay,
        memorySuggestions: mergeMemorySuggestions(overlay.memorySuggestions, [toMemorySuggestion(event)]),
      }
    case 'run.failed':
      return {
        ...overlay,
        traceSummary: event.error.message,
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
  const [workspaceError, setWorkspaceError] = useState<string | null>(null)
  const [conversationError, setConversationError] = useState<string | null>(null)
  const [composerError, setComposerError] = useState<string | null>(null)
  const [streamOverlay, setStreamOverlay] = useState<StreamOverlayState | null>(null)
  const [streamMemorySuggestions, setStreamMemorySuggestions] = useState<MemorySuggestionView[]>([])
  const [remoteProjects, setRemoteProjects] = useState<ProjectListItem[] | null>(null)
  const [remoteProject, setRemoteProject] = useState<ProjectView | null>(null)
  const [remoteConversations, setRemoteConversations] = useState<ConversationListItem[] | null>(null)
  const [remoteConversation, setRemoteConversation] = useState<ConversationView | null>(null)
  const [remoteMessages, setRemoteMessages] = useState<MessageView[] | null>(null)
  const [remoteTrace, setRemoteTrace] = useState<TracePanelView | null>(null)
  const [remoteContext, setRemoteContext] = useState<ContextPanelView | null>(null)
  const [remoteMemory, setRemoteMemory] = useState<MemoryItemView[] | null>(null)
  const [memoryError, setMemoryError] = useState<string | null>(null)
  const [remoteSettings, setRemoteSettings] = useState<SettingsOverviewView | null>(null)
  const [remoteCapabilities, setRemoteCapabilities] = useState<CapabilityOverviewView | null>(null)
  const [draftsByConversation, setDraftsByConversation] = useState<Record<string, ComposerDraftState>>({})
  const [scrollPositions, setScrollPositions] = useState<Record<string, number>>({})
  const streamUnsubscribeRef = useRef<null | (() => void)>(null)
  const activeProjectId = route.projectId ?? payload.project.id
  const activeConversationId = route.conversationId ?? defaultConversationId(payload)
  const bootstrapConversationId = defaultConversationId(payload)

  const effectivePayload = useMemo(() => {
    const nextPayload =
      streamOverlay && streamOverlay.conversationId === activeConversationId
        ? {
            ...payload,
            messages: streamOverlay.messages,
            traceSummary: streamOverlay.traceSummary,
            traceSteps: streamOverlay.traceSteps,
            contextBlocks: streamOverlay.contextBlocks,
          }
        : payload

    const remoteConversationGroups = remoteConversations ? partitionRemoteConversations(remoteConversations) : null
    const pinnedConversations = remoteConversationGroups?.pinnedConversations ?? nextPayload.pinnedConversations
    const recentConversations = remoteConversationGroups?.recentConversations ?? nextPayload.recentConversations
    const activeConversation =
      remoteConversation ?? [...pinnedConversations, ...recentConversations].find((item) => item.id === activeConversationId) ?? null
    const nextProject = remoteProject ? toReadableProjectSummary(nextPayload.project, remoteProject) : nextPayload.project
    const overlayMessages = streamOverlay && streamOverlay.conversationId === activeConversationId ? streamOverlay.messages : null
    const overlayTraceSummary = streamOverlay && streamOverlay.conversationId === activeConversationId ? streamOverlay.traceSummary : null
    const overlayTraceSteps = streamOverlay && streamOverlay.conversationId === activeConversationId ? streamOverlay.traceSteps : null
    const overlayContextBlocks = streamOverlay && streamOverlay.conversationId === activeConversationId ? streamOverlay.contextBlocks : null
    const nextMessages = overlayMessages ?? (remoteMessages ? remoteMessages.map(toConversationMessage) : activeConversationId === bootstrapConversationId ? nextPayload.messages : [])
    const nextTraceSteps =
      overlayTraceSteps ?? (remoteTrace ? remoteTrace.steps.map(toTraceStep) : activeConversationId === bootstrapConversationId ? nextPayload.traceSteps : [])
    const nextContextBlocks =
      overlayContextBlocks ?? (remoteContext ? toReadableContextBlocks(remoteContext) : activeConversationId === bootstrapConversationId ? nextPayload.contextBlocks : [])
    const nextSettings = remoteSettings ? toDetailedSettingsOverview(remoteSettings) : nextPayload.settings
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
      conversationTitle: remoteConversation?.title ?? activeConversation?.title ?? (activeConversationId === bootstrapConversationId ? nextPayload.conversationTitle : '新会话'),
      conversationMeta: remoteConversation != null ? `${nextProject.name} · ${conversationStatusLabel} · ${conversationSummary}` : nextPayload.conversationMeta,
      messages: nextMessages,
      traceSummary: overlayTraceSummary ?? remoteTrace?.reasoningSummary ?? nextPayload.traceSummary,
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
    setStreamMemorySuggestions([])
    setConversationError(null)
    setComposerError(null)
  }, [activeConversationId, payload])

  useEffect(() => {
    if (!activeProjectId) {
      setRemoteProjects(null)
      setRemoteProject(null)
      setRemoteConversations(null)
      setRemoteMemory(null)
      setMemoryError(null)
      setStreamMemorySuggestions([])
      setRemoteSettings(null)
      setRemoteCapabilities(null)
      setWorkspaceError(null)
      return
    }

    const controller = new AbortController()
    setRemoteProjects(null)
    setRemoteProject(null)
    setRemoteConversations(null)
    setRemoteMemory(null)
    setMemoryError(null)
    setStreamMemorySuggestions([])
    setRemoteSettings(null)
    setRemoteCapabilities(null)
    setWorkspaceError(null)

    void Promise.all([
      sdk.workspace.listProjects(controller.signal),
      sdk.workspace.getProject(activeProjectId, controller.signal),
      sdk.workspace.listConversations(activeProjectId, controller.signal),
      sdk.workspace.getSettingsOverview('project', controller.signal),
      sdk.workspace.getCapabilitiesOverview('project', controller.signal),
    ])
      .then(([projects, project, conversations, settings, capabilities]) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteProjects(projects.items)
        setRemoteProject(project)
        setRemoteConversations(conversations.items)
        setRemoteSettings(settings)
        setRemoteCapabilities(capabilities)
        setWorkspaceError(null)
      })
      .catch((fetchError) => {
        if (controller.signal.aborted) {
          return
        }

        setWorkspaceError(fetchError instanceof Error ? fetchError.message : '远端工作区数据加载失败。')
      })

    void sdk.workspace
      .getMemory(activeProjectId, controller.signal)
      .then((memory) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteMemory(memory.items)
      })
      .catch((fetchError) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteMemory([])
        setMemoryError(fetchError instanceof Error ? fetchError.message : '记忆数据读取失败')
      })

    return () => controller.abort()
  }, [activeProjectId, sdk])

  useEffect(() => {
    if (!activeProjectId || !activeConversationId) {
      setRemoteConversation(null)
      setRemoteMessages(null)
      setRemoteTrace(null)
      setRemoteContext(null)
      setConversationError(null)
      return
    }

    const controller = new AbortController()
    setRemoteConversation(null)
    setRemoteMessages(null)
    setRemoteTrace(null)
    setRemoteContext(null)
    setConversationError(null)

    void Promise.all([
      sdk.workspace.getConversation(activeProjectId, activeConversationId, controller.signal),
      sdk.workspace.listMessages(activeProjectId, activeConversationId, controller.signal),
      sdk.workspace.getTrace(activeProjectId, activeConversationId, controller.signal),
      sdk.workspace.getContext(activeProjectId, activeConversationId, controller.signal),
    ])
      .then(([conversation, messages, trace, context]) => {
        if (controller.signal.aborted) {
          return
        }

        setRemoteConversation(conversation)
        setRemoteMessages(messages.items)
        setRemoteTrace(trace)
        setRemoteContext(context)
        setConversationError(null)
      })
      .catch((fetchError) => {
        if (controller.signal.aborted) {
          return
        }

        setConversationError(fetchError instanceof Error ? fetchError.message : '会话数据同步失败。')
      })

    return () => controller.abort()
  }, [activeConversationId, activeProjectId, payload.messages, payload.traceSteps, sdk])

  useEffect(() => () => {
    streamUnsubscribeRef.current?.()
  }, [])

  const openConversation = (conversationId: string) => {
    navigate({
      ...route,
      layout: 'app',
      page: 'conversation',
      projectId: activeProjectId,
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
      projectId: activeProjectId,
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
      if (!conversationId || !activeProjectId) {
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

      setComposerError(null)
      setDraftsByConversation((current) => ({
        ...current,
        [key]: {
          ...(current[key] ?? fallbackDraft),
          draftText: '',
          submitting: true,
        },
      }))

      const optimisticUserId = `optimistic-user-${Date.now()}`
      const optimisticAssistantId = `${OPTIMISTIC_ASSISTANT_PREFIX}${Date.now()}`
      const nowIso = new Date().toISOString()
      const baseMessages = effectivePayload.messages
      const baseTraceSummary = effectivePayload.traceSummary
      const baseTraceSteps = effectivePayload.traceSteps
      const baseContextBlocks = effectivePayload.contextBlocks

      setStreamOverlay(
        createOverlayState(
          conversationId,
          [
            ...baseMessages,
            {
              id: optimisticUserId,
              kind: 'user',
              label: '用户',
              body,
              createdAt: nowIso,
              completedAt: nowIso,
            },
            {
              id: optimisticAssistantId,
              kind: 'assistant',
              label: '助手',
              body: '',
              statusLabel: 'pending',
              createdAt: nowIso,
              completedAt: null,
            },
          ],
          baseTraceSummary,
          baseTraceSteps,
          baseContextBlocks,
          streamMemorySuggestions,
        ),
      )

      try {
        const response = await sdk.workspace.submitMessage(activeProjectId, conversationId, {
          body,
          requestedMode: route.mode ?? findConversationMode(effectivePayload, conversationId),
          allowActions: draft.allowActions,
          allowMemory: draft.allowMemory,
        })

        streamUnsubscribeRef.current?.()
        setStreamOverlay((current) => {
          const nextMessages = (current?.messages ?? []).filter((message) => message.id !== optimisticUserId)
          nextMessages.push(toConversationMessage(response.userMessage))
          return createOverlayState(
            conversationId,
            nextMessages,
            current?.traceSummary ?? baseTraceSummary,
            current?.traceSteps ?? baseTraceSteps,
            current?.contextBlocks ?? baseContextBlocks,
            current?.memorySuggestions ?? streamMemorySuggestions,
          )
        })
        let receivedFirstEvent = false
        streamUnsubscribeRef.current = streamClient.subscribe(
          response.streamPath,
          (event) => {
            if (!receivedFirstEvent) {
              receivedFirstEvent = true
              setDraftsByConversation((current) => ({
                ...current,
                [key]: {
                  ...(current[key] ?? fallbackDraft),
                  submitting: false,
                },
              }))
            }
            if (event.event === 'run.failed') {
              setComposerError(event.error.message)
            }
            if (event.event === 'memory.suggested') {
              setStreamMemorySuggestions((current) => mergeMemorySuggestions(current, [event.suggestion]))
            }
            setStreamOverlay((current) =>
              applyConversationStreamEvent(
                current ?? createOverlayState(conversationId, baseMessages, baseTraceSummary, baseTraceSteps, baseContextBlocks),
                event,
              ),
            )
          },
          () => {
            streamUnsubscribeRef.current = null
            setDraftsByConversation((current) => ({
              ...current,
              [key]: {
                ...(current[key] ?? fallbackDraft),
                draftText: '',
                submitting: false,
              },
            }))
            onRefreshPayload()
          },
        )

        setDraftsByConversation((current) => ({
          ...current,
          [key]: {
            ...(current[key] ?? fallbackDraft),
            submitting: true,
          },
        }))
      } catch (submitError) {
        setComposerError(submitError instanceof Error ? submitError.message : '发送消息失败。')
        setStreamOverlay(
          createOverlayState(
            conversationId,
            baseMessages,
            baseTraceSummary,
            baseTraceSteps,
            baseContextBlocks,
            streamMemorySuggestions,
          ),
        )
        setDraftsByConversation((current) => ({
          ...current,
          [key]: {
            ...(current[key] ?? fallbackDraft),
            draftText: body,
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
    async createProject(request) {
      const project = await sdk.workspace.createProject(request)
      setRemoteProjects((current) => {
        const next = [...(current ?? []), toReadableProjectListItem(project)]
        return next.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
      })
      return project
    },
    async createConversation(projectId, request) {
      const conversation = await sdk.workspace.createConversation(projectId, request)
      const createdSummary = toReadableConversationSummary(conversation)
      if (projectId === activeProjectId) {
        setRemoteConversations((current) => {
          const next = [conversation, ...(current ?? []).filter((item) => item.id !== conversation.id)]
          return next.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
        })
        setRemoteConversation(conversation)
        setRemoteMessages([])
        setRemoteTrace(null)
        setRemoteContext(null)
      }
      navigate(
        {
          ...route,
          layout: 'app',
          page: 'conversation',
          projectId,
          conversationId: conversation.id,
          mode: conversation.mode,
          callbackKind: null,
        },
        true,
      )
      setDraftsByConversation((current) => ({
        ...current,
        [conversation.id]: current[conversation.id] ?? {
          draftText: '',
          attachments: [],
          allowActions: true,
          allowMemory: true,
          submitting: false,
        },
      }))
      if (projectId === activeProjectId) {
        setRemoteConversations((current) => {
          const items = current ?? []
          const withoutCurrent = items.filter((item) => item.id !== createdSummary.id)
          return [conversation, ...withoutCurrent].sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
        })
      }
      return conversation
    },
    async moveConversation(conversationId, nextProjectId) {
      if (!activeProjectId) {
        throw new Error('当前没有可用项目。')
      }

      const conversation = await sdk.workspace.updateConversation(activeProjectId, conversationId, {
        projectId: nextProjectId,
      })

      const refreshedProjects = await sdk.workspace.listProjects()
      setRemoteProjects(refreshedProjects.items)
      setRemoteConversation(conversation)
      navigate(
        {
          ...route,
          layout: 'app',
          page: 'conversation',
          projectId: nextProjectId,
          conversationId: conversation.id,
          mode: conversation.mode,
          callbackKind: null,
        },
        true,
      )
      return conversation
    },
    async updateLlmSettings(request) {
      const settings = await sdk.workspace.updateLlmSettings(request)
      setRemoteSettings(settings)
    },
    async testLlmSettings(request) {
      const testResult = await sdk.workspace.testLlmSettings(request)
      setRemoteSettings((current) => (current ? { ...current, lastConnectionTest: testResult } : current))
      return testResult
    },
    handlePrimaryAction(actionId) {
      if (actionId === 'new-thread') {
        if (!activeProjectId) {
          setWorkspaceError('当前没有可用项目。')
          return
        }
        void actions
          .createConversation(activeProjectId, {})
          .catch((createError) => setWorkspaceError(createError instanceof Error ? createError.message : '创建会话失败。'))
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

  const availableProjects = remoteProjects ?? [
    {
      id: effectivePayload.project.id,
      name: effectivePayload.project.name,
      description: effectivePayload.project.description,
      status: 'active',
      conversationCount: effectivePayload.pinnedConversations.length + effectivePayload.recentConversations.length,
      lastMessageAt: null,
      updatedAt: new Date().toISOString(),
    },
  ]

  const baseState = useMemo(
    () =>
      adaptBootstrapToWorkbench(effectivePayload, {
        route,
        loading,
        bootstrapError: error,
        workspaceError,
        conversationError,
        composerError,
        bootstrapSource,
        draftsByConversation,
        scrollPositions,
        availableProjects,
        globalSearchOpen,
        commandPaletteOpen,
        leftSidebarCollapsed,
      }),
    [
      availableProjects,
      bootstrapSource,
      commandPaletteOpen,
      composerError,
      conversationError,
      draftsByConversation,
      effectivePayload,
      error,
      globalSearchOpen,
      leftSidebarCollapsed,
      loading,
      route,
      scrollPositions,
      workspaceError,
    ],
  )

  const memory = useMemo(() => {
    const remoteItems = remoteMemory ?? baseState.memory.items
    const suggestions = streamMemorySuggestions
    return {
      activeProjectId,
      activeConversationId,
      items: remoteItems,
      suggestions,
      error: memoryError,
    }
  }, [activeConversationId, activeProjectId, baseState.memory.items, memoryError, remoteMemory, streamMemorySuggestions])

  const state = useMemo(
    () => ({
      ...baseState,
      memory,
    }),
    [baseState, memory],
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
