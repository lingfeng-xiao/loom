import { type ComponentType, type FormEvent, type KeyboardEvent, type ReactNode, useEffect, useMemo, useRef, useState } from 'react'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { ConversationSummary, MessageRecord, PlanRecord, ProjectSummary } from '@loom/contracts'
import { ArrowUpRight, Clock3, ListTodo, MessageSquareText, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Panel, StatusPill, formatAbsoluteDate, labelConversationMode, labelConversationStatus, relativeTime, toneForStatus } from '@/features/workspace/presentation'
import { cn } from '@/lib/utils'

type DraftMap = Record<string, string>

const markdownComponents: Components = {
  p({ children }) {
    return <p className="whitespace-pre-wrap leading-7 text-inherit">{children}</p>
  },
  a({ children, href }) {
    return (
      <a
        href={href}
        target="_blank"
        rel="noreferrer noopener"
        className="font-medium text-cyan-700 underline decoration-cyan-300 underline-offset-4 transition hover:text-cyan-900"
      >
        {children}
      </a>
    )
  },
  ul({ children }) {
    return <ul className="my-3 list-disc space-y-2 pl-5 leading-7">{children}</ul>
  },
  ol({ children }) {
    return <ol className="my-3 list-decimal space-y-2 pl-5 leading-7">{children}</ol>
  },
  li({ children }) {
    return <li className="leading-7">{children}</li>
  },
  blockquote({ children }) {
    return <blockquote className="my-4 border-l-2 border-slate-300 pl-4 text-slate-500">{children}</blockquote>
  },
  h1({ children }) {
    return <h1 className="mt-5 mb-3 text-xl font-semibold tracking-tight text-slate-950">{children}</h1>
  },
  h2({ children }) {
    return <h2 className="mt-5 mb-3 text-lg font-semibold tracking-tight text-slate-950">{children}</h2>
  },
  h3({ children }) {
    return <h3 className="mt-4 mb-2 text-base font-semibold tracking-tight text-slate-950">{children}</h3>
  },
  hr() {
    return <hr className="my-5 border-slate-200" />
  },
  code(props) {
    const { inline, className, children, ...rest } = props as {
      inline?: boolean
      className?: string
      children?: ReactNode
    } & Record<string, unknown>
    if (inline) {
      return (
        <code
          className="rounded-md border border-slate-200 bg-slate-100 px-1.5 py-0.5 font-mono text-[0.86em] text-slate-900"
          {...rest}
        >
          {children}
        </code>
      )
    }

    return (
      <code className={cn('font-mono text-[0.86em] text-inherit', className)} {...rest}>
        {children}
      </code>
    )
  },
  pre({ children }) {
    return (
      <pre className="my-4 overflow-x-auto rounded-2xl border border-slate-800/90 bg-slate-950 px-4 py-4 text-sm leading-6 text-slate-50 shadow-sm">
        {children}
      </pre>
    )
  },
  table({ children }) {
    return (
      <div className="my-4 overflow-x-auto rounded-2xl border border-slate-200">
        <table className="min-w-full border-collapse text-left text-sm">{children}</table>
      </div>
    )
  },
  thead({ children }) {
    return <thead className="bg-slate-50 text-slate-600">{children}</thead>
  },
  th({ children }) {
    return <th className="border-b border-slate-200 px-4 py-3 font-semibold">{children}</th>
  },
  td({ children }) {
    return <td className="border-b border-slate-100 px-4 py-3 align-top leading-6 text-slate-700">{children}</td>
  },
}

export function ChatWorkspace({
  project,
  conversation,
  messages,
  relatedPlans,
  onSendMessage,
  onPromoteToPlan,
  onOpenPlan,
}: {
  project: ProjectSummary
  conversation: ConversationSummary | null
  messages: MessageRecord[]
  relatedPlans: PlanRecord[]
  onSendMessage: (content: string) => Promise<void>
  onPromoteToPlan: () => Promise<void>
  onOpenPlan: (planId: string) => void
}) {
  const [drafts, setDrafts] = useState<DraftMap>({})
  const [submitting, setSubmitting] = useState(false)
  const [isComposing, setIsComposing] = useState(false)
  const scrollAnchorRef = useRef<HTMLDivElement | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const activePlan = relatedPlans[0] ?? null

  const conversationDraft = conversation ? drafts[conversation.id] ?? '' : ''
  const messageStats = useMemo(() => {
    const user = messages.filter((message) => message.role === 'user').length
    const assistant = messages.filter((message) => message.role === 'assistant').length
    const system = messages.filter((message) => message.role === 'system').length
    return { user, assistant, system, total: messages.length }
  }, [messages])

  useEffect(() => {
    scrollAnchorRef.current?.scrollIntoView({ block: 'end', behavior: 'smooth' })
  }, [conversation?.id, messages.length])

  useEffect(() => {
    textareaRef.current?.focus()
  }, [conversation?.id])

  function updateDraft(next: string) {
    if (!conversation) return
    setDrafts((current) => ({
      ...current,
      [conversation.id]: next,
    }))
  }

  async function submitDraft() {
    if (!conversation) return
    const next = conversationDraft.trim()
    if (!next || submitting) return

    setSubmitting(true)
    try {
      await onSendMessage(next)
      updateDraft('')
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void submitDraft()
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing || isComposing) {
      return
    }

    event.preventDefault()
    void submitDraft()
  }

  if (!conversation) {
    return (
      <div className="flex h-full min-h-0 items-center justify-center rounded-[28px] border border-slate-200/80 bg-white/92 shadow-[0_12px_40px_rgba(15,23,42,0.06)]">
        <div className="max-w-xl px-8 py-10 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-lg shadow-slate-900/10">
            <MessageSquareText className="h-6 w-6" />
          </div>
          <h2 className="mt-5 text-xl font-semibold tracking-tight text-slate-950">请选择一个会话</h2>
          <p className="mt-3 text-sm leading-7 text-slate-600">从左侧进入一个项目会话，这里会切到完整对话工作面。</p>
        </div>
      </div>
    )
  }

  return (
    <div className="grid h-full min-h-0 gap-6 xl:grid-cols-[minmax(0,1fr)_336px]">
      <section className="flex min-h-0 flex-col overflow-hidden rounded-[28px] border border-slate-200/80 bg-white/94 shadow-[0_12px_40px_rgba(15,23,42,0.06)]">
        <header className="border-b border-slate-200/80 px-6 py-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-500">Conversation Workspace</div>
              <div className="mt-2 flex flex-wrap items-center gap-3">
                <h1 className="truncate text-xl font-semibold tracking-tight text-slate-950">{conversation.title}</h1>
                <StatusPill label={labelConversationMode(conversation.mode)} tone={toneForStatus(conversation.mode)} />
                <StatusPill label={labelConversationStatus(conversation.status)} tone={toneForStatus(conversation.status)} />
              </div>
              <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-slate-500">
                <span className="inline-flex items-center gap-1.5">
                  <Clock3 className="h-4 w-4" />
                  {relativeTime(conversation.updatedAt)}
                </span>
                <span>项目：{project.name}</span>
                {conversation.summary ? <span className="max-w-3xl truncate text-slate-600">{conversation.summary}</span> : null}
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {activePlan ? (
                <Button variant="outline" onClick={() => onOpenPlan(activePlan.id)}>
                  打开关联 Plan
                </Button>
              ) : null}
              <Button onClick={() => void onPromoteToPlan()}>
                升级为 Plan
                <ArrowUpRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </div>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto px-4 py-5 sm:px-6">
          {messages.length === 0 ? (
            <div className="flex h-full min-h-[320px] items-center justify-center rounded-[24px] border border-dashed border-slate-300 bg-slate-50/70 px-6 text-center">
              <div className="max-w-lg">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-900 text-white">
                  <Sparkles className="h-5 w-5" />
                </div>
                <h3 className="mt-4 text-base font-semibold text-slate-950">开始这一轮对话</h3>
                <p className="mt-2 text-sm leading-7 text-slate-600">直接输入需求或上下文即可，支持 Markdown / GFM。</p>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {messages.map((message) => (
                <MessageBubble key={message.id} message={message} />
              ))}
              <div ref={scrollAnchorRef} />
            </div>
          )}
        </div>

        <div className="sticky bottom-0 border-t border-slate-200/80 bg-white/96 px-4 py-4 backdrop-blur sm:px-6">
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="rounded-[24px] border border-slate-200 bg-slate-50/90 p-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.65)]">
              <textarea
                ref={textareaRef}
                value={conversationDraft}
                onChange={(event) => updateDraft(event.target.value)}
                onKeyDown={handleComposerKeyDown}
                onCompositionStart={() => setIsComposing(true)}
                onCompositionEnd={() => setIsComposing(false)}
                placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                className="min-h-[132px] w-full resize-none border-0 bg-transparent px-2 py-1 text-[15px] leading-7 text-slate-900 outline-none placeholder:text-slate-400"
                disabled={submitting}
              />
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="text-xs text-slate-500">
                Enter 发送 · Shift+Enter 换行 · 支持 Markdown / GFM
              </div>
              <Button type="submit" disabled={submitting || !conversationDraft.trim()}>
                {submitting ? '发送中...' : '发送消息'}
                <ArrowUpRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </form>
        </div>
      </section>

      <aside className="min-h-0 space-y-4 overflow-y-auto">
        <Panel eyebrow="Context" title="当前上下文">
          <div className="space-y-3 text-sm text-slate-600">
            <ContextRow icon={MessageSquareText} label="会话" value={conversation.title} />
            <ContextRow icon={Sparkles} label="摘要" value={conversation.summary || '暂无摘要'} />
            <ContextRow icon={Clock3} label="更新" value={formatAbsoluteDate(conversation.updatedAt)} />
          </div>
        </Panel>

        <Panel eyebrow="Stats" title="消息概览">
          <div className="grid grid-cols-2 gap-3">
            <MetricTile label="总数" value={String(messageStats.total)} />
            <MetricTile label="用户" value={String(messageStats.user)} />
            <MetricTile label="助手" value={String(messageStats.assistant)} />
            <MetricTile label="系统" value={String(messageStats.system)} />
          </div>
        </Panel>

        <Panel eyebrow="Plan" title="相关执行路径">
          {activePlan ? (
            <div className="space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="text-sm font-semibold text-slate-950">{activePlan.goal}</div>
                    <div className="mt-1 text-xs text-slate-500">{relativeTime(activePlan.updatedAt)}</div>
                  </div>
                  <StatusPill label={activePlan.status === 'running' ? '执行中' : activePlan.status === 'completed' ? '已完成' : '草案'} tone={toneForStatus(activePlan.status)} />
                </div>
              </div>

              <div className="space-y-2">
                {activePlan.steps.slice(0, 3).map((step) => (
                  <div key={`${step.sortOrder}-${step.title}`} className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
                    <div className="flex items-center gap-2 text-xs font-medium text-slate-500">
                      <ListTodo className="h-4 w-4" />
                      Step {step.sortOrder}
                    </div>
                    <div className="mt-2 text-sm font-medium text-slate-950">{step.title}</div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/70 px-4 py-5 text-sm leading-7 text-slate-600">
              当前还没有关联 Plan，可以直接从这轮对话升级。
            </div>
          )}
        </Panel>

        {relatedPlans.length > 1 ? (
          <Panel eyebrow="History" title="其他相关 Plan">
            <div className="space-y-3">
              {relatedPlans.slice(1).map((plan) => (
                <button
                  key={plan.id}
                  type="button"
                  onClick={() => onOpenPlan(plan.id)}
                  className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-4 text-left transition hover:border-cyan-200 hover:bg-cyan-50/60"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="text-sm font-semibold text-slate-950">{plan.goal}</div>
                      <div className="mt-1 text-xs text-slate-500">{relativeTime(plan.updatedAt)}</div>
                    </div>
                    <StatusPill
                      label={plan.status === 'running' ? '执行中' : plan.status === 'completed' ? '已完成' : '草案'}
                      tone={toneForStatus(plan.status)}
                    />
                  </div>
                </button>
              ))}
            </div>
          </Panel>
        ) : null}
      </aside>
    </div>
  )
}

function MessageBubble({ message }: { message: MessageRecord }) {
  const isUser = message.role === 'user'
  const isSystem = message.role === 'system'
  const roleLabel = isUser ? '我' : isSystem ? '系统' : '助手'

  return (
    <div className={cn('flex', isUser ? 'justify-end' : 'justify-start')}>
      <article
        className={cn(
          'max-w-[min(820px,92%)] rounded-[24px] px-5 py-4 shadow-sm',
          isUser
            ? 'border border-slate-900/90 bg-slate-900 text-slate-50 shadow-[0_18px_40px_rgba(15,23,42,0.16)]'
            : isSystem
              ? 'border border-amber-200 bg-amber-50/90 text-slate-800'
              : 'border border-slate-200 bg-white text-slate-800',
        )}
      >
        <div className={cn('flex items-center justify-between gap-3 text-xs', isUser ? 'text-slate-300' : 'text-slate-500')}>
          <div className="inline-flex items-center gap-2">
            <span
              className={cn(
                'inline-flex h-7 min-w-7 items-center justify-center rounded-full border px-2 text-[11px] font-semibold',
                isUser
                  ? 'border-white/15 bg-white/10 text-white'
                  : isSystem
                    ? 'border-amber-200 bg-amber-100 text-amber-700'
                    : 'border-slate-200 bg-slate-100 text-slate-700',
              )}
            >
              {roleLabel}
            </span>
            <span>{formatAbsoluteDate(message.createdAt)}</span>
          </div>
        </div>

        <div className={cn('mt-3 text-[15px] leading-7', isUser ? 'text-slate-50' : 'text-slate-700')}>
          <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
            {message.content}
          </ReactMarkdown>
        </div>
      </article>
    </div>
  )
}

function ContextRow({
  icon: Icon,
  label,
  value,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  value: string
}) {
  return (
    <div className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4">
      <div className="rounded-2xl bg-slate-900 p-2.5 text-white">
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0">
        <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
        <div className="mt-2 text-sm leading-6 text-slate-700">{value}</div>
      </div>
    </div>
  )
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
      <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-3 text-2xl font-semibold tracking-tight text-slate-950">{value}</div>
    </div>
  )
}
