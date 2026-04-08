import { useEffect, useMemo, useState } from 'react'
import type { UiRightPanelTab } from '../../app/routeTypes'
import { WorkbenchIcon } from '../WorkbenchIcon'
import type { ContextSectionViewModel, TraceDomainState } from '../../domains/workbenchTypes'
import type { TraceStep } from '../../types'

interface WorkspaceRightRailProps {
  activeTab: UiRightPanelTab
  collapsed: boolean
  trace: TraceDomainState
  contextSections: ContextSectionViewModel[]
  onTabChange: (tab: UiRightPanelTab) => void
  onToggleCollapsed: () => void
}

function displayStepStatus(status: TraceStep['status']) {
  switch (status) {
    case 'pending':
      return '待处理'
    case 'running':
      return '执行中'
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

function buildCommandLines(step: TraceStep, index: number) {
  const command = step.detail.replace(/\s*\+\s*/g, ' && ').replace(/\s*\/\s*/g, ' / ')

  return [
    `$ loom run --step "${step.label}"`,
    command.startsWith('$') ? command : `$ ${command}`,
    `# 状态: ${displayStepStatus(step.status)}`,
  ].join('\n')
}

export function WorkspaceRightRail({
  activeTab,
  collapsed,
  trace,
  contextSections,
  onTabChange,
  onToggleCollapsed,
}: WorkspaceRightRailProps) {
  const defaultStepId = useMemo(
    () => trace.steps.find((step) => step.status === 'running' || step.status === 'waiting')?.id ?? trace.steps[0]?.id ?? null,
    [trace.steps],
  )
  const [selectedStepId, setSelectedStepId] = useState<string | null>(defaultStepId)

  useEffect(() => {
    setSelectedStepId((current) => {
      if (current && trace.steps.some((step) => step.id === current)) {
        return current
      }

      return defaultStepId
    })
  }, [defaultStepId, trace.steps])

  const selectedStep = trace.steps.find((step) => step.id === selectedStepId) ?? trace.steps[0] ?? null

  if (collapsed) {
    return (
      <aside className="rightRail rightRail-collapsed">
        <div className="rightRailCollapsedButtons">
          <button aria-label="展开执行面板" className="railIconButton active" onClick={onToggleCollapsed} type="button">
            <WorkbenchIcon name="panelExpand" />
          </button>
          <button
            aria-label="切换到任务视角"
            className={`railIconButton ${activeTab === 'tasks' ? 'active' : ''}`}
            onClick={() => onTabChange('tasks')}
            type="button"
          >
            <WorkbenchIcon name="tasks" />
          </button>
          <button
            aria-label="切换到命令面板"
            className={`railIconButton ${activeTab === 'commands' ? 'active' : ''}`}
            onClick={() => onTabChange('commands')}
            type="button"
          >
            <WorkbenchIcon name="terminal" />
          </button>
        </div>
      </aside>
    )
  }

  return (
    <aside className="rightRail rightRail-expanded">
      <div className="railWorkbenchHeader">
        <div className="railTabButtons">
          <button className={`railTabButton ${activeTab === 'tasks' ? 'active' : ''}`} onClick={() => onTabChange('tasks')} type="button">
            <WorkbenchIcon name="tasks" />
            <span>任务</span>
          </button>
          <button className={`railTabButton ${activeTab === 'commands' ? 'active' : ''}`} onClick={() => onTabChange('commands')} type="button">
            <WorkbenchIcon name="terminal" />
            <span>命令</span>
          </button>
        </div>

        <button aria-label="收起执行面板" className="railIconButton" onClick={onToggleCollapsed} type="button">
          <WorkbenchIcon name="panelCollapse" />
        </button>
      </div>

      {activeTab === 'tasks' ? (
        <div className="railWorkbenchBody">
          <section className="railSurfaceCard railSurfaceCard-compact">
            <div className="railSurfaceEyebrow">当前执行</div>
            <div className="railSurfaceTopRow">
              <strong>{trace.runStatus.label}</strong>
            </div>
            <p className="railSummary">{trace.summary}</p>
          </section>

          <section className="railSection railSection-grow">
            <div className="railSurfaceEyebrow">任务视角</div>
            <div className="railTaskList">
              {trace.steps.map((step, index) => (
                <button
                  className={`railTaskItem ${step.id === selectedStep?.id ? 'active' : ''}`}
                  key={step.id}
                  onClick={() => {
                    setSelectedStepId(step.id)
                    onTabChange('commands')
                  }}
                  type="button"
                >
                  <span className={`timelineMarker timelineMarker-${step.status}`}>{index + 1}</span>
                  <div className="railTaskContent">
                    <div className="railTaskRow">
                      <strong>{step.label}</strong>
                      <span className={`inlineStatus inlineStatus-${step.status}`}>{displayStepStatus(step.status)}</span>
                    </div>
                    <small>{step.detail}</small>
                  </div>
                </button>
              ))}
            </div>
          </section>

          <section className="railSection">
            <div className="railSurfaceEyebrow">任务上下文</div>
            <div className="contextSectionList">
              {contextSections.flatMap((section) => section.blocks).slice(0, 3).map((block) => (
                <div className="contextItem" key={block.id}>
                  <strong>{block.label}</strong>
                  <p>{block.value}</p>
                </div>
              ))}
            </div>
          </section>
        </div>
      ) : (
        <div className="railWorkbenchBody">
          <section className="railSurfaceCard railSurfaceCard-compact">
            <div className="railSurfaceEyebrow">命令执行面板</div>
            {selectedStep ? (
              <>
                <div className="railSurfaceTopRow">
                  <strong>{selectedStep.label}</strong>
                  <span className={`inlineStatus inlineStatus-${selectedStep.status}`}>{displayStepStatus(selectedStep.status)}</span>
                </div>
                <small className="railCommandMeta">{selectedStep.detail}</small>
              </>
            ) : (
              <strong>暂无命令</strong>
            )}
          </section>

          <section className="railCommandPanel">
            {selectedStep ? <pre className="railCommandLog">{buildCommandLines(selectedStep, trace.steps.indexOf(selectedStep))}</pre> : null}
          </section>

          <section className="railSection">
            <div className="railSurfaceEyebrow">可切换任务</div>
            <div className="railCommandStepStrip">
              {trace.steps.map((step) => (
                <button
                  className={`railCommandStepButton ${step.id === selectedStep?.id ? 'active' : ''}`}
                  key={step.id}
                  onClick={() => setSelectedStepId(step.id)}
                  type="button"
                >
                  {step.label}
                </button>
              ))}
            </div>
          </section>
        </div>
      )}
    </aside>
  )
}
