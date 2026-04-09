import type { TraceRunStatus } from '../../domains/workbenchTypes'
import type { TraceStep } from '../../types'

interface TraceRailProps {
  summary: string
  steps: TraceStep[]
  runStatus: TraceRunStatus
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

export function TraceRail({ summary, steps, runStatus }: TraceRailProps) {
  return (
    <div className="railBody">
      <section className="railSection">
        <div className="railSectionTitle">执行摘要</div>
        <div className="railSummaryCard">
          <strong>{runStatus.label}</strong>
          <p className="railSummary">{summary}</p>
        </div>
      </section>

      <section className="railSection railSection-grow">
        <div className="railSectionTitle">Run Timeline</div>
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
  )
}
