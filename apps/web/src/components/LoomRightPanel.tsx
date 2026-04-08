import type { UiRightPanelTab } from '../frontendTypes'
import type { ContextBlock, TraceStep } from '../types'

interface LoomRightPanelProps {
  tab: UiRightPanelTab
  summary: string
  steps: TraceStep[]
  contextBlocks: ContextBlock[]
  onChangeTab: (tab: UiRightPanelTab) => void
}

function displayStepStatus(status: TraceStep['status']) {
  switch (status) {
    case 'pending':
      return '待处理'
    case 'running':
      return '进行中'
    case 'waiting':
      return '等待中'
    case 'success':
      return '成功'
    case 'failed':
      return '失败'
    case 'skipped':
      return '已跳过'
    default:
      return status
  }
}

export function LoomRightPanel({ tab, summary, steps, contextBlocks, onChangeTab }: LoomRightPanelProps) {
  return (
    <aside className="rightRail">
      <div className="railTabs">
        <button className={`railTab ${tab === 'trace' ? 'active' : ''}`} onClick={() => onChangeTab('trace')} type="button">
          日志
        </button>
        <button className={`railTab ${tab === 'context' ? 'active' : ''}`} onClick={() => onChangeTab('context')} type="button">
          Context
        </button>
      </div>

      {tab === 'trace' ? (
        <div className="railBody">
          <section className="railSection">
            <div className="railSectionTitle">当前摘要</div>
            <p className="railSummary">{summary}</p>
          </section>

          <section className="railSection railSection-grow">
            <div className="railSectionTitle">步骤</div>
            <div className="timelineList">
              {steps.map((step, index) => (
                <div className="timelineItem" key={step.id}>
                  <div className="timelineMarkerWrap">
                    <span className={`timelineMarker timelineMarker-${step.status}`}>{index + 1}</span>
                    {index < steps.length - 1 ? <span className="timelineLine" /> : null}
                  </div>
                  <div className="timelineContent">
                    <div className="timelineTopRow">
                      <strong>{step.label}</strong>
                      <span className={`inlineStatus inlineStatus-${step.status}`}>{displayStepStatus(step.status)}</span>
                    </div>
                    <small>{step.detail}</small>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      ) : (
        <div className="railBody">
          <section className="railSection railSection-grow">
            <div className="railSectionTitle">上下文</div>
            <div className="contextList">
              {contextBlocks.map((block) => (
                <div className="contextItem" key={block.id}>
                  <strong>{block.label}</strong>
                  <p>{block.value}</p>
                </div>
              ))}
            </div>
          </section>
        </div>
      )}
    </aside>
  )
}
