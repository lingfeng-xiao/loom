import type { AssetRecord, PlanRecord } from '@loom/contracts'
import { CheckCircle2, Play, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Panel,
  StatusPill,
  formatAbsoluteDate,
  labelPlanStepStatus,
  relativeTime,
  toneForStatus,
} from '@/features/workspace/presentation'

export function PlanWorkspace({
  plan,
  assets,
  onApprove,
  onRun,
  onComplete,
}: {
  plan: PlanRecord | null
  assets: AssetRecord[]
  onApprove: () => Promise<void>
  onRun: () => Promise<void>
  onComplete: () => Promise<void>
}) {
  if (!plan) {
    return (
      <Panel eyebrow="Plan Workspace" title="还没有选中 Plan" description="从左侧创建或打开一个项目计划。">
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-8 text-sm text-slate-600">
          当前没有可展示的计划。
        </div>
      </Panel>
    )
  }

  const relatedAssets = assets.filter((asset) => asset.sourcePlanId === plan.id)

  return (
    <div className="grid gap-6 2xl:grid-cols-[minmax(0,1.1fr)_360px]">
      <div className="space-y-6">
        <Panel
          eyebrow="Plan Workspace"
          title={plan.goal}
          description="Plan 是项目级结构化执行对象，适合处理多步骤、需审批、需沉淀资产的复杂任务。"
          actions={
            <>
              {(plan.status === 'draft' || plan.status === 'ready') && (
                <Button variant="outline" onClick={() => void onApprove()}>
                  <ShieldCheck className="mr-2 h-4 w-4" />
                  批准
                </Button>
              )}
              {plan.status === 'approved' && (
                <Button variant="outline" onClick={() => void onRun()}>
                  <Play className="mr-2 h-4 w-4" />
                  开始执行
                </Button>
              )}
              {plan.status === 'running' && (
                <Button onClick={() => void onComplete()}>
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                  标记完成
                </Button>
              )}
            </>
          }
        >
          <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_300px]">
            <div className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
              <div className="flex flex-wrap items-center gap-3">
                <StatusPill label={plan.approvalRequired ? '需要批准' : '可直接执行'} tone={toneForStatus(plan.approvalRequired ? 'ready' : 'active')} />
                <StatusPill label={plan.status === 'draft' ? '草案' : plan.status === 'approved' ? '已批准' : plan.status === 'running' ? '执行中' : plan.status === 'completed' ? '已完成' : '计划'} tone={toneForStatus(plan.status)} />
                <span className="text-xs text-slate-500">最后更新 {relativeTime(plan.updatedAt)}</span>
              </div>
              <div className="mt-4 text-sm leading-7 text-slate-700">
                {plan.constraints.length > 0 ? (
                  <>
                    <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">约束</div>
                    <div className="mt-2">{plan.constraints.join(' / ')}</div>
                  </>
                ) : (
                  '当前没有额外约束。'
                )}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4">
              <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">执行结果</div>
              <div className="mt-3 text-sm leading-7 text-slate-700">
                {plan.executionResult?.summary ?? '计划尚未完成，暂无执行摘要。'}
              </div>
              {plan.executionResult ? (
                <div className="mt-4 space-y-2">
                  {plan.executionResult.logs.map((log, index) => (
                    <div key={`${log}-${index}`} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-600">
                      {log}
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </div>

          <div className="mt-6 space-y-3">
            {plan.steps.map((step) => (
              <div key={step.id} className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex items-center gap-3">
                      <div className="rounded-full bg-slate-900 px-2 py-1 text-[11px] font-semibold text-white">
                        {step.sortOrder}
                      </div>
                      <div className="text-sm font-semibold text-slate-950">{step.title}</div>
                    </div>
                    <div className="mt-2 text-sm leading-6 text-slate-600">{step.description}</div>
                    {step.result ? (
                      <div className="mt-3 rounded-2xl border border-cyan-200 bg-cyan-50/70 px-3 py-3 text-sm text-cyan-900">
                        {step.result}
                      </div>
                    ) : null}
                  </div>
                  <StatusPill label={labelPlanStepStatus(step.status)} tone={toneForStatus(step.status)} />
                </div>
              </div>
            ))}
          </div>
        </Panel>
      </div>

      <div className="space-y-6">
        <Panel eyebrow="Meta" title="计划信息">
          <div className="space-y-3 text-sm text-slate-600">
            <MetaRow label="创建时间" value={formatAbsoluteDate(plan.createdAt)} />
            <MetaRow label="最后更新" value={formatAbsoluteDate(plan.updatedAt)} />
            <MetaRow label="步骤数量" value={`${plan.steps.length} 步`} />
            <MetaRow label="关联资产" value={`${relatedAssets.length} 个`} />
          </div>
        </Panel>

        <Panel eyebrow="Assets" title="关联输出">
          <div className="space-y-3">
            {relatedAssets.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-6 text-sm text-slate-600">
                当前 Plan 还没有沉淀输出资产。
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
      </div>
    </div>
  )
}

function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <span>{label}</span>
      <span className="font-medium text-slate-950">{value}</span>
    </div>
  )
}
