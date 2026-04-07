import type {
  AppBootstrapPayload,
  AssetRecord,
  CommandExecutionRequest,
  CommandExecutionResult,
  ConversationListQuery,
  ConversationMode,
  ConversationSummary,
  ConversationUpdateRequest,
  MemoryRecord,
  MessageRecord,
  NodeRecord,
  PlanRecord,
  ProjectSummary,
  WorkspaceSettings,
} from '@loom/contracts'
import { apiClient } from '@/api/client'
import { createMockWorkspace } from '@/data/mock'

type ApiEnvelope<T> = { data: T }

type ProjectCreateRequest = {
  name: string
  type: ProjectSummary['type']
  description?: string
  defaultSkills?: ProjectSummary['defaultSkills']
  defaultCommands?: ProjectSummary['defaultCommands']
  boundNodeIds?: string[]
  knowledgeRoots?: string[]
}

type ProjectUpdateRequest = Partial<{
  name: string
  type: ProjectSummary['type']
  description: string
  defaultSkills: ProjectSummary['defaultSkills']
  defaultCommands: ProjectSummary['defaultCommands']
  boundNodeIds: string[]
  knowledgeRoots: string[]
  projectMemoryRefs: string[]
}>

type ConversationCreateRequest = {
  title: string
  mode: ConversationMode
  summary?: string
}

type MessageCreateRequest = {
  role: MessageRecord['role']
  content: string
}

type PlanCreateRequest = {
  projectId: string
  conversationId: string
  goal: string
  constraints: string[]
  approvalRequired: boolean
  steps?: Array<{ title: string; description: string; sortOrder?: number }>
}

type WorkspaceSettingsUpdateRequest = Partial<{
  workspaceName: string
  language: WorkspaceSettings['language']
  density: WorkspaceSettings['density']
  defaultProjectId: string | null
  defaultLandingView: WorkspaceSettings['defaultLandingView']
  inspectorDefaultOpen: boolean
  model: WorkspaceSettings['model']
  vault: WorkspaceSettings['vault']
  nodes: WorkspaceSettings['nodes']
  appearance: WorkspaceSettings['appearance']
  enabledCommands: WorkspaceSettings['enabledCommands']
  enabledSkills: WorkspaceSettings['enabledSkills']
}>

let mockWorkspace = createMockWorkspace()

function unwrap<T>(response: ApiEnvelope<T>) {
  return response.data
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function nowIso() {
  return new Date().toISOString()
}

function createId(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`
}

async function apiOrMock<T>(request: () => Promise<ApiEnvelope<T>>, fallback: () => T | Promise<T>): Promise<T> {
  try {
    return unwrap(await request())
  } catch {
    return fallback()
  }
}

function ensureProject(projectId: string) {
  const project = mockWorkspace.bootstrap.projects.find((item) => item.id === projectId)
  if (!project) {
    throw new Error(`Unknown project: ${projectId}`)
  }
  return project
}

function readConversations(projectId: string) {
  return mockWorkspace.conversationsByProject[projectId] ?? []
}

function writeConversations(projectId: string, conversations: ConversationSummary[]) {
  mockWorkspace.conversationsByProject[projectId] = conversations
}

function readPlans(projectId: string) {
  return mockWorkspace.plansByProject[projectId] ?? []
}

function writePlans(projectId: string, plans: PlanRecord[]) {
  mockWorkspace.plansByProject[projectId] = plans
}

function readMessages(conversationId: string) {
  return mockWorkspace.messagesByConversation[conversationId] ?? []
}

function writeMessages(conversationId: string, messages: MessageRecord[]) {
  mockWorkspace.messagesByConversation[conversationId] = messages
}

function readMemories(projectId: string) {
  return mockWorkspace.memoriesByProject[projectId] ?? []
}

function writeMemories(projectId: string, memories: MemoryRecord[]) {
  mockWorkspace.memoriesByProject[projectId] = memories
}

function readAssets(projectId: string) {
  return mockWorkspace.assetsByProject[projectId] ?? []
}

function writeAssets(projectId: string, assets: AssetRecord[]) {
  mockWorkspace.assetsByProject[projectId] = assets
}

function updateProjectInMock(nextProject: ProjectSummary) {
  mockWorkspace.bootstrap.projects = mockWorkspace.bootstrap.projects.map((project) =>
    project.id === nextProject.id ? nextProject : project,
  )
}

function filterConversations(conversations: ConversationSummary[], query?: ConversationListQuery) {
  const filtered = conversations.filter((conversation) => {
    if (query?.mode && conversation.mode !== query.mode) return false
    if (query?.status && conversation.status !== query.status) return false
    if (query?.q) {
      const needle = query.q.toLowerCase()
      if (
        !conversation.title.toLowerCase().includes(needle) &&
        !conversation.summary.toLowerCase().includes(needle)
      ) {
        return false
      }
    }
    return true
  })

  return filtered.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
}

export async function fetchBootstrap() {
  return apiOrMock<AppBootstrapPayload>(
    () => apiClient.get<ApiEnvelope<AppBootstrapPayload>>('/api/bootstrap'),
    () => clone(mockWorkspace.bootstrap),
  )
}

export async function fetchWorkspaceSettings() {
  return apiOrMock<WorkspaceSettings>(
    () => apiClient.get<ApiEnvelope<WorkspaceSettings>>('/api/settings/workspace'),
    () => clone(mockWorkspace.bootstrap.workspaceSettings),
  )
}

export async function fetchProjectConversations(projectId: string, query?: ConversationListQuery) {
  const params = new URLSearchParams()
  if (query?.mode) params.set('mode', query.mode)
  if (query?.status) params.set('status', query.status)
  if (query?.q) params.set('q', query.q)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return apiOrMock<ConversationSummary[]>(
    () => apiClient.get<ApiEnvelope<ConversationSummary[]>>(`/api/projects/${projectId}/conversations${suffix}`),
    () => clone(filterConversations(readConversations(projectId), query)),
  )
}

export async function fetchConversationMessages(conversationId: string) {
  return apiOrMock<MessageRecord[]>(
    () => apiClient.get<ApiEnvelope<MessageRecord[]>>(`/api/messages?conversationId=${conversationId}`),
    () => clone(readMessages(conversationId)),
  )
}

export async function fetchProjectMemories(projectId: string) {
  return apiOrMock<MemoryRecord[]>(
    () => apiClient.get<ApiEnvelope<MemoryRecord[]>>(`/api/memories?projectId=${projectId}`),
    () => clone(readMemories(projectId)),
  )
}

export async function fetchProjectPlans(projectId: string) {
  return apiOrMock<PlanRecord[]>(
    () =>
      apiClient
        .get<ApiEnvelope<PlanRecord[]>>('/api/plans')
        .then((response) => ({ data: unwrap(response).filter((plan) => plan.projectId === projectId) })),
    () => clone(readPlans(projectId)),
  )
}

export async function fetchProjectAssets(projectId: string) {
  return apiOrMock<AssetRecord[]>(
    () =>
      apiClient
        .get<ApiEnvelope<AssetRecord[]>>('/api/assets')
        .then((response) => ({ data: unwrap(response).filter((asset) => asset.projectId === projectId) })),
    () => clone(readAssets(projectId)),
  )
}

export async function fetchNodes() {
  return apiOrMock<NodeRecord[]>(
    () => apiClient.get<ApiEnvelope<NodeRecord[]>>('/api/nodes'),
    () => clone(mockWorkspace.bootstrap.nodes),
  )
}

export async function createProject(request: ProjectCreateRequest) {
  return apiOrMock<ProjectSummary>(
    () => apiClient.post<ApiEnvelope<ProjectSummary>>('/api/projects', request),
    () => {
      const project: ProjectSummary = {
        id: createId('project'),
        name: request.name,
        type: request.type,
        description: request.description ?? '',
        defaultSkills: request.defaultSkills ?? [],
        defaultCommands: request.defaultCommands ?? [],
        boundNodeIds: request.boundNodeIds ?? [],
        knowledgeRoots: request.knowledgeRoots ?? [],
        projectMemoryRefs: [],
        createdAt: nowIso(),
        updatedAt: nowIso(),
      }

      mockWorkspace.bootstrap.projects = [project, ...mockWorkspace.bootstrap.projects]
      mockWorkspace.conversationsByProject[project.id] = []
      mockWorkspace.memoriesByProject[project.id] = []
      mockWorkspace.plansByProject[project.id] = []
      mockWorkspace.assetsByProject[project.id] = []
      return clone(project)
    },
  )
}

export async function updateProject(projectId: string, request: ProjectUpdateRequest) {
  return apiOrMock<ProjectSummary>(
    () => apiClient.put<ApiEnvelope<ProjectSummary>>(`/api/projects/${projectId}`, request),
    () => {
      const project = ensureProject(projectId)
      const updated: ProjectSummary = {
        ...project,
        ...request,
        defaultSkills: request.defaultSkills ?? project.defaultSkills,
        defaultCommands: request.defaultCommands ?? project.defaultCommands,
        boundNodeIds: request.boundNodeIds ?? project.boundNodeIds,
        knowledgeRoots: request.knowledgeRoots ?? project.knowledgeRoots,
        projectMemoryRefs: request.projectMemoryRefs ?? project.projectMemoryRefs,
        updatedAt: nowIso(),
      }
      updateProjectInMock(updated)
      return clone(updated)
    },
  )
}

export async function createConversation(projectId: string, request: ConversationCreateRequest) {
  return apiOrMock<ConversationSummary>(
    () => apiClient.post<ApiEnvelope<ConversationSummary>>(`/api/projects/${projectId}/conversations`, request),
    () => {
      ensureProject(projectId)
      const conversation: ConversationSummary = {
        id: createId('conversation'),
        projectId,
        title: request.title,
        mode: request.mode,
        status: 'active',
        summary: request.summary ?? '',
        createdAt: nowIso(),
        updatedAt: nowIso(),
      }
      writeConversations(projectId, [conversation, ...readConversations(projectId)])
      writeMessages(conversation.id, [])
      return clone(conversation)
    },
  )
}

export async function updateConversation(conversationId: string, request: ConversationUpdateRequest) {
  return apiOrMock<ConversationSummary>(
    () => apiClient.patch<ApiEnvelope<ConversationSummary>>(`/api/conversations/${conversationId}`, request),
    () => {
      let updatedConversation: ConversationSummary | null = null
      Object.keys(mockWorkspace.conversationsByProject).forEach((projectId) => {
        const conversations = readConversations(projectId).map((conversation) => {
          if (conversation.id !== conversationId) return conversation
          updatedConversation = {
            ...conversation,
            title: request.title && request.title.trim() ? request.title : conversation.title,
            status: request.status ?? conversation.status,
            summary: request.summary ?? conversation.summary,
            updatedAt: nowIso(),
          }
          return updatedConversation
        })
        writeConversations(projectId, conversations.filter(Boolean) as ConversationSummary[])
      })
      if (!updatedConversation) {
        throw new Error(`Unknown conversation: ${conversationId}`)
      }
      return clone(updatedConversation)
    },
  )
}

export async function sendMessage(conversationId: string, request: MessageCreateRequest) {
  return apiOrMock<MessageRecord>(
    () => apiClient.post<ApiEnvelope<MessageRecord>>(`/api/messages/${conversationId}`, request, { timeoutMs: 90000 }),
    () => {
      let owningProjectId: string | null = null
      let conversationSummary: ConversationSummary | undefined

      for (const [projectId, conversations] of Object.entries(mockWorkspace.conversationsByProject)) {
        const current = conversations.find((item) => item.id === conversationId)
        if (current) {
          owningProjectId = projectId
          conversationSummary = current
          break
        }
      }

      if (!owningProjectId || !conversationSummary) {
        throw new Error(`Unknown conversation: ${conversationId}`)
      }

      const message: MessageRecord = {
        id: createId('message'),
        conversationId,
        projectId: owningProjectId,
        role: request.role,
        content: request.content,
        createdAt: nowIso(),
      }

      writeMessages(conversationId, [...readMessages(conversationId), message])
      if (request.role === 'user') {
        const assistantMessage: MessageRecord = {
          id: createId('message'),
          conversationId,
          projectId: owningProjectId,
          role: 'assistant',
          content: 'Mock assistant 已收到消息。连接真实后端后，这里会显示真实 LLM 的回复。',
          createdAt: nowIso(),
        }
        writeMessages(conversationId, [...readMessages(conversationId), assistantMessage])
      }
      const updatedConversation: ConversationSummary = {
        ...conversationSummary,
        summary: conversationSummary.summary || request.content.slice(0, 120),
        updatedAt: nowIso(),
      }
      writeConversations(
        owningProjectId,
        readConversations(owningProjectId).map((conversation) =>
          conversation.id === conversationId ? updatedConversation : conversation,
        ),
      )
      return clone(message)
    },
  )
}

export async function createPlan(request: PlanCreateRequest) {
  return apiOrMock<PlanRecord>(
    () => apiClient.post<ApiEnvelope<PlanRecord>>('/api/plans', request),
    () => {
      const steps =
        request.steps?.map((step, index) => ({
          id: createId('step'),
          title: step.title,
          description: step.description,
          status: 'pending' as const,
          result: '',
          sortOrder: step.sortOrder ?? index + 1,
        })) ?? [
          {
            id: createId('step'),
            title: '理解目标',
            description: request.goal,
            status: 'pending' as const,
            result: '',
            sortOrder: 1,
          },
          {
            id: createId('step'),
            title: '收集上下文',
            description: '整理项目、会话、记忆和节点信息',
            status: 'pending' as const,
            result: '',
            sortOrder: 2,
          },
          {
            id: createId('step'),
            title: '执行并沉淀',
            description: '完成任务并输出资产与记录',
            status: 'pending' as const,
            result: '',
            sortOrder: 3,
          },
        ]

      const plan: PlanRecord = {
        id: createId('plan'),
        projectId: request.projectId,
        conversationId: request.conversationId,
        goal: request.goal,
        constraints: request.constraints,
        status: 'draft',
        approvalRequired: request.approvalRequired,
        steps,
        executionResult: null,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      }

      writePlans(request.projectId, [plan, ...readPlans(request.projectId)])
      return clone(plan)
    },
  )
}

export async function approvePlan(planId: string) {
  return apiOrMock<PlanRecord>(
    () => apiClient.post<ApiEnvelope<PlanRecord>>(`/api/plans/${planId}/approve`),
    () => mutatePlan(planId, (plan) => ({ ...plan, status: 'approved', updatedAt: nowIso() })),
  )
}

export async function runPlan(planId: string) {
  return apiOrMock<PlanRecord>(
    () => apiClient.post<ApiEnvelope<PlanRecord>>(`/api/plans/${planId}/run`),
    () =>
      mutatePlan(planId, (plan) => ({
        ...plan,
        status: 'running',
        steps: plan.steps.map((step) => ({ ...step, status: 'running' })),
        updatedAt: nowIso(),
      })),
  )
}

export async function completePlan(planId: string, summary: string) {
  return apiOrMock<PlanRecord>(
    () =>
      apiClient.post<ApiEnvelope<PlanRecord>>(`/api/plans/${planId}/complete`, {
        summary,
        outputAssetIds: [],
        logs: [],
      }),
    () =>
      mutatePlan(planId, (plan) => ({
        ...plan,
        status: 'completed',
        steps: plan.steps.map((step) => ({ ...step, status: 'completed', result: step.result || '已完成' })),
        executionResult: {
          summary,
          outputAssetIds: [],
          logs: ['已在 mock 模式下完成计划。'],
        },
        updatedAt: nowIso(),
      })),
  )
}

function mutatePlan(planId: string, updater: (plan: PlanRecord) => PlanRecord) {
  let nextPlan: PlanRecord | null = null
  Object.keys(mockWorkspace.plansByProject).forEach((projectId) => {
    const plans = readPlans(projectId).map((plan) => {
      if (plan.id !== planId) return plan
      nextPlan = updater(plan)
      return nextPlan
    })
    writePlans(projectId, plans)
  })

  if (!nextPlan) {
    throw new Error(`Unknown plan: ${planId}`)
  }

  return clone(nextPlan)
}

export async function updateWorkspaceSettings(request: WorkspaceSettingsUpdateRequest) {
  return apiOrMock<WorkspaceSettings>(
    () => apiClient.put<ApiEnvelope<WorkspaceSettings>>('/api/settings/workspace', request),
    () => {
      mockWorkspace.bootstrap.workspaceSettings = {
        ...mockWorkspace.bootstrap.workspaceSettings,
        ...request,
        model: request.model ?? mockWorkspace.bootstrap.workspaceSettings.model,
        vault: request.vault ?? mockWorkspace.bootstrap.workspaceSettings.vault,
        nodes: request.nodes ?? mockWorkspace.bootstrap.workspaceSettings.nodes,
        appearance: request.appearance ?? mockWorkspace.bootstrap.workspaceSettings.appearance,
        enabledCommands: request.enabledCommands ?? mockWorkspace.bootstrap.workspaceSettings.enabledCommands,
        enabledSkills: request.enabledSkills ?? mockWorkspace.bootstrap.workspaceSettings.enabledSkills,
        updatedAt: nowIso(),
      }
      return clone(mockWorkspace.bootstrap.workspaceSettings)
    },
  )
}

export async function executeCommand(request: CommandExecutionRequest) {
  return apiOrMock<CommandExecutionResult>(
    () => apiClient.post<ApiEnvelope<CommandExecutionResult>>('/api/commands/execute', request),
    async () => {
      switch (request.commandId) {
        case '/plan': {
          const goal = request.args?.goal ?? '整理当前项目的执行计划'
          const conversationId = request.conversationId ?? request.args?.conversationId
          if (!conversationId) {
            return {
              commandId: request.commandId,
              message: '当前命令需要先选中一个会话。',
            }
          }
          const plan = await createPlan({
            projectId: request.projectId,
            conversationId,
            goal,
            constraints: request.args?.constraints?.split('|') ?? [],
            approvalRequired: true,
          })
          return {
            commandId: request.commandId,
            message: '计划草案已生成。',
            planId: plan.id,
          }
        }
        case '/save-card': {
          const title = request.args?.title ?? '新建知识卡片'
          const asset: AssetRecord = {
            id: createId('asset'),
            projectId: request.projectId,
            type: 'knowledge_card',
            title,
            contentRef: `markdown://${title}`,
            storagePath: `/vault/mock/${title}.md`,
            sourceConversationId: request.conversationId ?? null,
            sourcePlanId: request.args?.planId ?? null,
            sourceNodeId: null,
            tags: ['manual'],
            createdAt: nowIso(),
          }
          writeAssets(request.projectId, [asset, ...readAssets(request.projectId)])
          return {
            commandId: request.commandId,
            message: `${title} 已写入资产库。`,
            assetId: asset.id,
          }
        }
        case '/memory-save': {
          const memory: MemoryRecord = {
            id: createId('memory'),
            scope: request.args?.scope === 'global' ? 'global' : 'project',
            projectId: request.args?.scope === 'global' ? null : request.projectId,
            title: request.args?.title ?? '新建记忆',
            content: request.args?.content ?? '',
            priority: 80,
            status: 'active',
            sourceType: 'command',
            sourceRef: request.commandId,
            createdAt: nowIso(),
            updatedAt: nowIso(),
          }
          writeMemories(request.projectId, [memory, ...readMemories(request.projectId)])
          return {
            commandId: request.commandId,
            message: '记忆已保存。',
            memoryIds: [memory.id],
          }
        }
        case '/project-new': {
          await createProject({
            name: request.args?.name ?? '未命名项目',
            type: 'knowledge',
            description: request.args?.description ?? '',
          })
          return {
            commandId: request.commandId,
            message: '项目已创建。',
            memoryIds: [],
            assetId: undefined,
            planId: undefined,
          }
        }
        case '/plan-run':
          if (request.args?.planId) {
            await approvePlan(request.args.planId)
            await runPlan(request.args.planId)
            await completePlan(request.args.planId, '命令面板已完成计划执行。')
          }
          return {
            commandId: request.commandId,
            message: '计划已执行。',
            planId: request.args?.planId,
          }
        case '/node-status':
          return {
            commandId: request.commandId,
            message: `当前共有 ${mockWorkspace.bootstrap.nodes.length} 个节点。`,
          }
        case '/skill-list':
          return {
            commandId: request.commandId,
            message: `当前启用了 ${mockWorkspace.bootstrap.skills.length} 个技能。`,
          }
        case '/memory-show':
          return {
            commandId: request.commandId,
            message: `当前项目可见记忆 ${readMemories(request.projectId).length} 条。`,
          }
        case '/project-switch':
          return {
            commandId: request.commandId,
            message: '项目上下文已切换。',
          }
        case '/project-status':
          return {
            commandId: request.commandId,
            message: '项目状态摘要已准备好。',
          }
        case '/logs':
          return {
            commandId: request.commandId,
            message: '最近执行日志已整理。',
          }
        default:
          return {
            commandId: request.commandId,
            message: '命令已记录。',
          }
      }
    },
  )
}
