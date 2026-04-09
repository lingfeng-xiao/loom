import { useMemo } from 'react'
import { useMemoryStore } from '../domains/memory/useMemoryStore'
import { useProjectStore } from '../domains/project/useProjectStore'
import type { MemoryItemView, MemorySuggestionView } from '../types'

function scopeLabel(scope: MemoryItemView['scope']) {
  return scope === 'project' ? '项目' : scope === 'conversation' ? '会话' : '全局'
}

function sourceLabel(source: MemoryItemView['source']) {
  return source === 'explicit' ? '显式写入' : source === 'assisted' ? '辅助沉淀' : '系统写入'
}

function suggestionStatusLabel(status: MemorySuggestionView['status']) {
  return status === 'pending' ? '待处理' : status === 'accepted' ? '已接受' : '已拒绝'
}

export function MemoryPage() {
  const project = useProjectStore()
  const memory = useMemoryStore()

  const items = memory.items ?? []
  const projectItems = useMemo(() => items.filter((item) => item.scope === 'project'), [items])
  const conversationItems = useMemo(() => items.filter((item) => item.scope === 'conversation'), [items])
  const globalItems = useMemo(() => items.filter((item) => item.scope === 'global'), [items])
  const pendingSuggestions = useMemo(
    () => memory.suggestions.filter((suggestion) => suggestion.status === 'pending'),
    [memory.suggestions],
  )
  const loading = memory.items === null && !memory.error

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>记忆</h2>
        <p>统一展示当前项目的长期记忆条目，以及流式返回的记忆建议。</p>
      </div>

      <div className="infoBanner">
        当前项目：{project.currentProject.name} | 状态：{memory.error ? '远端读取失败' : loading ? '正在加载' : '远端数据已接管'}
        {pendingSuggestions.length > 0 ? ` | 待处理建议：${pendingSuggestions.length}` : ''}
      </div>

      {memory.error ? <section className="infoBanner">记忆读取失败：{memory.error}</section> : null}

      {loading ? (
        <div className="emptyPage">
          <p className="eyebrow">记忆</p>
          <h3>正在加载项目记忆</h3>
          <p>页面已经切换到 provider 统一状态流，当前仅等待远端 memory 读模型返回。</p>
        </div>
      ) : items.length === 0 && memory.suggestions.length === 0 ? (
        <div className="emptyPage">
          <p className="eyebrow">记忆</p>
          <h3>当前还没有可展示的记忆</h3>
          <p>后续记忆写入、建议确认和系统沉淀都会在这里汇总。</p>
        </div>
      ) : (
        <div className="toolPageGrid">
          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>长期记忆条目</h3>
              <span>{items.length} 条</span>
            </div>
            <ul className="toolList">
              {items.map((item) => (
                <li key={item.id}>
                  <strong>
                    {scopeLabel(item.scope)} / {sourceLabel(item.source)}
                  </strong>
                  <small>{item.content}</small>
                  <span>
                    {item.projectId ?? '全局'}{item.conversationId ? ` / ${item.conversationId}` : ''}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>记忆建议</h3>
              <span>{memory.suggestions.length} 条</span>
            </div>
            <ul className="toolList">
              {memory.suggestions.map((suggestion) => (
                <li key={suggestion.id}>
                  <strong>
                    {scopeLabel(suggestion.scope)} / {suggestionStatusLabel(suggestion.status)}
                  </strong>
                  <small>{suggestion.content}</small>
                  <span>{suggestion.createdAt}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>项目级</h3>
              <span>{projectItems.length} 条</span>
            </div>
            <ul className="toolList">
              {projectItems.map((item) => (
                <li key={item.id}>
                  <strong>{sourceLabel(item.source)}</strong>
                  <small>{item.content}</small>
                  <span>{item.updatedAt}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>会话级</h3>
              <span>{conversationItems.length} 条</span>
            </div>
            <ul className="toolList">
              {conversationItems.map((item) => (
                <li key={item.id}>
                  <strong>{sourceLabel(item.source)}</strong>
                  <small>{item.content}</small>
                  <span>{item.conversationId ?? '当前会话'}</span>
                </li>
              ))}
            </ul>
          </div>

          {globalItems.length > 0 ? (
            <div className="toolPanel">
              <div className="toolPanelHeader">
                <h3>全局</h3>
                <span>{globalItems.length} 条</span>
              </div>
              <ul className="toolList">
                {globalItems.map((item) => (
                  <li key={item.id}>
                    <strong>{sourceLabel(item.source)}</strong>
                    <small>{item.content}</small>
                    <span>{item.updatedAt}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      )}
    </section>
  )
}
