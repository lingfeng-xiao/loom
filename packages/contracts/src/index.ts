export type ProjectType = 'knowledge' | 'ops' | 'learning'
export type ConversationMode = 'chat' | 'plan'
export type ConversationStatus = 'active' | 'archived'
export type MemoryScope = 'global' | 'project' | 'derived'
export type MemoryStatus = 'active' | 'disabled'
export type PlanStatus = 'draft' | 'ready' | 'approved' | 'running' | 'completed' | 'failed'
export type PlanStepStatus = 'pending' | 'running' | 'completed' | 'failed'
export type NodeType = 'local_pc' | 'server'
export type NodeStatus = 'online' | 'offline' | 'degraded' | 'unknown'
export type AssetType =
  | 'knowledge_card'
  | 'ops_note'
  | 'learning_card'
  | 'summary_note'
  | 'structured_markdown'
export type SkillId =
  | 'knowledge-card-generator'
  | 'ops-summary-generator'
  | 'obsidian-note-writer'
export type CommandId =
  | '/project-new'
  | '/project-switch'
  | '/project-status'
  | '/plan'
  | '/plan-run'
  | '/save-card'
  | '/memory-show'
  | '/memory-save'
  | '/skill-list'
  | '/node-status'
  | '/logs'

export type WorkspaceLanguage = 'zh-CN' | 'en-US'
export type WorkspaceDensity = 'compact' | 'comfortable'
export type WorkspaceTheme = 'light' | 'system' | 'dark'
export type DefaultLandingView = 'last_conversation' | 'project_home'

export interface ApiError {
  code: string
  message: string
  details?: string
}

export interface ApiResponse<T> {
  data: T
  meta?: Record<string, unknown>
}

export interface PageResult<T> {
  items: T[]
  total: number
}

export interface ProjectSummary {
  id: string
  name: string
  type: ProjectType
  description: string
  defaultSkills: SkillId[]
  defaultCommands: CommandId[]
  boundNodeIds: string[]
  knowledgeRoots: string[]
  projectMemoryRefs?: string[]
  createdAt: string
  updatedAt: string
}

export interface ConversationSummary {
  id: string
  projectId: string
  title: string
  mode: ConversationMode
  status: ConversationStatus
  summary: string
  createdAt: string
  updatedAt: string
}

export interface ConversationUpdateRequest {
  title?: string
  status?: ConversationStatus
  summary?: string
}

export interface ConversationListQuery {
  mode?: ConversationMode
  status?: ConversationStatus
  q?: string
}

export interface MessageRecord {
  id: string
  conversationId: string
  projectId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt: string
}

export interface MemoryRecord {
  id: string
  scope: MemoryScope
  projectId: string | null
  title: string
  content: string
  priority: number
  status: MemoryStatus
  sourceType: string
  sourceRef: string | null
  createdAt: string
  updatedAt: string
}

export interface PlanStepRecord {
  id: string
  title: string
  description: string
  status: PlanStepStatus
  result: string
  sortOrder: number
}

export interface PlanExecutionResult {
  summary: string
  outputAssetIds: string[]
  logs: string[]
}

export interface PlanRecord {
  id: string
  projectId: string
  conversationId: string
  goal: string
  constraints: string[]
  status: PlanStatus
  approvalRequired: boolean
  steps: PlanStepRecord[]
  executionResult: PlanExecutionResult | null
  createdAt: string
  updatedAt: string
}

export interface SkillRecord {
  id: SkillId
  name: string
  description: string
  version: string
  triggerMode: 'manual' | 'plan'
  enabled: boolean
}

export interface AssetRecord {
  id: string
  projectId: string
  type: AssetType
  title: string
  contentRef: string
  storagePath: string
  sourceConversationId: string | null
  sourcePlanId: string | null
  sourceNodeId: string | null
  tags: string[]
  createdAt: string
}

export interface NodeServiceStatus {
  name: string
  kind?: string
  target?: string
  status: 'online' | 'offline' | 'degraded' | 'up' | 'down' | 'unknown'
  detail?: string
  recordedAt?: string
}

export interface NodeSnapshot {
  hostname: string
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  services: NodeServiceStatus[]
  recordedAt: string
}

export interface NodeRecord {
  id: string
  name: string
  type: NodeType
  host: string
  tags: string[]
  status: NodeStatus
  lastHeartbeat: string | null
  snapshot: NodeSnapshot | null
  capabilities: string[]
  lastError?: string | null
}

export interface ModelSettings {
  providerLabel: string
  baseUrl: string
  model: string
  temperature: number
}

export interface VaultSettings {
  serverVaultRoot: string
  localVaultRoot: string
  assetPathTemplate: string
  writeTarget: 'server'
}

export interface NodeSettings {
  heartbeatTimeoutSeconds: number
  inspectorShowOffline: boolean
  centerNodeLabel: string
}

export interface AppearanceSettings {
  theme: WorkspaceTheme
  density: WorkspaceDensity
  accentTone: 'blue-teal'
  fontSans: 'Geist Sans'
  fontMono: 'JetBrains Mono'
}

export interface WorkspaceSettings {
  workspaceName: string
  language: WorkspaceLanguage
  density: WorkspaceDensity
  defaultProjectId: string | null
  defaultLandingView: DefaultLandingView
  inspectorDefaultOpen: boolean
  model: ModelSettings
  vault: VaultSettings
  nodes: NodeSettings
  appearance: AppearanceSettings
  enabledCommands: CommandId[]
  enabledSkills: SkillId[]
  updatedAt: string
}

export interface CommandExecutionRequest {
  projectId: string
  conversationId?: string
  commandId: CommandId
  args?: Record<string, string>
}

export interface CommandExecutionResult {
  commandId: CommandId
  message: string
  planId?: string
  assetId?: string
  memoryIds?: string[]
}

export interface DashboardPayload {
  project: ProjectSummary
  conversations: ConversationSummary[]
  memories: MemoryRecord[]
  recentAssets: AssetRecord[]
  nodes: NodeRecord[]
  activePlan: PlanRecord | null
}

export interface BootstrapPayload {
  projects: ProjectSummary[]
  commands: CommandId[]
  skills: SkillRecord[]
  nodes: NodeRecord[]
  defaultProjectId: string | null
  workspaceSettings: WorkspaceSettings
}

export interface AppBootstrapPayload extends BootstrapPayload {}

export const COMMAND_CATALOG: Array<{ commandId: CommandId; message: string }> = [
  { commandId: '/project-new', message: '创建项目并进入新的项目上下文。' },
  { commandId: '/project-switch', message: '切换当前激活的项目上下文。' },
  { commandId: '/project-status', message: '查看当前项目的状态概览。' },
  { commandId: '/plan', message: '基于当前上下文生成计划草案。' },
  { commandId: '/plan-run', message: '执行已经批准的计划。' },
  { commandId: '/save-card', message: '将内容沉淀为正式资产。' },
  { commandId: '/memory-show', message: '查看全局记忆与项目记忆。' },
  { commandId: '/memory-save', message: '按指定作用域保存一条记忆。' },
  { commandId: '/skill-list', message: '查看当前项目可用的技能。' },
  { commandId: '/node-status', message: '查看已注册节点的状态与快照。' },
  { commandId: '/logs', message: '查看最近执行与审计日志摘要。' },
]

export const DEFAULT_SKILLS: SkillRecord[] = [
  {
    id: 'knowledge-card-generator',
    name: '知识卡片生成器',
    description: '把对话与计划整理成长周期可复用的知识卡片。',
    version: '0.1.0',
    triggerMode: 'plan',
    enabled: true,
  },
  {
    id: 'ops-summary-generator',
    name: '运维总结生成器',
    description: '把节点快照与运维记录整理成正式运维资产。',
    version: '0.1.0',
    triggerMode: 'plan',
    enabled: true,
  },
  {
    id: 'obsidian-note-writer',
    name: 'Obsidian 笔记写入器',
    description: '把渲染后的 Markdown 资产写入配置好的 Vault。',
    version: '0.1.0',
    triggerMode: 'manual',
    enabled: true,
  },
]

export const SAMPLE_PROJECTS: ProjectSummary[] = [
  {
    id: 'project-knowledge-base',
    name: '知识库',
    type: 'knowledge',
    description: '用于沉淀长期技术知识与生成知识卡片的工作区。',
    defaultSkills: ['knowledge-card-generator', 'obsidian-note-writer'],
    defaultCommands: ['/plan', '/save-card', '/memory-save'],
    boundNodeIds: ['node-local'],
    knowledgeRoots: ['vault/knowledge'],
    projectMemoryRefs: [],
    createdAt: '2026-04-06T00:00:00Z',
    updatedAt: '2026-04-06T00:00:00Z',
  },
  {
    id: 'project-ops-console',
    name: '运维控制台',
    type: 'ops',
    description: '用于查看节点状态、快照、诊断信息和运维总结的工作区。',
    defaultSkills: ['ops-summary-generator', 'obsidian-note-writer'],
    defaultCommands: ['/node-status', '/logs', '/plan-run'],
    boundNodeIds: ['node-local', 'node-jd'],
    knowledgeRoots: ['vault/ops'],
    projectMemoryRefs: [],
    createdAt: '2026-04-06T00:00:00Z',
    updatedAt: '2026-04-06T00:00:00Z',
  },
  {
    id: 'project-english-lab',
    name: '英语实验室',
    type: 'learning',
    description: '用于语言练习和学习笔记的工作区。',
    defaultSkills: ['knowledge-card-generator'],
    defaultCommands: ['/project-status', '/save-card'],
    boundNodeIds: [],
    knowledgeRoots: ['vault/learning'],
    projectMemoryRefs: [],
    createdAt: '2026-04-06T00:00:00Z',
    updatedAt: '2026-04-06T00:00:00Z',
  },
]

export const DEFAULT_WORKSPACE_SETTINGS: WorkspaceSettings = {
  workspaceName: 'Loom',
  language: 'zh-CN',
  density: 'comfortable',
  defaultProjectId: SAMPLE_PROJECTS[0].id,
  defaultLandingView: 'last_conversation',
  inspectorDefaultOpen: true,
  model: {
    providerLabel: 'OpenAI Compatible',
    baseUrl: 'http://127.0.0.1:11434/v1',
    model: 'gpt-4.1-mini',
    temperature: 0.2,
  },
  vault: {
    serverVaultRoot: './data/obsidian-server',
    localVaultRoot: './data/obsidian-local',
    assetPathTemplate: '/{vault}/{project-slug}/{asset-type}/{yyyy}/{mm}/{slug}.md',
    writeTarget: 'server',
  },
  nodes: {
    heartbeatTimeoutSeconds: 90,
    inspectorShowOffline: true,
    centerNodeLabel: 'JD 中心节点',
  },
  appearance: {
    theme: 'light',
    density: 'comfortable',
    accentTone: 'blue-teal',
    fontSans: 'Geist Sans',
    fontMono: 'JetBrains Mono',
  },
  enabledCommands: COMMAND_CATALOG.map((item) => item.commandId),
  enabledSkills: DEFAULT_SKILLS.map((item) => item.id),
  updatedAt: '2026-04-06T00:00:00Z',
}
