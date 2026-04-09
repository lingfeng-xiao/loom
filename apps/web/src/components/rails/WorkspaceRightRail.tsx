import { useEffect, useMemo, useState } from 'react'
import type { UiRightPanelTab } from '../../app/routeTypes'
import type { ContextSectionViewModel, TraceDomainState } from '../../domains/workbenchTypes'
import type { ConversationMessage, TraceStep } from '../../types'
import { durationBetween, formatDurationMs } from '../../utils/time'
import { WorkbenchIcon } from '../WorkbenchIcon'
import { MarkdownContent } from '../conversation/MarkdownContent'

interface WorkspaceRightRailProps {
  activeTab: UiRightPanelTab
  collapsed: boolean
  trace: TraceDomainState
  contextSections: ContextSectionViewModel[]
  selectedThinkingMessage: ConversationMessage | null
  onTabChange: (tab: UiRightPanelTab) => void
  onToggleCollapsed: () => void
  onClearThinkingSelection: () => void
}

function displayStepStatus(status: TraceStep['status']) {
  switch (status) {
    case 'pending':
      return '待开始'
    case 'running':
      return '执行中'
    case 'waiting':
      return '等待中'
    case 'success':
      return '已完成'
    case 'failed':
      return '失败'
    case 'skipped':
      return '已跳过'
    default:
      return status
  }
}

function buildCommandLines(step: TraceStep) {
  const command = step.detail.replace(/\s*\+\s*/g, ' && ').replace(/\s*\/\s*/g, ' / ')
  return [`$ loom run --step "${step.label}"`, command.startsWith('$') ? command : `$ ${command}`, `# 状态：${displayStepStatus(step.status)}`].join('\n')
}

export function WorkspaceRightRail({
  activeTab,
  collapsed,
  trace,
  contextSections,
  selectedThinkingMessage,
  onTabChange,
  onToggleCollapsed,
  onClearThinkingSelection,
}: WorkspaceRightRailProps) {
  const [nowMs, setNowMs] = useState(() => Date.now())
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

  useEffect(() => {
    const isActive = trace.steps.some((step) => step.status === 'running' || step.status === 'waiting')
    if (!isActive) {
      return
    }

    const timer = window.setInterval(() => setNowMs(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [trace.steps])

  const selectedStep = trace.steps.find((step) => step.id === selectedStepId) ?? trace.steps[0] ?? null
  const runDurationLabel = useMemo(() => {
    const startedAt = trace.steps.map((step) => step.startedAt).find(Boolean)
    if (!startedAt) {
      return null
    }

    const completedAt = trace.steps.every((step) => step.completedAt != null)
      ? [...trace.steps]
          .reverse()
          .map((step) => step.completedAt)
          .find(Boolean)
      : null
    const durationMs = durationBetween(startedAt, completedAt, nowMs)
    return durationMs == null ? null : formatDurationMs(durationMs)
  }, [nowMs, trace.steps])

  if (collapsed) {
    return (
      <aside className="rightRail rightRail-collapsed">
        <div className="rightRailCollapsedButtons">
          <button aria-label="展开右侧栏" className="uiButton uiButton-secondary uiButton-icon railIconButton active" onClick={onToggleCollapsed} type="button">
            <WorkbenchIcon name="panelExpand" />
          </button>
          <button
            aria-label="打开轨迹视图"
            className={`uiButton uiButton-secondary uiButton-icon railIconButton ${activeTab === 'tasks' ? 'active' : ''}`}
            onClick={() => onTabChange('tasks')}
            type="button"
          >
            <WorkbenchIcon name="tasks" />
          </button>
          <button
            aria-label="打开步骤视图"
            className={`uiButton uiButton-secondary uiButton-icon railIconButton ${activeTab === 'commands' ? 'active' : ''}`}
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
          <button className={`uiButton uiButton-secondary railTabButton ${activeTab === 'tasks' ? 'active' : ''}`} onClick={() => onTabChange('tasks')} type="button">
            <WorkbenchIcon name="tasks" />
            <span>轨迹</span>
          </button>
          <button className={`uiButton uiButton-secondary railTabButton ${activeTab === 'commands' ? 'active' : ''}`} onClick={() => onTabChange('commands')} type="button">
            <WorkbenchIcon name="terminal" />
            <span>步骤</span>
          </button>
        </div>

        <button aria-label="收起右侧栏" className="uiButton uiButton-secondary uiButton-icon railIconButton" onClick={onToggleCollapsed} type="button">
          <WorkbenchIcon name="panelCollapse" />
        </button>
      </div>

      {activeTab === 'tasks' ? (
        <div className="railWorkbenchBody">
          {selectedThinkingMessage ? (
            <section className="uiSurface railSurfaceCard railSurfaceCard-compact">
              <div className="railSurfaceTopRow">
                <div className="railSurfaceEyebrow">思考详情</div>
                <button className="railInlineButton" onClick={onClearThinkingSelection} type="button">
                  收起
                </button>
              </div>
              <div className="railThoughtBody">
                <MarkdownContent content={selectedThinkingMessage.body} />
              </div>
            </section>
          ) : null}

          <section className="uiSurface railSurfaceCard railSurfaceCard-compact">
            <div className="railSurfaceEyebrow">运行状态</div>
            <div className="railSurfaceTopRow">
              <strong>{trace.runStatus.label}</strong>
              {runDurationLabel ? <span className="inlineStatus inlineStatus-neutral">{runDurationLabel}</span> : null}
            </div>
            <p className="railSummary">{trace.summary}</p>
          </section>

          <section className="railSection railSection-grow">
            <div className="railSurfaceEyebrow">执行时间线</div>
            <div className="railTaskList">
              {trace.steps.map((step, index) => {
                const stepDurationMs = durationBetween(step.startedAt, step.completedAt, nowMs)
                return (
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
                      {stepDurationMs != null ? <small className="railTaskMeta">耗时 {formatDurationMs(stepDurationMs)}</small> : null}
                    </div>
                  </button>
                )
              })}
            </div>
          </section>

          <section className="railSection">
            <div className="railSurfaceEyebrow">实时上下文</div>
            <div className="contextSectionList">
              {contextSections
                .flatMap((section) => section.blocks)
                .slice(0, 3)
                .map((block) => (
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
          <section className="uiSurface railSurfaceCard railSurfaceCard-compact">
            <div className="railSurfaceEyebrow">当前步骤</div>
            {selectedStep ? (
              <>
                <div className="railSurfaceTopRow">
                  <strong>{selectedStep.label}</strong>
                  <span className={`inlineStatus inlineStatus-${selectedStep.status}`}>{displayStepStatus(selectedStep.status)}</span>
                </div>
                <small className="railCommandMeta">{selectedStep.detail}</small>
                {durationBetween(selectedStep.startedAt, selectedStep.completedAt, nowMs) != null ? (
                  <small className="railCommandMeta">
                    耗时 {formatDurationMs(durationBetween(selectedStep.startedAt, selectedStep.completedAt, nowMs) ?? 0)}
                  </small>
                ) : null}
              </>
            ) : (
              <strong>暂无步骤</strong>
            )}
          </section>

          <section className="railCommandPanel">{selectedStep ? <pre className="railCommandLog">{buildCommandLines(selectedStep)}</pre> : null}</section>

          <section className="railSection">
            <div className="railSurfaceEyebrow">切换步骤</div>
            <div className="railCommandStepStrip">
              {trace.steps.map((step) => (
                <button
                  className={`uiButton uiButton-secondary railCommandStepButton ${step.id === selectedStep?.id ? 'active' : ''}`}
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
