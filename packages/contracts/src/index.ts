export type LoomId = string
export type IsoTimestamp = string

export interface ApiMeta {
  traceId?: string
  requestId?: string
  // Returned only by cursor-based list endpoints.
  nextCursor?: string | null
  // Returned only by cursor-based list endpoints.
  hasMore?: boolean
}

export interface ApiEnvelope<T> {
  data: T
  meta?: ApiMeta
}

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiError {
  code: string
  message: string
  retryable?: boolean
  fieldErrors?: ApiFieldError[]
}

export interface ApiErrorEnvelope {
  // Error responses may explicitly include `data: null` for transport symmetry.
  data?: null
  error: ApiError
  meta?: ApiMeta
}

export type WorkspacePageId = 'conversation' | 'capabilities' | 'openclaw' | 'files' | 'memory' | 'settings'
export type ProjectStatus = 'active' | 'archived'
export type ConversationMode = 'chat' | 'plan' | 'action' | 'review'
export type ConversationStatus = 'active' | 'idle' | 'blocked' | 'archived'
export type MessageKind = 'user' | 'assistant' | 'thinking_summary' | 'action_card' | 'run_progress' | 'external_feedback' | 'context_update' | 'system'
export type TimelineStatus = 'pending' | 'running' | 'waiting' | 'success' | 'failed' | 'skipped'
export type RunStatus = 'pending' | 'running' | 'waiting' | 'success' | 'failed' | 'cancelled'
export type ContextSnapshotKind = 'conversation_summary' | 'decisions' | 'open_loops' | 'planning_state' | 'active_context'
export type MemoryScope = 'global' | 'project' | 'conversation'
export type MemorySuggestionStatus = 'pending' | 'accepted' | 'rejected'
export type SettingScope = 'global' | 'project' | 'conversation'
export type SurfaceTone = 'neutral' | 'accent' | 'good' | 'warn' | 'danger'
export type StreamEventName =
  | 'message.delta'
  | 'message.done'
  | 'thinking.summary.delta'
  | 'thinking.summary.done'
  | 'trace.step.created'
  | 'trace.step.updated'
  | 'trace.step.completed'
  | 'context.updated'
  | 'memory.suggested'
  | 'run.completed'
  | 'run.failed'

// Phase 1 keeps a single cursor pagination shape across project, conversation, message,
// context snapshot, run step, file, and memory list endpoints.

export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
  hasMore: boolean
}

export interface ProjectListItem {
  id: LoomId
  name: string
  description: string
  status: ProjectStatus
  conversationCount: number
  lastMessageAt: IsoTimestamp | null
  updatedAt: IsoTimestamp
}

export interface CapabilityBindingSummary {
  defaultModelProfileId: LoomId | null
  enabledSkillIds: LoomId[]
  defaultRoutingPolicyId: LoomId | null
}

export interface ProjectView extends ProjectListItem {
  instructions: string
  pinnedConversationIds: LoomId[]
  capabilityBindings: CapabilityBindingSummary
}

export interface CreateProjectRequest {
  name?: string
  description?: string
  instructions?: string
}

export interface UpdateProjectRequest {
  name?: string
  description?: string
  instructions?: string
  status?: ProjectStatus
}

export interface ConversationListItem {
  id: LoomId
  projectId: LoomId
  title: string
  summary: string
  mode: ConversationMode
  status: ConversationStatus
  pinned: boolean
  updatedAt: IsoTimestamp
  lastMessageAt: IsoTimestamp | null
}

export interface ConversationView extends ConversationListItem {
  contextSummary: string
  activeRunId: LoomId | null
}

export interface CreateConversationRequest {
  title?: string
  mode?: ConversationMode
}

export interface UpdateConversationRequest {
  projectId?: LoomId
  title?: string
  mode?: ConversationMode
  status?: ConversationStatus
  pinned?: boolean
}

export interface MessageAttachmentRef {
  fileAssetId: LoomId
  displayName: string
  mimeType?: string
}

export interface MessageView {
  id: LoomId
  projectId: LoomId
  conversationId: LoomId
  kind: MessageKind
  role: 'user' | 'assistant' | 'system'
  body: string
  summary?: string
  statusLabel?: string
  latencyMs?: number | null
  sequence: number
  createdAt: IsoTimestamp
  completedAt?: IsoTimestamp | null
  attachments: MessageAttachmentRef[]
}

export interface SubmitMessageRequest {
  body: string
  clientMessageId?: LoomId
  requestedMode?: ConversationMode
  attachmentIds?: LoomId[]
  allowActions?: boolean
  allowMemory?: boolean
}

export interface SubmitMessageResponse {
  conversationId: LoomId
  userMessage: MessageView
  acceptedRunId: LoomId | null
  // Frontend must subscribe to this path for the canonical stream of message/trace/context events.
  streamPath: string
}

export interface ActionView {
  id: LoomId
  projectId: LoomId
  conversationId: LoomId
  runId: LoomId
  title: string
  status: 'pending' | 'running' | 'waiting' | 'completed' | 'failed' | 'cancelled'
  summary: string
  startedAt: IsoTimestamp
  completedAt?: IsoTimestamp | null
  stepIds: LoomId[]
}

export interface ContextReferenceItem {
  id: LoomId
  label: string
  kind: 'file' | 'memory' | 'conversation' | 'run'
  summary: string
}

export interface ContextSnapshotView {
  id: LoomId
  projectId: LoomId
  conversationId: LoomId
  kind: ContextSnapshotKind
  content: string
  updatedAt: IsoTimestamp
}

export interface ContextPanelView {
  // Used by the right-side Context panel as the canonical assembled snapshot.
  conversationSummary: string
  decisions: string[]
  openLoops: string[]
  activeGoals: string[]
  constraints: string[]
  references: ContextReferenceItem[]
  snapshots: ContextSnapshotView[]
  updatedAt: IsoTimestamp
}

export interface ContextRefreshResponse {
  context: ContextPanelView
}

export interface RunStepView {
  id: LoomId
  runId: LoomId
  title: string
  detail: string
  status: TimelineStatus
  startedAt?: IsoTimestamp | null
  completedAt?: IsoTimestamp | null
  errorMessage?: string | null
}

export interface RunView {
  id: LoomId
  actionId: LoomId
  projectId: LoomId
  conversationId: LoomId
  status: RunStatus
  startedAt: IsoTimestamp
  completedAt?: IsoTimestamp | null
  externalReference?: string | null
}

export interface TracePanelView {
  reasoningSummary: string
  activeAction: ActionView | null
  activeRun: RunView | null
  steps: RunStepView[]
  updatedAt: IsoTimestamp
}

export interface ModelProfileView {
  id: LoomId
  presetId: LoomId
  scope: SettingScope
  name: string
  provider: string
  modelId: string
  configured: boolean
  active: boolean
  supportsStreaming: boolean
  supportsImages: boolean
  supportsTools: boolean
  supportsLongContext: boolean
  supportsReasoningSummary: boolean
  timeoutMs: number
}

export interface SkillView {
  id: LoomId
  scope: SettingScope
  name: string
  enabled: boolean
  source: 'internal' | 'mcp'
}

export interface McpServerView {
  id: LoomId
  scope: SettingScope
  name: string
  status: 'connected' | 'disconnected' | 'degraded'
  resourceCount: number
  promptCount: number
  toolCount: number
}

export interface MemoryPolicyView {
  id: LoomId
  scope: SettingScope
  autoSuggest: boolean
  autoPromoteConversationSummary: boolean
  allowSystemWrites: boolean
}

export interface RoutingPolicyView {
  id: LoomId
  scope: SettingScope
  defaultRuntime: 'internal'
  allowExternalExecutors: boolean
  externalExecutorLabel?: string | null
}

export interface LlmModelOptionView {
  id: LoomId
  label: string
  description: string
}

export interface LlmProviderPresetView {
  id: LoomId
  label: string
  provider: string
  supported: boolean
  recommended: boolean
  apiBaseUrl: string
  defaultModelId: string
  description: string
  modelOptions: LlmModelOptionView[]
}

export interface LlmConfigView {
  id: LoomId
  presetId: LoomId
  provider: string
  displayName: string
  apiBaseUrl: string
  modelId: string
  configured: boolean
  active: boolean
  apiKeyHint: string
  systemPrompt: string
  temperature: number
  maxTokens?: number | null
  timeoutMs: number
  updatedAt: IsoTimestamp
}

export interface UpdateLlmConfigRequest {
  profileId?: LoomId
  presetId?: LoomId
  displayName?: string
  apiBaseUrl?: string
  modelId?: string
  apiKey?: string
  systemPrompt?: string
  temperature?: number
  maxTokens?: number | null
  timeoutMs?: number
  activate?: boolean
}

export interface LlmConnectionTestView {
  success: boolean
  provider: string
  modelId: string
  baseUrl: string
  message: string
  responsePreview: string
  testedAt: IsoTimestamp
  latencyMs?: number | null
}

export interface SettingsOverviewView {
  // Phase 1 keeps Capabilities and Settings sourced from the same underlying view model.
  activeScope: SettingScope
  tabs: Array<'Models' | 'Skills' | 'MCP' | 'Memory' | 'Routing'>
  modelProfiles: ModelProfileView[]
  skills: SkillView[]
  mcpServers: McpServerView[]
  memoryPolicy: MemoryPolicyView | null
  routingPolicy: RoutingPolicyView | null
  providerPresets: LlmProviderPresetView[]
  llmConfigs: LlmConfigView[]
  activeLlmConfig: LlmConfigView | null
  lastConnectionTest: LlmConnectionTestView | null
}

export interface FileAssetSummary {
  id: LoomId
  projectId: LoomId
  displayName: string
  mimeType: string
  sizeBytes: number
  parseStatus: 'pending' | 'ready' | 'failed'
  uploadedAt: IsoTimestamp
}

export interface MemoryItemView {
  id: LoomId
  scope: MemoryScope
  projectId?: LoomId | null
  conversationId?: LoomId | null
  content: string
  source: 'explicit' | 'assisted' | 'system'
  updatedAt: IsoTimestamp
}

export interface MemorySuggestionView {
  id: LoomId
  scope: MemoryScope
  status: MemorySuggestionStatus
  content: string
  createdAt: IsoTimestamp
}

export interface ConversationStreamEventBase {
  event: StreamEventName
  eventId: string
  projectId: LoomId
  conversationId: LoomId
  // ISO 8601 UTC timestamp.
  emittedAt: IsoTimestamp
}

export interface MessageDeltaEvent extends ConversationStreamEventBase {
  event: 'message.delta'
  messageId: LoomId
  chunk: string
  chunkIndex: number
}

export interface MessageDoneEvent extends ConversationStreamEventBase {
  event: 'message.done'
  message: MessageView
}

export interface ThinkingSummaryDeltaEvent extends ConversationStreamEventBase {
  event: 'thinking.summary.delta'
  messageId: LoomId
  chunk: string
  chunkIndex: number
}

export interface ThinkingSummaryDoneEvent extends ConversationStreamEventBase {
  event: 'thinking.summary.done'
  message: MessageView
}

export interface TraceStepCreatedEvent extends ConversationStreamEventBase {
  event: 'trace.step.created'
  runId: LoomId
  step: RunStepView
}

export interface TraceStepUpdatedEvent extends ConversationStreamEventBase {
  event: 'trace.step.updated'
  runId: LoomId
  step: RunStepView
}

export interface TraceStepCompletedEvent extends ConversationStreamEventBase {
  event: 'trace.step.completed'
  runId: LoomId
  step: RunStepView
}

export interface ContextUpdatedEvent extends ConversationStreamEventBase {
  event: 'context.updated'
  context: ContextPanelView
}

export interface MemorySuggestedEvent extends ConversationStreamEventBase {
  event: 'memory.suggested'
  suggestion: MemorySuggestionView
}

export interface RunCompletedEvent extends ConversationStreamEventBase {
  event: 'run.completed'
  run: RunView
}

export interface RunFailedEvent extends ConversationStreamEventBase {
  event: 'run.failed'
  run: RunView
  error: ApiError
}

export type ConversationStreamEvent =
  | MessageDeltaEvent
  | MessageDoneEvent
  | ThinkingSummaryDeltaEvent
  | ThinkingSummaryDoneEvent
  | TraceStepCreatedEvent
  | TraceStepUpdatedEvent
  | TraceStepCompletedEvent
  | ContextUpdatedEvent
  | MemorySuggestedEvent
  | RunCompletedEvent
  | RunFailedEvent

// Canonical stream order for a successful Phase 1 response:
// thinking.summary.delta* -> thinking.summary.done -> message.delta* -> message.done
// -> trace.step.created/updated/completed -> context.updated -> run.completed

export interface ProjectSummary {
  id: string
  name: string
  eyebrow: string
  description: string
  workspaceLabel: string
  lastUpdatedLabel: string
  openClawStatus: string
}

export interface WorkspacePageLink {
  id: WorkspacePageId
  label: string
  description: string
  shortcut?: string
  available?: boolean
}

export interface ConversationSummary {
  id: string
  title: string
  summary: string
  lastUpdatedLabel: string
  mode: ConversationMode
  status: ConversationStatus
  pinned: boolean
}

export interface WorkspaceModeOption {
  id: ConversationMode
  label: string
  description: string
}

export interface ConversationMessage {
  id: string
  kind: MessageKind
  label: string
  body: string
  emphasis?: string
  statusLabel?: string
  latencyMs?: number | null
  latencyLabel?: string
  createdAt?: IsoTimestamp | null
  completedAt?: IsoTimestamp | null
}

export interface ComposerToggle {
  label: string
  enabled: boolean
}

export interface ComposerState {
  placeholder: string
  primaryActionLabel: string
  secondaryActions: string[]
  toggles: ComposerToggle[]
}

export interface TraceStep {
  id: string
  label: string
  detail: string
  status: TimelineStatus
  startedAt?: IsoTimestamp | null
  completedAt?: IsoTimestamp | null
  errorMessage?: string | null
}

export interface ContextBlock {
  id: string
  label: string
  value: string
}

export interface OverviewCard {
  id: string
  title: string
  summary: string
  items: string[]
}

export interface DetailItem {
  label: string
  value: string
}

export interface StatusItem {
  label: string
  value: string
  tone?: SurfaceTone
}

export interface CapabilitiesOverview {
  summary: string
  cards: OverviewCard[]
  bindingRules: StatusItem[]
}

export interface OpenClawOverview {
  summary: string
  connection: DetailItem[]
  discovery: StatusItem[]
  routing: StatusItem[]
  recentActivity: StatusItem[]
  linkedConversations: string[]
}

export interface SettingsOverview {
  summary: string
  tabs: string[]
  profile: DetailItem[]
  guidance: string[]
  riskNotes: string[]
  providerPresets?: LlmProviderPresetView[]
  llmConfigs?: LlmConfigView[]
  activeConfig?: LlmConfigView | null
  lastConnectionTest?: LlmConnectionTestView | null
}

export interface LoomBootstrapPayload {
  appName: string
  description: string
  project: ProjectSummary
  pages: WorkspacePageLink[]
  recentConversations: ConversationSummary[]
  pinnedConversations: ConversationSummary[]
  modes: WorkspaceModeOption[]
  activeMode: ConversationMode
  conversationTitle: string
  conversationMeta: string
  messages: ConversationMessage[]
  composer: ComposerState
  traceSummary: string
  traceSteps: TraceStep[]
  contextBlocks: ContextBlock[]
  capabilities: CapabilitiesOverview
  openClaw: OpenClawOverview
  settings: SettingsOverview
}
