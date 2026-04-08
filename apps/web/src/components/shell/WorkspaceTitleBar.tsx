import { WorkbenchIcon } from '../WorkbenchIcon'

interface WorkspaceTitleBarProps {
  environmentStatus: string
  bootstrapSourceLabel: string
  bootstrapSourceDetail: string
  onToggleBootstrapSource: () => void
  onOpenClaw: () => void
}

export function WorkspaceTitleBar({
  environmentStatus,
  bootstrapSourceLabel,
  bootstrapSourceDetail,
  onToggleBootstrapSource,
  onOpenClaw,
}: WorkspaceTitleBarProps) {
  const isConnected = environmentStatus.includes('已连接')
  const statusClassName = isConnected ? 'openClawStatusButton connected' : 'openClawStatusButton'

  return (
    <header className="workspaceTitleBar">
      <div className="titleBarBrand">
        <div className="titleBarBrandMark">L</div>
        <strong>loom</strong>
      </div>

      <div className="titleBarActions">
        <button
          aria-label={`切换 bootstrap 数据源，当前 ${bootstrapSourceLabel}`}
          className="bootstrapSourceButton"
          onClick={onToggleBootstrapSource}
          title={bootstrapSourceDetail}
          type="button"
        >
          <WorkbenchIcon name="status" />
          <span>{bootstrapSourceLabel}</span>
        </button>

        <button aria-label="OpenClaw 状态" className={statusClassName} onClick={onOpenClaw} title={environmentStatus} type="button">
          <WorkbenchIcon name="automation" />
        </button>
      </div>
    </header>
  )
}
