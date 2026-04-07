import type { AssetRecord, ConversationSummary, PlanRecord } from '@loom/contracts'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Panel, labelAssetType, relativeTime } from '@/features/workspace/presentation'

export function AssetsLibrary({
  assets,
  conversations,
  plans,
}: {
  assets: AssetRecord[]
  conversations: ConversationSummary[]
  plans: PlanRecord[]
}) {
  const conversationMap = new Map(conversations.map((conversation) => [conversation.id, conversation]))
  const planMap = new Map(plans.map((plan) => [plan.id, plan]))

  return (
    <Panel
      eyebrow="Assets Library"
      title="项目资产库"
      description="资产页显示从 Chat 与 Plan 中沉淀出来的正式内容，包括类型、来源、标签和存储路径。"
    >
      {assets.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-4 py-8 text-sm text-slate-600">
          还没有资产输出，执行 /save-card 或完成 Plan 后会出现在这里。
        </div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>标题</TableHead>
              <TableHead>类型</TableHead>
              <TableHead>来源</TableHead>
              <TableHead>标签</TableHead>
              <TableHead>更新时间</TableHead>
              <TableHead>存储路径</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {assets.map((asset) => (
              <TableRow key={asset.id}>
                <TableCell className="font-medium text-slate-950">{asset.title}</TableCell>
                <TableCell>{labelAssetType(asset.type)}</TableCell>
                <TableCell className="text-slate-600">
                  {asset.sourcePlanId
                    ? `Plan · ${planMap.get(asset.sourcePlanId)?.goal ?? asset.sourcePlanId}`
                    : asset.sourceConversationId
                      ? `Chat · ${conversationMap.get(asset.sourceConversationId)?.title ?? asset.sourceConversationId}`
                      : '手动创建'}
                </TableCell>
                <TableCell className="text-slate-600">{asset.tags.length > 0 ? asset.tags.join(' / ') : '暂无'}</TableCell>
                <TableCell className="text-slate-600">{relativeTime(asset.createdAt)}</TableCell>
                <TableCell className="font-mono text-xs text-slate-500">{asset.storagePath}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Panel>
  )
}
