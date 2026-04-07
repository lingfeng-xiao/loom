import type { NodeRecord, NodeServiceStatus } from '@loom/contracts'
import { AlertTriangle, Server } from 'lucide-react'
import {
  Panel,
  StatusPill,
  formatAbsoluteDate,
  formatPercent,
  labelNodeType,
  toneForStatus,
} from '@/features/workspace/presentation'

export function NodesCenter({ nodes, centerNodeLabel }: { nodes: NodeRecord[]; centerNodeLabel: string }) {
  return (
    <Panel
      eyebrow="Nodes Center"
      title="节点状态中心"
      description="这里只展示只读节点信息，用于查看心跳、快照、探测结果和最近错误。"
    >
      <div className="grid gap-4 xl:grid-cols-2">
        {nodes.map((node) => (
          <section key={node.id} className="rounded-3xl border border-slate-200 bg-white px-5 py-5">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="flex items-center gap-3">
                  <div className="rounded-2xl bg-slate-900 p-3 text-white">
                    <Server className="h-4 w-4" />
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-slate-950">{node.name}</div>
                    <div className="mt-1 text-xs text-slate-500">{node.host}</div>
                  </div>
                </div>
              </div>
              <StatusPill label={statusLabel(node.status)} tone={toneForStatus(node.status)} />
            </div>

            {node.lastError ? (
              <div className="mt-4 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 px-4 py-4 text-sm text-amber-800">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                <div className="min-w-0">
                  <div className="font-medium">最近错误</div>
                  <div className="mt-1 break-words leading-6">{node.lastError}</div>
                </div>
              </div>
            ) : null}

            <div className="mt-5 grid grid-cols-3 gap-3">
              <MiniMetric label="CPU" value={formatPercent(node.snapshot?.cpuUsage)} />
              <MiniMetric label="内存" value={formatPercent(node.snapshot?.memoryUsage)} />
              <MiniMetric label="磁盘" value={formatPercent(node.snapshot?.diskUsage)} />
            </div>

            <div className="mt-5 grid gap-2 text-sm text-slate-600">
              <MetaRow label="节点类型" value={labelNodeType(node.type)} />
              <MetaRow label="最近心跳" value={formatAbsoluteDate(node.lastHeartbeat)} />
              <MetaRow label="快照时间" value={formatAbsoluteDate(node.snapshot?.recordedAt)} />
              <MetaRow label="能力标签" value={node.capabilities.join(' / ') || '暂无'} />
              <MetaRow label="中心节点标签" value={node.type === 'server' ? centerNodeLabel : '本机节点'} />
            </div>

            <div className="mt-5 space-y-2">
              {(node.snapshot?.services ?? []).length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-4 text-sm text-slate-600">
                  当前节点还没有服务探测记录。
                </div>
              ) : (
                node.snapshot?.services.map((service) => <ServiceRow key={`${node.id}-${service.name}-${service.target ?? ''}`} service={service} />)
              )}
            </div>
          </section>
        ))}
      </div>
    </Panel>
  )
}

function ServiceRow({ service }: { service: NodeServiceStatus }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-sm font-medium text-slate-900">{service.name}</div>
          <div className="mt-1 text-xs text-slate-500">
            {[service.kind, service.target].filter(Boolean).join(' · ') || '未提供探测目标'}
          </div>
        </div>
        <StatusPill label={serviceStatusLabel(service.status)} tone={toneForStatus(service.status)} />
      </div>
      {service.detail ? <div className="mt-2 text-sm leading-6 text-slate-600">{service.detail}</div> : null}
      {service.recordedAt ? <div className="mt-2 text-xs text-slate-500">{formatAbsoluteDate(service.recordedAt)}</div> : null}
    </div>
  )
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-2 text-lg font-semibold text-slate-950">{value}</div>
    </div>
  )
}

function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 px-4 py-3">
      <span>{label}</span>
      <span className="text-right text-slate-950">{value}</span>
    </div>
  )
}

function statusLabel(status: NodeRecord['status']) {
  switch (status) {
    case 'online':
      return '在线'
    case 'offline':
      return '离线'
    case 'degraded':
      return '降级'
    default:
      return '未知'
  }
}

function serviceStatusLabel(status: NodeServiceStatus['status']) {
  switch (status) {
    case 'online':
    case 'up':
      return '正常'
    case 'offline':
    case 'down':
      return '离线'
    case 'degraded':
      return '降级'
    default:
      return '未知'
  }
}
