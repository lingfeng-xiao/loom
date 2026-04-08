import type { WorkspaceHeaderAction } from '../frontendTypes'
import { WorkbenchIcon } from './WorkbenchIcon'

interface LoomTopBarProps {
  workspaceTitle: string
  workspaceMeta: string
  environmentStatus: string
  actions: WorkspaceHeaderAction[]
  onQuickSearch: () => void
  onAction: (actionId: string) => void
}

export function LoomTopBar({ workspaceTitle, workspaceMeta, environmentStatus, actions, onQuickSearch, onAction }: LoomTopBarProps) {
  return (
    <header className="topbar workspaceTitleBar">
      <div className="titleBarMain">
        <div className="titleBarCopy">
          <h1>{workspaceTitle}</h1>
          <p>{workspaceMeta}</p>
        </div>
      </div>

      <div className="titleBarActions">
        <button aria-label="搜索" className="iconButton" onClick={onQuickSearch} type="button">
          <WorkbenchIcon name="search" />
        </button>
        <span className="environmentHint">
          <WorkbenchIcon name="status" />
          {environmentStatus}
        </span>
        {actions.map((action) => (
          <button
            className={action.tone === 'primary' ? 'primaryHeaderButton' : 'headerGhostButton'}
            key={action.id}
            onClick={() => onAction(action.id)}
            type="button"
          >
            <WorkbenchIcon name={action.icon} />
            <span>{action.label}</span>
          </button>
        ))}
      </div>
    </header>
  )
}
