import type {
  AssetType,
  ConversationMode,
  ConversationStatus,
  MemoryScope,
  MemoryStatus,
  NodeStatus,
  NodeType,
  PlanStatus,
  PlanStepStatus,
  ProjectType,
} from '@loom/contracts'
import { cn } from '@/lib/utils'

const projectTypeLabels: Record<ProjectType, string> = {
  knowledge: '知识',
  ops: '运维',
  learning: '学习',
}

const modeLabels: Record<ConversationMode, string> = {
  chat: 'Chat',
  plan: 'Plan',
}

const conversationStatusLabels: Record<ConversationStatus, string> = {
  active: '进行中',
  archived: '已归档',
}

const memoryScopeLabels: Record<MemoryScope, string> = {
  global: '全局',
  project: '项目',
  derived: '派生',
}

const memoryStatusLabels: Record<MemoryStatus, string> = {
  active: '启用',
  disabled: '停用',
}

const planStatusLabels: Record<PlanStatus, string> = {
  draft: '草案',
  ready: '就绪',
  approved: '已批准',
  running: '执行中',
  completed: '已完成',
  failed: '失败',
}

const stepStatusLabels: Record<PlanStepStatus, string> = {
  pending: '待开始',
  running: '进行中',
  completed: '已完成',
  failed: '失败',
}

const nodeStatusLabels: Record<NodeStatus, string> = {
  online: '在线',
  offline: '离线',
  degraded: '降级',
  unknown: '未知',
}

const nodeTypeLabels: Record<NodeType, string> = {
  local_pc: '本机',
  server: '服务器',
}

const assetTypeLabels: Record<AssetType, string> = {
  knowledge_card: '知识卡片',
  ops_note: '运维记录',
  learning_card: '学习卡片',
  summary_note: '总结笔记',
  structured_markdown: '结构化 Markdown',
}

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

const toneClasses: Record<Tone, string> = {
  neutral: 'border-slate-200 bg-slate-100 text-slate-700',
  info: 'border-cyan-200 bg-cyan-50 text-cyan-700',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  warning: 'border-amber-200 bg-amber-50 text-amber-700',
  danger: 'border-rose-200 bg-rose-50 text-rose-700',
}

export function labelProjectType(value: ProjectType) {
  return projectTypeLabels[value]
}

export function labelConversationMode(value: ConversationMode) {
  return modeLabels[value]
}

export function labelConversationStatus(value: ConversationStatus) {
  return conversationStatusLabels[value]
}

export function labelMemoryScope(value: MemoryScope) {
  return memoryScopeLabels[value]
}

export function labelMemoryStatus(value: MemoryStatus) {
  return memoryStatusLabels[value]
}

export function labelPlanStatus(value: PlanStatus) {
  return planStatusLabels[value]
}

export function labelPlanStepStatus(value: PlanStepStatus) {
  return stepStatusLabels[value]
}

export function labelNodeStatus(value: NodeStatus) {
  return nodeStatusLabels[value]
}

export function labelNodeType(value: NodeType) {
  return nodeTypeLabels[value]
}

export function labelAssetType(value: AssetType) {
  return assetTypeLabels[value]
}

export function toneForStatus(value: string): Tone {
  if (['online', 'completed', 'approved', 'active', 'up', 'enabled'].includes(value)) return 'success'
  if (['running', 'ready', 'plan'].includes(value)) return 'info'
  if (['draft', 'pending', 'project', 'global', 'degraded'].includes(value)) return 'warning'
  if (['failed', 'offline', 'down', 'archived', 'disabled'].includes(value)) return 'danger'
  return 'neutral'
}

export function StatusPill({ label, tone }: { label: string; tone: Tone }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-medium tracking-[0.04em]',
        toneClasses[tone],
      )}
    >
      {label}
    </span>
  )
}

export function Panel({
  title,
  eyebrow,
  description,
  actions,
  children,
  className,
}: {
  title: string
  eyebrow?: string
  description?: string
  actions?: React.ReactNode
  children: React.ReactNode
  className?: string
}) {
  return (
    <section
      className={cn(
        'rounded-3xl border border-slate-200/90 bg-white/94 shadow-[0_12px_40px_rgba(15,23,42,0.06)] backdrop-blur',
        className,
      )}
    >
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200/80 px-6 py-5">
        <div className="min-w-0">
          {eyebrow ? (
            <div className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">{eyebrow}</div>
          ) : null}
          <h2 className="mt-2 text-lg font-semibold tracking-tight text-slate-950">{title}</h2>
          {description ? <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">{description}</p> : null}
        </div>
        {actions ? <div className="flex shrink-0 items-center gap-2">{actions}</div> : null}
      </header>
      <div className={cn('px-6 py-5', className ? '' : '')}>{children}</div>
    </section>
  )
}

export function MetricCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/90 px-4 py-4">
      <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-3 text-[28px] font-semibold tracking-tight text-slate-950">{value}</div>
      {hint ? <div className="mt-2 text-xs text-slate-500">{hint}</div> : null}
    </div>
  )
}

export function formatPercent(value?: number) {
  if (typeof value !== 'number') return '--'
  return `${Math.round(value * 100)}%`
}

export function formatDateTime(value?: string | null) {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function formatAbsoluteDate(value?: string | null) {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function relativeTime(value?: string | null) {
  if (!value) return '暂无更新'
  const diffMs = Date.now() - new Date(value).getTime()
  const minutes = Math.max(1, Math.floor(diffMs / 60_000))
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  return `${days} 天前`
}

export function DividerLabel({ children }: { children: React.ReactNode }) {
  return <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{children}</div>
}
