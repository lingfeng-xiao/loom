import type { UiRightPanelTab } from '../app/routeTypes'
import type { TraceDomainState } from '../domains/workbenchTypes'
import type { LoomBootstrapPayload } from '../types'

function deriveRunStatus(payload: LoomBootstrapPayload): TraceDomainState['runStatus'] {
  const runningStep = payload.traceSteps.find((step) => step.status === 'running' || step.status === 'waiting')
  if (runningStep) {
    return {
      status: runningStep.status,
      label: runningStep.status === 'running' ? '执行中' : '等待回调',
    }
  }

  const failedStep = payload.traceSteps.find((step) => step.status === 'failed')
  if (failedStep) {
    return {
      status: 'failed',
      label: '执行失败',
    }
  }

  return {
    status: 'success',
    label: '已同步',
  }
}

export function buildTraceState(payload: LoomBootstrapPayload, activeTab: UiRightPanelTab): TraceDomainState {
  return {
    summary: payload.traceSummary,
    steps: payload.traceSteps,
    runStatus: deriveRunStatus(payload),
    activeTab,
  }
}
