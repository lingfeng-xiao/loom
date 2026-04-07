import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type {
  CommandExecutionRequest,
  CommandId,
  ConversationSummary,
  ProjectSummary,
  WorkspaceSettings,
} from '@loom/contracts'
import { matchPath, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Archive, BrainCircuit, ChevronRight, Command, FolderKanban, FolderTree, Home, LayoutPanelLeft, ListTodo, MessageSquareText, PanelsRightBottom, PencilLine, Plus, Search, Server, Settings2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { AssetsLibrary } from '@/features/assets/assets-library'
import { ChatWorkspace } from '@/features/conversations/chat-workspace'
import { MemoryCenter } from '@/features/memory/memory-center'
import { NodesCenter } from '@/features/nodes/nodes-center'
import { PlanWorkspace } from '@/features/plans/plan-workspace'
import { ProjectHome } from '@/features/project-home/project-home'
import { SettingsCenter } from '@/features/settings/settings-center'
import {
  approvePlan,
  completePlan,
  createConversation,
  createPlan,
  createProject,
  executeCommand,
  fetchBootstrap,
  fetchConversationMessages,
  fetchNodes,
  fetchProjectAssets,
  fetchProjectConversations,
  fetchProjectMemories,
  fetchProjectPlans,
  fetchWorkspaceSettings,
  runPlan,
  sendMessage,
  updateConversation,
  updateProject,
  updateWorkspaceSettings,
} from '@/features/workspace/data'
import { DividerLabel, Panel, StatusPill, labelConversationMode, labelConversationStatus, labelProjectType, relativeTime, toneForStatus } from '@/features/workspace/presentation'
import { getInspectorOpen, getLastConversationId, setInspectorOpen, setLastConversationId } from '@/features/workspace/storage'

const commandCatalog: Record<CommandId, { label: string; description: string }> = {
  '/project-new': { label: '新建项目', description: '创建新的项目工作区。' },
  '/project-switch': { label: '切换项目', description: '切换当前项目上下文。' },
  '/project-status': { label: '项目状态', description: '查看项目摘要、节点和资产状态。' },
  '/plan': { label: '生成 Plan', description: '为当前会话生成结构化执行计划。' },
  '/plan-run': { label: '执行 Plan', description: '批准并执行指定的计划。' },
  '/save-card': { label: '沉淀资产', description: '将当前内容写入资产库。' },
  '/memory-show': { label: '查看记忆', description: '查看当前项目可见的记忆条目。' },
  '/memory-save': { label: '保存记忆', description: '保存一条新的全局或项目记忆。' },
  '/skill-list': { label: '技能列表', description: '查看当前启用的技能。' },
  '/node-status': { label: '节点状态', description: '查看节点心跳、快照和服务状态。' },
  '/logs': { label: '最近日志', description: '查看最近的执行与审计记录。' },
}

type PaneView = 'home' | 'chat' | 'plan' | 'memory' | 'assets' | 'nodes' | 'settings'

type NewConversationDraft = {
  title: string
  summary: string
}

type NewPlanDraft = {
  title: string
  goal: string
  constraints: string
}

type NewProjectDraft = {
  name: string
  description: string
  type: ProjectSummary['type']
}

type RenameDraft = {
  id: string
  title: string
  summary: string
}

export function WorkspaceShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { projectId: routeProjectId = '' } = useParams()

  const [conversationSearch, setConversationSearch] = useState('')
  const [conversationModeFilter, setConversationModeFilter] = useState<'all' | 'chat' | 'plan'>('all')
  const [showArchived, setShowArchived] = useState(false)
  const [feedback, setFeedback] = useState('新的工作台已切换为项目优先模式，支持多轮会话、Plan、记忆、资产和设置中心。')
  const [inspectorOpen, setInspectorOpenState] = useState(true)
  const [commandDialogOpen, setCommandDialogOpen] = useState(false)
  const [newProjectOpen, setNewProjectOpen] = useState(false)
  const [newChatOpen, setNewChatOpen] = useState(false)
  const [newPlanOpen, setNewPlanOpen] = useState(false)
  const [renameDraft, setRenameDraft] = useState<RenameDraft | null>(null)
  const [commandInput, setCommandInput] = useState('/plan')
  const [newConversationDraft, setNewConversationDraft] = useState<NewConversationDraft>({ title: '', summary: '' })
  const [newPlanDraft, setNewPlanDraft] = useState<NewPlanDraft>({ title: '', goal: '', constraints: '' })
  const [newProjectDraft, setNewProjectDraft] = useState<NewProjectDraft>({ name: '', description: '', type: 'knowledge' })

  const bootstrapQuery = useQuery({ queryKey: ['bootstrap'], queryFn: fetchBootstrap })
  const settingsQuery = useQuery({
    queryKey: ['workspace-settings'],
    queryFn: fetchWorkspaceSettings,
    initialData: bootstrapQuery.data?.workspaceSettings,
  })

  const project = useMemo(
    () => bootstrapQuery.data?.projects.find((item) => item.id === routeProjectId) ?? bootstrapQuery.data?.projects[0] ?? null,
    [bootstrapQuery.data?.projects, routeProjectId],
  )

  const conversationsQuery = useQuery({
    queryKey: ['conversations', project?.id],
    queryFn: () => fetchProjectConversations(project!.id),
    enabled: !!project,
  })
  const plansQuery = useQuery({ queryKey: ['plans', project?.id], queryFn: () => fetchProjectPlans(project!.id), enabled: !!project })
  const memoriesQuery = useQuery({ queryKey: ['memories', project?.id], queryFn: () => fetchProjectMemories(project!.id), enabled: !!project })
  const assetsQuery = useQuery({ queryKey: ['assets', project?.id], queryFn: () => fetchProjectAssets(project!.id), enabled: !!project })
  const nodesQuery = useQuery({ queryKey: ['nodes'], queryFn: fetchNodes })

  const chatMatch = matchPath('/projects/:projectId/chat/:conversationId', location.pathname)
  const planMatch = matchPath('/projects/:projectId/plans/:planId', location.pathname)

  const currentView: PaneView = useMemo(() => {
    if (location.pathname.endsWith('/memory')) return 'memory'
    if (location.pathname.endsWith('/assets')) return 'assets'
    if (location.pathname.endsWith('/nodes')) return 'nodes'
    if (location.pathname.endsWith('/settings')) return 'settings'
    if (planMatch) return 'plan'
    if (chatMatch) return 'chat'
    return 'home'
  }, [chatMatch, location.pathname, planMatch])

  const selectedConversationId = chatMatch?.params.conversationId ?? null
  const selectedPlanId = planMatch?.params.planId ?? null
  const conversations = conversationsQuery.data ?? []
  const plans = plansQuery.data ?? []
  const memories = memoriesQuery.data ?? []
  const assets = assetsQuery.data ?? []
  const nodes = nodesQuery.data ?? bootstrapQuery.data?.nodes ?? []
  const settings = settingsQuery.data ?? bootstrapQuery.data?.workspaceSettings ?? null

  const selectedPlan = plans.find((item) => item.id === selectedPlanId) ?? null
  const selectedConversation =
    conversations.find((item) => item.id === selectedConversationId) ??
    (selectedPlan ? conversations.find((item) => item.id === selectedPlan.conversationId) ?? null : null)

  const messagesQuery = useQuery({
    queryKey: ['messages', selectedConversation?.id],
    queryFn: () => fetchConversationMessages(selectedConversation!.id),
    enabled: !!selectedConversation,
  })

  const messages = messagesQuery.data ?? []

  const visibleConversations = useMemo(
    () =>
      conversations
        .filter((conversation) => (showArchived ? true : conversation.status !== 'archived'))
        .filter((conversation) => (conversationModeFilter === 'all' ? true : conversation.mode === conversationModeFilter))
        .filter((conversation) => {
          const needle = conversationSearch.trim().toLowerCase()
          if (!needle) return true
          return conversation.title.toLowerCase().includes(needle) || conversation.summary.toLowerCase().includes(needle)
        })
        .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt)),
    [conversations, conversationModeFilter, conversationSearch, showArchived],
  )

  const recentChats = visibleConversations.filter((conversation) => conversation.status === 'active' && conversation.mode === 'chat')
  const recentPlans = visibleConversations.filter((conversation) => conversation.status === 'active' && conversation.mode === 'plan')
  const archivedConversations = conversations.filter((conversation) => conversation.status === 'archived')
  const projectNodes = useMemo(() => {
    if (!project) return nodes
    if (project.boundNodeIds.length === 0) return nodes
    return nodes.filter((node) => project.boundNodeIds.includes(node.id))
  }, [nodes, project])
  const enabledSkills = useMemo(() => {
    const skillIds = settings?.enabledSkills ?? bootstrapQuery.data?.skills.map((skill) => skill.id) ?? []
    return (bootstrapQuery.data?.skills ?? []).filter((skill) => skillIds.includes(skill.id))
  }, [bootstrapQuery.data?.skills, settings?.enabledSkills])
  const enabledCommands = useMemo(() => {
    const ids = settings?.enabledCommands ?? bootstrapQuery.data?.commands ?? []
    return ids.filter((id) => bootstrapQuery.data?.commands.includes(id))
  }, [bootstrapQuery.data?.commands, settings?.enabledCommands])

  useEffect(() => {
    if (settings) {
      setInspectorOpenState(getInspectorOpen(settings.inspectorDefaultOpen))
    }
  }, [settings])

  useEffect(() => {
    if (selectedConversation?.id && project?.id) {
      setLastConversationId(project.id, selectedConversation.id)
    }
  }, [project?.id, selectedConversation?.id])

  useEffect(() => {
    if (!bootstrapQuery.data || !project) return
    const projectPath = `/projects/${project.id}`
    if (location.pathname !== projectPath && location.pathname !== `${projectPath}/`) return
    const preferredConversationId =
      settings?.defaultLandingView === 'project_home' ? null : getLastConversationId(project.id) ?? conversations[0]?.id ?? null
    navigate(preferredConversationId ? `${projectPath}/chat/${preferredConversationId}` : `${projectPath}/home`, { replace: true })
  }, [bootstrapQuery.data, conversations, location.pathname, navigate, project, settings?.defaultLandingView])

  useEffect(() => {
    if (!bootstrapQuery.data) return
    if (routeProjectId && project) return
    const fallbackProjectId = settings?.defaultProjectId ?? bootstrapQuery.data.defaultProjectId ?? bootstrapQuery.data.projects[0]?.id
    if (fallbackProjectId) {
      navigate(`/projects/${fallbackProjectId}`, { replace: true })
    }
  }, [bootstrapQuery.data, navigate, project, routeProjectId, settings?.defaultProjectId])

  const createProjectMutation = useMutation({
    mutationFn: createProject,
    onSuccess: async (createdProject) => {
      await queryClient.invalidateQueries({ queryKey: ['bootstrap'] })
      setFeedback(`项目“${createdProject.name}”已创建。`)
      setNewProjectOpen(false)
      setNewProjectDraft({ name: '', description: '', type: 'knowledge' })
      navigate(`/projects/${createdProject.id}/home`)
    },
  })

  const createConversationMutation = useMutation({
    mutationFn: ({
      currentProjectId,
      title,
      summary,
      mode,
    }: {
      currentProjectId: string
      title: string
      summary: string
      mode: 'chat' | 'plan'
    }) => createConversation(currentProjectId, { title, summary, mode }),
    onSuccess: async (conversation) => {
      await queryClient.invalidateQueries({ queryKey: ['conversations', conversation.projectId] })
      setLastConversationId(conversation.projectId, conversation.id)
      setFeedback(`已创建会话“${conversation.title}”。`)
      navigate(`/projects/${conversation.projectId}/chat/${conversation.id}`)
    },
  })

  const createPlanMutation = useMutation({
    mutationFn: createPlan,
    onSuccess: async (plan) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['plans', plan.projectId] }),
        queryClient.invalidateQueries({ queryKey: ['conversations', plan.projectId] }),
      ])
      setFeedback('Plan 已生成。')
      setNewPlanOpen(false)
      setNewPlanDraft({ title: '', goal: '', constraints: '' })
      navigate(`/projects/${plan.projectId}/plans/${plan.id}`)
    },
  })

  const renameConversationMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: { title?: string; summary?: string; status?: 'active' | 'archived' } }) =>
      updateConversation(id, payload),
    onSuccess: async (conversation) => {
      await queryClient.invalidateQueries({ queryKey: ['conversations', conversation.projectId] })
      setFeedback(`会话“${conversation.title}”已更新。`)
      setRenameDraft(null)
    },
  })

  const sendMessageMutation = useMutation({
    mutationFn: ({ conversationId, content }: { conversationId: string; content: string }) =>
      sendMessage(conversationId, { role: 'user', content }),
    onSuccess: async () => {
      if (!selectedConversation?.id || !project?.id) return
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['messages', selectedConversation.id] }),
        queryClient.invalidateQueries({ queryKey: ['conversations', project.id] }),
      ])
      setFeedback('消息已追加到当前会话。')
    },
  })

  const approvePlanMutation = useMutation({
    mutationFn: approvePlan,
    onSuccess: async () => {
      if (!project?.id) return
      await queryClient.invalidateQueries({ queryKey: ['plans', project.id] })
      setFeedback('Plan 已批准。')
    },
  })

  const runPlanMutation = useMutation({
    mutationFn: runPlan,
    onSuccess: async () => {
      if (!project?.id) return
      await queryClient.invalidateQueries({ queryKey: ['plans', project.id] })
      setFeedback('Plan 已进入执行状态。')
    },
  })

  const completePlanMutation = useMutation({
    mutationFn: ({ planId, summary }: { planId: string; summary: string }) => completePlan(planId, summary),
    onSuccess: async () => {
      if (!project?.id) return
      await queryClient.invalidateQueries({ queryKey: ['plans', project.id] })
      setFeedback('Plan 已完成。')
    },
  })

  const commandMutation = useMutation({
    mutationFn: executeCommand,
    onSuccess: async (result, request) => {
      if (request.projectId) {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ['conversations', request.projectId] }),
          queryClient.invalidateQueries({ queryKey: ['plans', request.projectId] }),
          queryClient.invalidateQueries({ queryKey: ['assets', request.projectId] }),
          queryClient.invalidateQueries({ queryKey: ['memories', request.projectId] }),
        ])
      }
      setFeedback(result.message)
      setCommandDialogOpen(false)
      if (result.planId && request.projectId) navigate(`/projects/${request.projectId}/plans/${result.planId}`)
      if (result.assetId && request.projectId) navigate(`/projects/${request.projectId}/assets`)
    },
  })

  const updateWorkspaceMutation = useMutation({
    mutationFn: updateWorkspaceSettings,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['workspace-settings'] }),
        queryClient.invalidateQueries({ queryKey: ['bootstrap'] }),
      ])
      setFeedback('工作区设置已保存。')
    },
  })

  const updateProjectMutation = useMutation({
    mutationFn: ({ projectId, nextProject }: { projectId: string; nextProject: ProjectSummary }) =>
      updateProject(projectId, {
        name: nextProject.name,
        type: nextProject.type,
        description: nextProject.description,
        defaultSkills: nextProject.defaultSkills,
        defaultCommands: nextProject.defaultCommands,
        boundNodeIds: nextProject.boundNodeIds,
        knowledgeRoots: nextProject.knowledgeRoots,
        projectMemoryRefs: nextProject.projectMemoryRefs ?? [],
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['bootstrap'] })
      setFeedback('项目默认项已保存。')
    },
  })

  function switchProject(projectId: string) {
    navigate(`/projects/${projectId}`)
  }

  function openConversation(conversationId: string) {
    if (!project) return
    navigate(`/projects/${project.id}/chat/${conversationId}`)
  }

  function openPlan(planId: string) {
    if (!project) return
    navigate(`/projects/${project.id}/plans/${planId}`)
  }

  function openView(view: PaneView) {
    if (!project) return
    const base = `/projects/${project.id}`
    const destinations: Record<PaneView, string> = {
      home: `${base}/home`,
      chat: selectedConversation ? `${base}/chat/${selectedConversation.id}` : `${base}/home`,
      plan: selectedPlan ? `${base}/plans/${selectedPlan.id}` : `${base}/home`,
      memory: `${base}/memory`,
      assets: `${base}/assets`,
      nodes: `${base}/nodes`,
      settings: `${base}/settings`,
    }
    navigate(destinations[view])
  }

  async function handleCreateChat() {
    if (!project) return
    await createConversationMutation.mutateAsync({
      currentProjectId: project.id,
      title: newConversationDraft.title || `新建 Chat ${new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`,
      summary: newConversationDraft.summary,
      mode: 'chat',
    })
    setNewChatOpen(false)
    setNewConversationDraft({ title: '', summary: '' })
  }

  async function handleCreatePlan() {
    if (!project) return
    const planConversation = await createConversationMutation.mutateAsync({
      currentProjectId: project.id,
      title: newPlanDraft.title || '新的 Plan 会话',
      summary: newPlanDraft.goal,
      mode: 'plan',
    })
    await createPlanMutation.mutateAsync({
      projectId: project.id,
      conversationId: planConversation.id,
      goal: newPlanDraft.goal || newPlanDraft.title || '整理当前项目执行路径',
      constraints: newPlanDraft.constraints.split('|').map((item) => item.trim()).filter(Boolean),
      approvalRequired: true,
    })
  }

  async function handlePromoteToPlan() {
    if (!project || !selectedConversation) return
    const existingPlan = plans.find((plan) => plan.conversationId === selectedConversation.id)
    if (existingPlan) {
      navigate(`/projects/${project.id}/plans/${existingPlan.id}`)
      return
    }
    await createPlanMutation.mutateAsync({
      projectId: project.id,
      conversationId: selectedConversation.id,
      goal: selectedConversation.title,
      constraints: [],
      approvalRequired: true,
    })
  }

  async function handleExecuteCommand() {
    if (!project) return
    const request = parseCommand(commandInput, project.id, selectedConversation?.id ?? undefined)
    if (!request) {
      setFeedback('命令必须以 / 开头，参数格式为 key=value。')
      return
    }
    await commandMutation.mutateAsync(request)
  }

  if (!bootstrapQuery.data || !project || !settings) {
    return <div className="flex h-screen items-center justify-center bg-slate-100 text-sm text-slate-600">正在加载项目级工作台...</div>
  }

  return (
    <>
      <div className={inspectorOpen ? 'grid h-screen w-screen grid-cols-[72px_320px_minmax(0,1fr)_360px] overflow-hidden bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] text-slate-900' : 'grid h-screen w-screen grid-cols-[72px_320px_minmax(0,1fr)] overflow-hidden bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] text-slate-900'}>
        <GlobalRail activeView={currentView} onOpenView={openView} onOpenCommand={() => setCommandDialogOpen(true)} onToggleInspector={() => { const next = !inspectorOpen; setInspectorOpenState(next); setInspectorOpen(next) }} inspectorOpen={inspectorOpen} />
        <ProjectPane project={project} projects={bootstrapQuery.data.projects} conversations={{ chats: recentChats, plans: recentPlans, archived: archivedConversations }} search={conversationSearch} onSearch={setConversationSearch} modeFilter={conversationModeFilter} onModeFilterChange={setConversationModeFilter} showArchived={showArchived} onShowArchivedChange={setShowArchived} selectedConversationId={selectedConversation?.id ?? null} onSwitchProject={switchProject} onOpenConversation={openConversation} onCreateProject={() => setNewProjectOpen(true)} onCreateChat={() => setNewChatOpen(true)} onCreatePlan={() => setNewPlanOpen(true)} onRenameConversation={(conversation) => setRenameDraft({ id: conversation.id, title: conversation.title, summary: conversation.summary })} onArchiveConversation={(conversation) => void renameConversationMutation.mutateAsync({ id: conversation.id, payload: { status: conversation.status === 'archived' ? 'active' : 'archived' } })} />
        <main className="flex min-h-0 min-w-0 flex-col border-l border-r border-slate-200/80 bg-white/80">
          <header className="border-b border-slate-200/80 bg-white/85 px-7 py-5 backdrop-blur">
            <div className="flex items-start justify-between gap-6">
              <div className="min-w-0">
                <div className="flex items-center gap-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  <span>{pageLabel(currentView)}</span>
                  <StatusPill label={labelProjectType(project.type)} tone={toneForStatus(project.type)} />
                  <span>更新时间 {relativeTime(project.updatedAt)}</span>
                </div>
                <h1 className="mt-3 text-[28px] font-semibold tracking-tight text-slate-950">{pageTitle(currentView, project, selectedConversation, selectedPlan)}</h1>
                <p className="mt-2 max-w-4xl text-sm leading-6 text-slate-600">{pageDescription(currentView, project, selectedConversation, selectedPlan)}</p>
              </div>
              <div className="grid min-w-[300px] gap-3">
                <div className="rounded-2xl border border-cyan-200 bg-cyan-50/80 px-4 py-3 text-sm text-cyan-950">{feedback}</div>
              </div>
            </div>
          </header>
          <div className="min-h-0 flex-1 overflow-y-auto px-7 py-6">
            {currentView === 'home' ? <ProjectHome project={project} conversations={conversations} plans={plans} memories={memories} assets={assets} nodes={projectNodes} commands={enabledCommands} onOpenConversation={openConversation} onOpenPlan={openPlan} onOpenSettings={() => openView('settings')} /> : null}
            {currentView === 'chat' ? <ChatWorkspace project={project} conversation={selectedConversation} messages={messages} relatedPlans={plans.filter((plan) => plan.conversationId === selectedConversation?.id)} onSendMessage={async (content) => { if (!selectedConversation) return; await sendMessageMutation.mutateAsync({ conversationId: selectedConversation.id, content }) }} onPromoteToPlan={handlePromoteToPlan} onOpenPlan={openPlan} /> : null}
            {currentView === 'plan' ? <PlanWorkspace plan={selectedPlan} assets={assets} onApprove={async () => { if (selectedPlan) await approvePlanMutation.mutateAsync(selectedPlan.id) }} onRun={async () => { if (selectedPlan) await runPlanMutation.mutateAsync(selectedPlan.id) }} onComplete={async () => { if (selectedPlan) await completePlanMutation.mutateAsync({ planId: selectedPlan.id, summary: `${selectedPlan.goal} 已完成，结果已记录。` }) }} /> : null}
            {currentView === 'memory' ? <MemoryCenter memories={memories} onSaveTemplate={async (scope) => { await commandMutation.mutateAsync({ commandId: '/memory-save', projectId: project.id, conversationId: selectedConversation?.id, args: { scope, title: scope === 'global' ? '新的全局记忆' : '新的项目记忆', content: scope === 'global' ? '用于所有项目的偏好设置。' : '用于当前项目的长期上下文。' } }) }} /> : null}
            {currentView === 'assets' ? <AssetsLibrary assets={assets} conversations={conversations} plans={plans} /> : null}
            {currentView === 'nodes' ? <NodesCenter nodes={projectNodes} centerNodeLabel={settings.nodes.centerNodeLabel} /> : null}
            {currentView === 'settings' ? <SettingsCenter project={project} settings={settings} skills={enabledSkills} nodes={nodes} commandCatalog={enabledCommands} onSaveWorkspace={async (draft) => { await updateWorkspaceMutation.mutateAsync(draft) }} onSaveProjectDefaults={async (draft) => { await updateProjectMutation.mutateAsync({ projectId: draft.id, nextProject: draft }) }} /> : null}
          </div>
        </main>
        {inspectorOpen ? <ContextInspector project={project} settings={settings} selectedConversation={selectedConversation} selectedPlan={selectedPlan} memories={memories} assets={assets} nodes={projectNodes} skills={enabledSkills} /> : null}
      </div>
      <CommandPalette open={commandDialogOpen} onOpenChange={setCommandDialogOpen} input={commandInput} onInputChange={setCommandInput} availableCommands={enabledCommands} onExecute={() => void handleExecuteCommand()} />
      <CreateProjectDialog open={newProjectOpen} onOpenChange={setNewProjectOpen} draft={newProjectDraft} onChange={setNewProjectDraft} onCreate={() => void createProjectMutation.mutateAsync(newProjectDraft)} />
      <CreateChatDialog open={newChatOpen} onOpenChange={setNewChatOpen} draft={newConversationDraft} onChange={setNewConversationDraft} onCreate={() => void handleCreateChat()} />
      <CreatePlanDialog open={newPlanOpen} onOpenChange={setNewPlanOpen} draft={newPlanDraft} onChange={setNewPlanDraft} onCreate={() => void handleCreatePlan()} />
      <RenameConversationDialog draft={renameDraft} onOpenChange={(open) => { if (!open) setRenameDraft(null) }} onChange={setRenameDraft} onSave={() => { if (!renameDraft) return; void renameConversationMutation.mutateAsync({ id: renameDraft.id, payload: { title: renameDraft.title, summary: renameDraft.summary } }) }} />
    </>
  )
}

function parseCommand(input: string, projectId: string, conversationId?: string): CommandExecutionRequest | null {
  const [commandId, ...tokens] = input.trim().split(/\s+/)
  if (!commandId.startsWith('/')) return null
  const args: Record<string, string> = {}
  for (const token of tokens) {
    const [key, ...rest] = token.split('=')
    if (key && rest.length > 0) args[key] = rest.join('=')
  }
  if (conversationId) args.conversationId ??= conversationId
  return { commandId: commandId as CommandId, projectId, conversationId, args }
}

function pageLabel(view: PaneView) {
  return { home: 'Project Home', chat: 'Conversation Workspace', plan: 'Plan Workspace', memory: 'Memory Center', assets: 'Assets Library', nodes: 'Nodes Center', settings: 'Settings Center' }[view]
}

function pageTitle(view: PaneView, project: ProjectSummary, conversation: ConversationSummary | null, plan: { goal: string } | null) {
  if (view === 'chat' && conversation) return conversation.title
  if (view === 'plan' && plan) return plan.goal
  if (view === 'memory') return `${project.name} 的记忆中心`
  if (view === 'assets') return `${project.name} 的资产库`
  if (view === 'nodes') return `${project.name} 的节点状态`
  if (view === 'settings') return `${project.name} 的设置中心`
  return `${project.name} 项目首页`
}

function pageDescription(view: PaneView, project: ProjectSummary, conversation: ConversationSummary | null, plan: { goal: string } | null) {
  if (view === 'chat' && conversation) return conversation.summary || '围绕当前项目会话继续推进任务，支持多轮切换与升级到 Plan。'
  if (view === 'plan' && plan) return '把复杂任务拆成可批准、可执行、可沉淀的结构化流程。'
  if (view === 'memory') return '这里集中查看全局记忆和项目记忆，确保长期规则显式可见。'
  if (view === 'assets') return '这里汇总项目输出的正式资产，包括知识卡片、运维记录和结构化 Markdown。'
  if (view === 'nodes') return '这里只展示节点只读状态，用于心跳、快照和服务状态巡检。'
  if (view === 'settings') return '设置中心用于维护工作区、项目默认项、模型、Vault、节点策略和诊断信息。'
  return project.description
}

function GlobalRail({
  activeView,
  onOpenView,
  onOpenCommand,
  onToggleInspector,
  inspectorOpen,
}: {
  activeView: PaneView
  onOpenView: (view: PaneView) => void
  onOpenCommand: () => void
  onToggleInspector: () => void
  inspectorOpen: boolean
}) {
  const items: Array<{ id: PaneView; icon: typeof Home; label: string }> = [
    { id: 'home', icon: Home, label: '首页' },
    { id: 'chat', icon: MessageSquareText, label: 'Chat' },
    { id: 'plan', icon: ListTodo, label: 'Plan' },
    { id: 'memory', icon: BrainCircuit, label: '记忆' },
    { id: 'assets', icon: FolderTree, label: '资产' },
    { id: 'nodes', icon: Server, label: '节点' },
    { id: 'settings', icon: Settings2, label: '设置' },
  ]

  return (
    <aside className="flex h-full flex-col items-center justify-between border-r border-slate-200/80 bg-slate-950 px-3 py-4 text-slate-300">
      <div className="flex w-full flex-col items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-cyan-400/15 text-cyan-300">
          <FolderKanban className="h-5 w-5" />
        </div>
        {items.map((item) => (
          <button
            key={item.id}
            type="button"
            title={item.label}
            onClick={() => onOpenView(item.id)}
            className={
              activeView === item.id
                ? 'flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-slate-950 shadow'
                : 'flex h-12 w-12 items-center justify-center rounded-2xl text-slate-400 transition hover:bg-white/10 hover:text-white'
            }
          >
            <item.icon className="h-5 w-5" />
          </button>
        ))}
      </div>

      <div className="flex w-full flex-col items-center gap-3">
        <button
          type="button"
          title="命令面板"
          onClick={onOpenCommand}
          className="flex h-12 w-12 items-center justify-center rounded-2xl border border-slate-700 text-slate-300 transition hover:border-cyan-400 hover:text-cyan-300"
        >
          <Command className="h-5 w-5" />
        </button>
        <button
          type="button"
          title="切换 Inspector"
          onClick={onToggleInspector}
          className="flex h-12 w-12 items-center justify-center rounded-2xl border border-slate-700 text-slate-300 transition hover:border-cyan-400 hover:text-cyan-300"
        >
          {inspectorOpen ? <PanelsRightBottom className="h-5 w-5" /> : <LayoutPanelLeft className="h-5 w-5" />}
        </button>
      </div>
    </aside>
  )
}

function ProjectPane({
  project,
  projects,
  conversations,
  search,
  onSearch,
  modeFilter,
  onModeFilterChange,
  showArchived,
  onShowArchivedChange,
  selectedConversationId,
  onSwitchProject,
  onOpenConversation,
  onCreateProject,
  onCreateChat,
  onCreatePlan,
  onRenameConversation,
  onArchiveConversation,
}: {
  project: ProjectSummary
  projects: ProjectSummary[]
  conversations: {
    chats: ConversationSummary[]
    plans: ConversationSummary[]
    archived: ConversationSummary[]
  }
  search: string
  onSearch: (value: string) => void
  modeFilter: 'all' | 'chat' | 'plan'
  onModeFilterChange: (value: 'all' | 'chat' | 'plan') => void
  showArchived: boolean
  onShowArchivedChange: (value: boolean) => void
  selectedConversationId: string | null
  onSwitchProject: (projectId: string) => void
  onOpenConversation: (conversationId: string) => void
  onCreateProject: () => void
  onCreateChat: () => void
  onCreatePlan: () => void
  onRenameConversation: (conversation: ConversationSummary) => void
  onArchiveConversation: (conversation: ConversationSummary) => void
}) {
  return (
    <aside className="flex min-h-0 flex-col bg-white/92">
      <div className="border-b border-slate-200/80 px-5 py-5">
        <div className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">Project Switcher</div>
        <select
          className="mt-3 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-900 outline-none"
          value={project.id}
          onChange={(event) => onSwitchProject(event.target.value)}
        >
          {projects.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>

        <div className="mt-4 grid grid-cols-2 gap-2">
          <Button variant="outline" onClick={onCreateChat}>
            <Plus className="mr-2 h-4 w-4" />
            新建 Chat
          </Button>
          <Button onClick={onCreatePlan}>
            <Plus className="mr-2 h-4 w-4" />
            新建 Plan
          </Button>
        </div>
        <Button variant="ghost" className="mt-2 w-full justify-start rounded-2xl" onClick={onCreateProject}>
          <Plus className="mr-2 h-4 w-4" />
          创建新项目
        </Button>
      </div>

      <div className="border-b border-slate-200/80 px-5 py-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            value={search}
            onChange={(event) => onSearch(event.target.value)}
            placeholder="搜索当前项目会话"
            className="h-11 rounded-2xl border-slate-200 bg-slate-50 pl-10"
          />
        </div>
        <div className="mt-3 flex gap-2">
          {(['all', 'chat', 'plan'] as const).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => onModeFilterChange(item)}
              className={
                modeFilter === item
                  ? 'rounded-full bg-slate-900 px-3 py-1.5 text-xs font-medium text-white'
                  : 'rounded-full bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-600'
              }
            >
              {item === 'all' ? '全部' : item === 'chat' ? 'Chat' : 'Plan'}
            </button>
          ))}
          <button
            type="button"
            onClick={() => onShowArchivedChange(!showArchived)}
            className={
              showArchived
                ? 'rounded-full bg-amber-100 px-3 py-1.5 text-xs font-medium text-amber-800'
                : 'rounded-full bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-600'
            }
          >
            已归档
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-5">
        <ConversationGroup
          title="最近 Chat"
          items={conversations.chats}
          selectedConversationId={selectedConversationId}
          onOpenConversation={onOpenConversation}
          onRenameConversation={onRenameConversation}
          onArchiveConversation={onArchiveConversation}
        />
        <ConversationGroup
          title="最近 Plan"
          items={conversations.plans}
          selectedConversationId={selectedConversationId}
          onOpenConversation={onOpenConversation}
          onRenameConversation={onRenameConversation}
          onArchiveConversation={onArchiveConversation}
        />
        {showArchived ? (
          <ConversationGroup
            title="已归档"
            items={conversations.archived}
            selectedConversationId={selectedConversationId}
            onOpenConversation={onOpenConversation}
            onRenameConversation={onRenameConversation}
            onArchiveConversation={onArchiveConversation}
          />
        ) : null}
      </div>

      <div className="border-t border-slate-200/80 px-5 py-4">
        <div className="text-sm font-semibold text-slate-950">{project.name}</div>
        <div className="mt-1 text-sm leading-6 text-slate-600">{project.description}</div>
        <div className="mt-3 flex flex-wrap gap-2">
          <StatusPill label={labelProjectType(project.type)} tone={toneForStatus(project.type)} />
          <StatusPill label={`${project.boundNodeIds.length} Nodes`} tone="neutral" />
          <StatusPill label={`${project.defaultSkills.length} Skills`} tone="neutral" />
        </div>
      </div>
    </aside>
  )
}

function ConversationGroup({
  title,
  items,
  selectedConversationId,
  onOpenConversation,
  onRenameConversation,
  onArchiveConversation,
}: {
  title: string
  items: ConversationSummary[]
  selectedConversationId: string | null
  onOpenConversation: (conversationId: string) => void
  onRenameConversation: (conversation: ConversationSummary) => void
  onArchiveConversation: (conversation: ConversationSummary) => void
}) {
  return (
    <section className="space-y-3">
      <DividerLabel>{title}</DividerLabel>
      {items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-4 text-sm text-slate-500">
          当前分组还没有会话。
        </div>
      ) : (
        items.map((conversation) => (
          <div
            key={conversation.id}
            className={
              selectedConversationId === conversation.id
                ? 'rounded-2xl border border-cyan-200 bg-cyan-50/70 px-4 py-4'
                : 'rounded-2xl border border-slate-200 bg-white px-4 py-4'
            }
          >
            <button type="button" onClick={() => onOpenConversation(conversation.id)} className="w-full text-left">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="truncate text-sm font-semibold text-slate-950">{conversation.title}</div>
                  <div className="mt-1 flex flex-wrap items-center gap-2">
                    <StatusPill label={labelConversationMode(conversation.mode)} tone={toneForStatus(conversation.mode)} />
                    <StatusPill label={labelConversationStatus(conversation.status)} tone={toneForStatus(conversation.status)} />
                  </div>
                </div>
                <ChevronRight className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
              </div>
              <div className="mt-3 line-clamp-2 text-sm leading-6 text-slate-600">{conversation.summary || '暂无摘要'}</div>
            </button>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-slate-500">{relativeTime(conversation.updatedAt)}</span>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  title="重命名"
                  onClick={() => onRenameConversation(conversation)}
                  className="rounded-xl p-2 text-slate-500 transition hover:bg-white hover:text-slate-900"
                >
                  <PencilLine className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  title={conversation.status === 'archived' ? '恢复' : '归档'}
                  onClick={() => onArchiveConversation(conversation)}
                  className="rounded-xl p-2 text-slate-500 transition hover:bg-white hover:text-slate-900"
                >
                  <Archive className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        ))
      )}
    </section>
  )
}

function ContextInspector({
  project,
  settings,
  selectedConversation,
  selectedPlan,
  memories,
  assets,
  nodes,
  skills,
}: {
  project: ProjectSummary
  settings: WorkspaceSettings
  selectedConversation: ConversationSummary | null
  selectedPlan: { id: string; goal: string; status: string; steps: Array<{ title: string; sortOrder: number }> } | null
  memories: Array<{ id: string; title: string; content: string; scope: string; updatedAt: string }>
  assets: Array<{ id: string; title: string; storagePath: string; sourceConversationId: string | null; sourcePlanId: string | null }>
  nodes: Array<{ id: string; name: string; status: string; host: string }>
  skills: Array<{ id: string; name: string; version: string }>
}) {
  const relatedAssets = assets.filter(
    (asset) => asset.sourceConversationId === selectedConversation?.id || asset.sourcePlanId === selectedPlan?.id,
  )

  return (
    <aside className="min-h-0 overflow-y-auto bg-slate-50/85 px-4 py-5">
      <div className="space-y-4">
        <Panel eyebrow="Context" title="当前项目">
          <div className="space-y-3 text-sm text-slate-600">
            <MetaLine label="项目名称" value={project.name} />
            <MetaLine label="默认落点" value={settings.defaultLandingView === 'last_conversation' ? '上次会话' : '项目首页'} />
            <MetaLine label="知识根" value={project.knowledgeRoots.join(' / ') || '暂无'} />
          </div>
        </Panel>

        <Panel eyebrow="Memory" title="关键记忆">
          <div className="space-y-3">
            {memories.slice(0, 3).map((memory) => (
              <div key={memory.id} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div className="text-sm font-medium text-slate-950">{memory.title}</div>
                <div className="mt-2 text-sm leading-6 text-slate-600">{memory.content}</div>
              </div>
            ))}
          </div>
        </Panel>

        <Panel eyebrow="Assets" title="关联输出">
          <div className="space-y-3">
            {relatedAssets.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-5 text-sm text-slate-600">
                当前上下文还没有关联资产。
              </div>
            ) : (
              relatedAssets.map((asset) => (
                <div key={asset.id} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                  <div className="text-sm font-medium text-slate-950">{asset.title}</div>
                  <div className="mt-2 font-mono text-xs text-slate-500">{asset.storagePath}</div>
                </div>
              ))
            )}
          </div>
        </Panel>

        <Panel eyebrow="Plan" title="当前 Plan 摘要">
          {selectedPlan ? (
            <div className="space-y-3">
              <div className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="text-sm font-semibold text-slate-950">{selectedPlan.goal}</div>
                  <StatusPill label={selectedPlan.status} tone={toneForStatus(selectedPlan.status)} />
                </div>
              </div>
              {selectedPlan.steps.slice(0, 3).map((step) => (
                <div key={`${step.sortOrder}-${step.title}`} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700">
                  {step.sortOrder}. {step.title}
                </div>
              ))}
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-5 text-sm text-slate-600">
              当前上下文还没有激活中的 Plan。
            </div>
          )}
        </Panel>

        <Panel eyebrow="Nodes & Skills" title="项目绑定能力">
          <div className="space-y-3">
            {nodes.slice(0, 3).map((node) => (
              <div key={node.id} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3">
                <div>
                  <div className="text-sm font-medium text-slate-950">{node.name}</div>
                  <div className="mt-1 text-xs text-slate-500">{node.host}</div>
                </div>
                <StatusPill label={node.status} tone={toneForStatus(node.status)} />
              </div>
            ))}
            <div className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">已启用 Skills</div>
              <div className="mt-3 space-y-2">
                {skills.map((skill) => (
                  <div key={skill.id} className="flex items-center justify-between text-sm text-slate-700">
                    <span>{skill.name}</span>
                    <span className="font-mono text-xs text-slate-500">{skill.version}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Panel>
      </div>
    </aside>
  )
}

function MetaLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-2xl border border-slate-200 bg-white px-4 py-3">
      <span className="text-slate-500">{label}</span>
      <span className="text-right text-slate-900">{value}</span>
    </div>
  )
}

function CommandPalette({
  open,
  onOpenChange,
  input,
  onInputChange,
  availableCommands,
  onExecute,
}: {
  open: boolean
  onOpenChange: React.Dispatch<React.SetStateAction<boolean>>
  input: string
  onInputChange: (value: string) => void
  availableCommands: CommandId[]
  onExecute: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl rounded-3xl border-slate-200 bg-white p-0">
        <DialogHeader className="px-6 pt-6">
          <DialogTitle>命令面板</DialogTitle>
          <DialogDescription>直接执行项目级 Slash Commands，用于新建项目、生成 Plan、查看节点状态和沉淀资产。</DialogDescription>
        </DialogHeader>
        <div className="px-6 pb-6">
          <Input
            value={input}
            onChange={(event) => onInputChange(event.target.value)}
            className="h-12 rounded-2xl border-slate-200"
            placeholder="/plan goal=重构前端工作台"
          />
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            {availableCommands.map((commandId) => (
              <button
                key={commandId}
                type="button"
                onClick={() => onInputChange(commandId)}
                className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-left transition hover:border-cyan-200 hover:bg-cyan-50/70"
              >
                <div className="text-sm font-semibold text-slate-950">{commandCatalog[commandId].label}</div>
                <div className="mt-1 text-sm text-slate-600">{commandCatalog[commandId].description}</div>
                <div className="mt-2 font-mono text-xs text-slate-500">{commandId}</div>
              </button>
            ))}
          </div>
        </div>
        <DialogFooter className="border-t border-slate-200 px-6 py-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          <Button onClick={onExecute}>执行命令</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function CreateProjectDialog({
  open,
  onOpenChange,
  draft,
  onChange,
  onCreate,
}: {
  open: boolean
  onOpenChange: React.Dispatch<React.SetStateAction<boolean>>
  draft: NewProjectDraft
  onChange: React.Dispatch<React.SetStateAction<NewProjectDraft>>
  onCreate: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-slate-200 bg-white">
        <DialogHeader>
          <DialogTitle>创建新项目</DialogTitle>
          <DialogDescription>项目是 Loom 的一级上下文，会承载会话、Plan、记忆、资产和节点信息。</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <Input value={draft.name} onChange={(event) => onChange((current) => ({ ...current, name: event.target.value }))} placeholder="项目名称" />
          <textarea
            className="min-h-[120px] w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
            value={draft.description}
            onChange={(event) => onChange((current) => ({ ...current, description: event.target.value }))}
            placeholder="项目描述"
          />
          <select
            className="h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm"
            value={draft.type}
            onChange={(event) => onChange((current) => ({ ...current, type: event.target.value as ProjectSummary['type'] }))}
          >
            <option value="knowledge">知识</option>
            <option value="ops">运维</option>
            <option value="learning">学习</option>
          </select>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onCreate}>创建项目</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function CreateChatDialog({
  open,
  onOpenChange,
  draft,
  onChange,
  onCreate,
}: {
  open: boolean
  onOpenChange: React.Dispatch<React.SetStateAction<boolean>>
  draft: NewConversationDraft
  onChange: React.Dispatch<React.SetStateAction<NewConversationDraft>>
  onCreate: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-slate-200 bg-white">
        <DialogHeader>
          <DialogTitle>新建 Chat</DialogTitle>
          <DialogDescription>新的 Chat 会进入当前项目的多轮会话列表，方便持续切换和追踪。</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <Input value={draft.title} onChange={(event) => onChange((current) => ({ ...current, title: event.target.value }))} placeholder="会话标题" />
          <textarea
            className="min-h-[120px] w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
            value={draft.summary}
            onChange={(event) => onChange((current) => ({ ...current, summary: event.target.value }))}
            placeholder="会话摘要"
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onCreate}>创建 Chat</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function CreatePlanDialog({
  open,
  onOpenChange,
  draft,
  onChange,
  onCreate,
}: {
  open: boolean
  onOpenChange: React.Dispatch<React.SetStateAction<boolean>>
  draft: NewPlanDraft
  onChange: React.Dispatch<React.SetStateAction<NewPlanDraft>>
  onCreate: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-slate-200 bg-white">
        <DialogHeader>
          <DialogTitle>新建 Plan</DialogTitle>
          <DialogDescription>Plan 会创建一条独立会话，并把复杂任务整理成结构化执行路径。</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <Input value={draft.title} onChange={(event) => onChange((current) => ({ ...current, title: event.target.value }))} placeholder="Plan 会话标题" />
          <textarea
            className="min-h-[120px] w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
            value={draft.goal}
            onChange={(event) => onChange((current) => ({ ...current, goal: event.target.value }))}
            placeholder="Plan 目标"
          />
          <Input value={draft.constraints} onChange={(event) => onChange((current) => ({ ...current, constraints: event.target.value }))} placeholder="约束，使用 | 分隔" />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onCreate}>创建 Plan</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function RenameConversationDialog({
  draft,
  onOpenChange,
  onChange,
  onSave,
}: {
  draft: RenameDraft | null
  onOpenChange: (open: boolean) => void
  onChange: React.Dispatch<React.SetStateAction<RenameDraft | null>>
  onSave: () => void
}) {
  return (
    <Dialog open={!!draft} onOpenChange={onOpenChange as React.Dispatch<React.SetStateAction<boolean>>}>
      <DialogContent className="rounded-3xl border-slate-200 bg-white">
        <DialogHeader>
          <DialogTitle>编辑会话</DialogTitle>
          <DialogDescription>你可以在这里调整标题和摘要，让会话管理更清晰。</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <Input
            value={draft?.title ?? ''}
            onChange={(event) => onChange((current) => (current ? { ...current, title: event.target.value } : current))}
            placeholder="会话标题"
          />
          <textarea
            className="min-h-[120px] w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
            value={draft?.summary ?? ''}
            onChange={(event) => onChange((current) => (current ? { ...current, summary: event.target.value } : current))}
            placeholder="会话摘要"
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onSave}>保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
