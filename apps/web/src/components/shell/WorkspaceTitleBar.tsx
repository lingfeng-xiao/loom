import type { ThemePreference } from '../../app/theme'
import { WorkbenchIcon } from '../WorkbenchIcon'

interface WorkspaceTitleBarProps {
  environmentStatus: string
  bootstrapSourceLabel: string
  bootstrapSourceDetail: string
  themePreference: ThemePreference
  onToggleBootstrapSource: () => void
  onOpenClaw: () => void
  onThemeChange: (preference: ThemePreference) => void
}

const themeOptions: Array<{ id: ThemePreference; label: string; icon: 'systemTheme' | 'sun' | 'moon' }> = [
  { id: 'system', label: '跟随系统', icon: 'systemTheme' },
  { id: 'light', label: '亮色', icon: 'sun' },
  { id: 'dark', label: '暗色', icon: 'moon' },
]

function resolveStatusTone(environmentStatus: string) {
  const normalized = environmentStatus.toLowerCase()
  if (normalized.includes('healthy') || normalized.includes('connected') || normalized.includes('online')) {
    return 'connected'
  }
  if (normalized.includes('warning') || normalized.includes('degraded')) {
    return 'warning'
  }
  return 'default'
}

export function WorkspaceTitleBar({
  environmentStatus,
  bootstrapSourceLabel,
  bootstrapSourceDetail,
  themePreference,
  onToggleBootstrapSource,
  onOpenClaw,
  onThemeChange,
}: WorkspaceTitleBarProps) {
  return (
    <header className="workspaceTitleBar">
      <div className="titleBarBrand">
        <div className="titleBarBrandMark">L</div>
        <div className="titleBarBrandText">
          <strong>loom</strong>
        </div>
      </div>

      <div className="titleBarActions">
        <button
          aria-label={`切换数据源，当前为 ${bootstrapSourceLabel}`}
          className="uiButton uiButton-secondary titleBarChipButton"
          onClick={onToggleBootstrapSource}
          title={bootstrapSourceDetail}
          type="button"
        >
          <WorkbenchIcon name="status" size={14} />
          <span>{bootstrapSourceLabel}</span>
        </button>

        <div aria-label="主题切换" className="themeSwitcher" role="group">
          {themeOptions.map((option) => (
            <button
              aria-pressed={themePreference === option.id}
              aria-label={option.label}
              className={`uiButton uiButton-ghost uiButton-icon themeSwitchButton ${themePreference === option.id ? 'active' : ''}`}
              key={option.id}
              onClick={() => onThemeChange(option.id)}
              title={option.label}
              type="button"
            >
              <WorkbenchIcon name={option.icon} size={14} />
            </button>
          ))}
        </div>

        <button
          aria-label="打开自动化状态"
          className={`uiButton uiButton-secondary uiButton-icon titleBarIconButton titleBarIconButton-${resolveStatusTone(environmentStatus)}`}
          onClick={onOpenClaw}
          title={environmentStatus}
          type="button"
        >
          <WorkbenchIcon name="automation" size={15} />
        </button>
      </div>
    </header>
  )
}
