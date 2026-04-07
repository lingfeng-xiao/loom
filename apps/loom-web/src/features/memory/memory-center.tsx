import type { MemoryRecord } from '@loom/contracts'
import { Button } from '@/components/ui/button'
import {
  Panel,
  StatusPill,
  formatAbsoluteDate,
  labelMemoryScope,
  relativeTime,
  toneForStatus,
} from '@/features/workspace/presentation'

export function MemoryCenter({
  memories,
  onSaveTemplate,
}: {
  memories: MemoryRecord[]
  onSaveTemplate: (scope: 'global' | 'project') => Promise<void>
}) {
  const globalMemories = memories.filter((memory) => memory.scope === 'global')
  const projectMemories = memories.filter((memory) => memory.scope === 'project')

  return (
    <div className="space-y-6">
      <Panel
        eyebrow="Memory Center"
        title="可见记忆"
        description="把长期约束、约定和沉淀内容维持在显式可见的状态，避免它们继续散落在对话里。"
        actions={
          <>
            <Button variant="outline" onClick={() => void onSaveTemplate('global')}>
              保存全局记忆
            </Button>
            <Button onClick={() => void onSaveTemplate('project')}>保存项目记忆</Button>
          </>
        }
      >
        <div className="grid gap-6 xl:grid-cols-2">
          <MemoryGroup title="全局记忆" description="对所有项目都生效的固定偏好与规则。" memories={globalMemories} />
          <MemoryGroup title="项目记忆" description="只在当前项目生效的局部上下文与偏好。" memories={projectMemories} />
        </div>
      </Panel>
    </div>
  )
}

function MemoryGroup({
  title,
  description,
  memories,
}: {
  title: string
  description: string
  memories: MemoryRecord[]
}) {
  return (
    <section className="space-y-3">
      <div>
        <div className="text-sm font-semibold text-slate-950">{title}</div>
        <div className="mt-1 text-sm text-slate-500">{description}</div>
      </div>
      {memories.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-6 text-sm text-slate-600">
          当前分组还没有记忆条目。
        </div>
      ) : (
        memories.map((memory) => (
          <div key={memory.id} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="min-w-0">
                <div className="text-sm font-semibold text-slate-950">{memory.title}</div>
                <div className="mt-1 text-xs text-slate-500">
                  {labelMemoryScope(memory.scope)} · 优先级 {memory.priority} · {relativeTime(memory.updatedAt)}
                </div>
              </div>
              <StatusPill label={memory.status === 'active' ? '启用' : '停用'} tone={toneForStatus(memory.status)} />
            </div>
            <div className="mt-3 text-sm leading-7 text-slate-700">{memory.content}</div>
            <div className="mt-3 text-xs text-slate-500">来源 {memory.sourceType} · 更新时间 {formatAbsoluteDate(memory.updatedAt)}</div>
          </div>
        ))
      )}
    </section>
  )
}
