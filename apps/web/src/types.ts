export type {
  ApiEnvelope,
  CapabilitiesOverview,
  ComposerState,
  ContextPanelView,
  ContextBlock,
  CursorPage,
  ConversationStreamEvent,
  ConversationMessage,
  ConversationMode,
  ConversationSummary,
  DetailItem,
  FileAssetSummary,
  CreateConversationRequest,
  CreateProjectRequest,
  ProjectListItem,
  ProjectView,
  LoomBootstrapPayload,
  LlmConfigView,
  LlmConnectionTestView,
  LlmModelOptionView,
  LlmProviderPresetView,
  MemoryItemView,
  MemoryScope,
  MemorySuggestionStatus,
  MemorySuggestionView,
  MessageKind,
  MessageView,
  OpenClawOverview,
  OverviewCard,
  ProjectSummary,
  SettingsOverviewView,
  RunStepView,
  SettingsOverview,
  StatusItem,
  SubmitMessageRequest,
  SubmitMessageResponse,
  UpdateConversationRequest,
  UpdateLlmConfigRequest,
  SurfaceTone,
  TimelineStatus,
  TraceStep,
  WorkspacePageId,
  WorkspacePageLink,
} from '../../../packages/contracts/src/index'

export interface CapabilityCardView {
  id: string
  title: string
  summary: string
  items: string[]
}

export interface CapabilityBindingRuleView {
  label: string
  value: string
  tone: 'neutral' | 'accent' | 'good' | 'warn' | 'danger'
}

export interface CapabilityOverviewView {
  activeScope: string
  summary: string
  cards: CapabilityCardView[]
  bindingRules: CapabilityBindingRuleView[]
}
