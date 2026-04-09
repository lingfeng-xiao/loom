import type {
  CapabilitiesOverview,
  ComposerState,
  ContextBlock,
  ConversationMessage,
  ConversationMode,
  OpenClawOverview,
  MemoryItemView,
  MemorySuggestionView,
  ProjectListItem,
  ProjectSummary,
  SettingsOverview,
  TimelineStatus,
  TraceStep,
  WorkspacePageId,
  WorkspacePageLink,
} from '../types'
import type { UiRightPanelTab } from '../app/routeTypes'
import type { BootstrapSourceViewModel } from '../app/bootstrapSource'
import type {
  ConversationThreadViewModel,
  SidebarPrimaryAction,
  SidebarSystemEntry,
  SidebarThreadGroup,
  WorkspaceHeaderAction,
} from '../components/shell/shellTypes'

export interface ComposerDraftState {
  draftText: string
  attachments: string[]
  allowActions: boolean
  allowMemory: boolean
  submitting: boolean
}

export interface TraceRunStatus {
  status: TimelineStatus | 'idle'
  label: string
}

export interface ContextSectionViewModel {
  id: string
  title: string
  blocks: ContextBlock[]
}

export interface ProjectDomainState {
  currentProject: ProjectSummary
  availableProjects: ProjectListItem[]
  workspaceTitle: string
  workspaceMeta: string
  environmentStatus: string
  headerActions: WorkspaceHeaderAction[]
}

export interface ConversationDomainState {
  pages: WorkspacePageLink[]
  currentPage: WorkspacePageId
  activeConversationId: string | null
  activeThread: ConversationThreadViewModel | null
  activeMode: ConversationMode
  primaryActions: SidebarPrimaryAction[]
  threadGroups: SidebarThreadGroup[]
  systemEntries: SidebarSystemEntry[]
  messages: ConversationMessage[]
  conversationModes: Array<{ id: ConversationMode; label: string }>
  subbarMeta: string
}

export interface ComposerDomainState {
  snapshot: ComposerState
  currentDraft: ComposerDraftState
  draftsByConversation: Record<string, ComposerDraftState>
}

export interface TraceDomainState {
  summary: string
  steps: TraceStep[]
  runStatus: TraceRunStatus
  activeTab: UiRightPanelTab
}

export interface ContextDomainState {
  sections: ContextSectionViewModel[]
}

export interface CapabilitiesDomainState extends CapabilitiesOverview {}

export interface OpenClawDomainState extends OpenClawOverview {}

export interface SettingsDomainState extends SettingsOverview {
  activeSection: string
}

export interface MemoryDomainState {
  activeProjectId: string | null
  activeConversationId: string | null
  items: MemoryItemView[] | null
  suggestions: MemorySuggestionView[]
  error: string | null
}

export interface UiDomainState {
  loading: boolean
  error: string | null
  bootstrapSource: BootstrapSourceViewModel
  globalSearchOpen: boolean
  commandPaletteOpen: boolean
  leftSidebarCollapsed: boolean
  rightPanelTab: UiRightPanelTab
  scrollPositions: Record<string, number>
}

export interface WorkbenchDomainState {
  project: ProjectDomainState
  conversation: ConversationDomainState
  composer: ComposerDomainState
  trace: TraceDomainState
  context: ContextDomainState
  capabilities: CapabilitiesDomainState
  openClaw: OpenClawDomainState
  settings: SettingsDomainState
  memory: MemoryDomainState
  ui: UiDomainState
}
