import type {
  AssetRecord,
  CommandId,
  ConversationSummary,
  MemoryRecord,
  NodeRecord,
  PlanRecord,
  ProjectSummary,
} from '@loom/contracts'
import { ArrowRight, BrainCircuit, Command, FolderTree, ListTodo, Server } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  MetricCard,
  Panel,
  StatusPill,
  labelConversationMode,
  labelNodeStatus,
  labelPlanStatus,
  relativeTime,
  toneForStatus,
} from '@/features/workspace/presentation'

export function ProjectHome({
  project,
  conversations,
  plans,
  memories,
  assets,
  nodes,
  commands,
  onOpenConversation,
  onOpenPlan,
  onOpenSettings,
}: {
  project: ProjectSummary
  conversations: ConversationSummary[]
  plans: PlanRecord[]
  memories: MemoryRecord[]
  assets: AssetRecord[]
  nodes: NodeRecord[]
  commands: CommandId[]
  onOpenConversation: (conversationId: string) => void
  onOpenPlan: (planId: string) => void
  onOpenSettings: () => void
}) {
  const activePlan = plans.find((plan) => ['draft', 'ready', 'approved', 'running'].includes(plan.status)) ?? plans[0] ?? null
  const recentConversations = conversations.slice(0, 5)
  const recentAssets = assets.slice(0, 4)
  const nodeSummary = nodes.slice(0, 4)

  return (
    <div className="space-y-6">
      <section className="grid gap-4 xl:grid-cols-4">
        <MetricCard label="项目会话" value={String(conversations.length)} hint="支持多轮切换与归档" />
        <MetricCard label="活跃计划" value={String(plans.filter((item) => item.status !== 'completed').length)} hint="草案、批准和执行中的 Plan" />
        <MetricCard label="记忆条目" value={String(memories.length)} hint="包含全局与项目记忆" />
        <MetricCard label="资产沉淀" value={String(assets.length)} hint="Obsidian 与知识资产入口" />
      </section>

      <section className="grid gap-6 2xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]">
        <div className="space-y-6">
          <Panel
            eyebrow="Project Home"
            title="最近会话"
            description="从项目维度管理多轮对话。最近活跃的 Chat 与 Plan 会固定显示在这里，方便快速继续。"
          >
            <div className="space-y-3">
              {recentConversations.length === 0 ? (
                <EmptyRow title="当前项目还没有会话" description="使用左侧的新建按钮开始第一轮 Chat 或 Plan。" />
              ) : (
                recentConversations.map((conversation) => (
                  <button
                    key={conversation.id}
                    type="button"
                    onClick={() => onOpenConversation(conversation.id)}
                    className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 text-left transition hover:border-cyan-200 hover:bg-cyan-50/60"
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-3">
                        <span className="truncate text-sm font-semibold text-slate-950">{conversation.title}</span>
                        <StatusPill label={labelConversationMode(conversation.mode)} tone={toneForStatus(conversation.mode)} />
                      </div>
                      <div className="mt-1 truncate text-sm text-slate-600">{conversation.summary || '暂无摘要'}</div>
                    </div>
                    <div className="ml-4 shrink-0 text-right">
                      <div className="text-xs text-slate-500">{relativeTime(conversation.updatedAt)}</div>
                      <ArrowRight className="ml-auto mt-2 h-4 w-4 text-slate-400" />
                    </div>
                  </button>
                ))
              )}
            </div>
          </Panel>

          <Panel eyebrow="Assets" title="最近资产" description="这里聚合最近沉淀到知识库或运维库的正式输出。">
            <div className="space-y-3">
              {recentAssets.length === 0 ? (
                <EmptyRow title="还没有沉淀资产" description="执行 /save-card 或在 Plan 完成后输出正式资产。" />
              ) : (
                recentAssets.map((asset) => (
                  <div key={asset.id} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-semibold text-slate-950">{asset.title}</div>
                        <div className="mt-1 truncate font-mono text-xs text-slate-500">{asset.storagePath}</div>
                      </div>
                      <FolderTree className="h-4 w-4 shrink-0 text-slate-400" />
                    </div>
                  </div>
                ))
              )}
            </div>
          </Panel>
        </div>

        <div className="space-y-6">
          <Panel
            eyebrow="Plan"
            title="当前执行焦点"
            description="把复杂工作拆成可审阅、可执行、可沉淀的结构化路径。"
            actions={
              activePlan ? (
                <Button variant="outline" onClick={() => onOpenPlan(activePlan.id)}>
                  打开 Plan
                </Button>
              ) : (
                <Button variant="outline" onClick={onOpenSettings}>
                  查看设置
                </Button>
              )
            }
          >
            {activePlan ? (
              <div className="space-y-4">
                <div className="flex items-start justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4">
                  <div className="min-w-0">
                    <div className="text-sm font-semibold text-slate-950">{activePlan.goal}</div>
                    <div className="mt-2 text-sm leading-6 text-slate-600">
                      {activePlan.constraints.length > 0 ? activePlan.constraints.join(' / ') : '当前没有额外约束。'}
                    </div>
                  </div>
                  <StatusPill label={labelPlanStatus(activePlan.status)} tone={toneForStatus(activePlan.status)} />
                </div>

                <div className="space-y-2">
                  {activePlan.steps.slice(0, 4).map((step) => (
                    <div key={step.id} className="flex items-start gap-3 rounded-2xl border border-slate-200 px-4 py-3">
                      <div className="mt-0.5 rounded-full bg-slate-900 px-2 py-1 text-[11px] font-semibold text-white">
                        {step.sortOrder}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-slate-950">{step.title}</span>
                          <StatusPill label={labelPlanStatus(activePlan.status)} tone={toneForStatus(step.status)} />
                        </div>
                        <div className="mt-1 text-sm text-slate-600">{step.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <EmptyRow title="当前没有活跃计划" description="从左侧直接新建 Plan，或在 Chat 页面里把当前会话升级为 Plan Mode。" />
            )}
          </Panel>

          <Panel eyebrow="Project Signals" title="关键信息面板">
            <div className="space-y-4">
              <SignalRow icon={BrainCircuit} label="关键记忆" value={String(memories.length)} hint="项目上下文与全局偏好" />
              <SignalRow icon={Server} label="绑定节点" value={String(nodeSummary.length)} hint="只读状态采集与快照" />
              <SignalRow icon={Command} label="可用命令" value={String(commands.length)} hint="命令面板与项目操作入口" />
              <SignalRow icon={ListTodo} label="默认技能" value={String(project.defaultSkills.length)} hint="用于计划执行与资产沉淀" />
            </div>
          </Panel>

          <Panel eyebrow="Nodes" title="节点摘要">
            <div className="space-y-3">
              {nodeSummary.length === 0 ? (
                <EmptyRow title="没有绑定节点" description="在项目设置里绑定本机或服务器节点，用于查看运行状态。" />
              ) : (
                nodeSummary.map((node) => (
                  <div key={node.id} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3">
                    <div>
                      <div className="text-sm font-medium text-slate-950">{node.name}</div>
                      <div className="mt-1 text-xs text-slate-500">{node.host}</div>
                    </div>
                    <StatusPill label={labelNodeStatus(node.status)} tone={toneForStatus(node.status)} />
                  </div>
                ))
              )}
            </div>
          </Panel>
        </div>
      </section>
    </div>
  )
}

function SignalRow({
  icon: Icon,
  label,
  value,
  hint,
}: {
  icon: typeof BrainCircuit
  label: string
  value: string
  hint: string
}) {
  return (
    <div className="flex items-center gap-4 rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4">
      <div className="rounded-2xl bg-slate-900 p-3 text-white">
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0">
        <div className="text-sm font-medium text-slate-950">{label}</div>
        <div className="mt-1 text-xs text-slate-500">{hint}</div>
      </div>
      <div className="ml-auto text-xl font-semibold tracking-tight text-slate-950">{value}</div>
    </div>
  )
}

function EmptyRow({ title, description }: { title: string; description: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-6">
      <div className="text-sm font-medium text-slate-900">{title}</div>
      <div className="mt-2 text-sm leading-6 text-slate-600">{description}</div>
    </div>
  )
}
