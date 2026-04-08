import { useEffect, useMemo, useState } from 'react'
import { useProjectStore } from '../domains/project/useProjectStore'
import { createLoomSdk } from '../sdk/loomApiClient'
import type { MemoryItemView } from '../types'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''
const sdk = createLoomSdk({ baseUrl: API_BASE })

const fallbackMemory: MemoryItemView[] = [
  {
    id: 'fallback-memory-project',
    scope: 'project',
    projectId: 'project-loom',
    conversationId: null,
    content: '保持会话优先、Trace 可见和文档先行。',
    source: 'explicit',
    updatedAt: 'fallback',
  },
  {
    id: 'fallback-memory-conversation',
    scope: 'conversation',
    projectId: 'project-loom',
    conversationId: 'conversation-v1',
    content: '当前会话聚焦 Context、Settings、Capabilities 与联调闭环。',
    source: 'assisted',
    updatedAt: 'fallback',
  },
]

export function MemoryPage() {
  const project = useProjectStore()
  const projectId = project.currentProject.id
  const [items, setItems] = useState<MemoryItemView[]>(fallbackMemory)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const sourceLabel = useMemo(() => (error ? 'fallback' : 'remote'), [error])

  useEffect(() => {
    if (!projectId) {
      setItems([])
      setLoading(false)
      return
    }

    const controller = new AbortController()
    setLoading(true)
    setError(null)

    sdk.workspace
      .getMemory(projectId, controller.signal)
      .then((response) => {
        setItems(response.items)
        setLoading(false)
      })
      .catch((fetchError) => {
        setItems(fallbackMemory)
        setError(fetchError instanceof Error ? fetchError.message : '记忆数据读取失败')
        setLoading(false)
      })

    return () => controller.abort()
  }, [projectId])

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>Memory</h2>
        <p>分层长期记忆的首版浏览页，优先展示项目级和会话级的当前沉淀内容。</p>
      </div>

      {error ? <section className="infoBanner">Memory 远端读取失败，已回退到 {sourceLabel} 数据：{error}</section> : null}

      {loading ? (
        <div className="emptyPage">
          <p className="eyebrow">Memory</p>
          <h3>正在加载分层记忆</h3>
          <p>当前优先读取 workspace API，失败时展示本地基线样例。</p>
        </div>
      ) : items.length === 0 ? (
        <div className="emptyPage">
          <p className="eyebrow">Memory</p>
          <h3>当前还没有记忆条目</h3>
          <p>后续可以接入显式写入、自动建议和审核流。</p>
        </div>
      ) : (
        <div className="toolPageGrid">
          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>记忆条目</h3>
              <span>{items.length} 条 · {sourceLabel}</span>
            </div>
            <ul className="toolList">
              {items.map((item) => (
                <li key={item.id}>
                  <strong>{item.scope.toUpperCase()}</strong>
                  <span>{item.source}</span>
                  <small>{item.content}</small>
                </li>
              ))}
            </ul>
          </div>

          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>作用域映射</h3>
              <span>便于后续联调</span>
            </div>
            <div className="toolDetailStack">
              {items.map((item) => (
                <div className="toolDetailRow" key={`${item.id}-scope`}>
                  <span>{item.id}</span>
                  <strong>{item.conversationId ?? item.projectId ?? 'global'}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
