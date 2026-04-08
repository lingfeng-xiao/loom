import { WorkbenchIcon } from '../WorkbenchIcon'
import type { ComposerDraftState } from '../../domains/workbenchTypes'
import type { ComposerState } from '../../types'
import type { WorkbenchIconName } from '../../frontendTypes'

interface ComposerDockProps {
  composer: ComposerState
  draft: ComposerDraftState
  onDraftChange: (text: string) => void
}

interface ComposerIconAction {
  id: string
  label: string
  icon: WorkbenchIconName
}

const iconActions: ComposerIconAction[] = [
  { id: 'context', label: '挂载 Context', icon: 'chat' },
  { id: 'file', label: '挂载文件', icon: 'paperclip' },
  { id: 'command', label: '命令面板', icon: 'slash' },
]

const slashCommands = ['/context', '/file', '/memory', '/action', '/plan']

function toggleIcon(label: string): WorkbenchIconName {
  if (label.toLowerCase().includes('memory')) {
    return 'memory'
  }
  if (label.includes('上传')) {
    return 'plus'
  }
  return 'bolt'
}

export function ComposerDock({ composer, draft, onDraftChange }: ComposerDockProps) {
  const showSlashMenu = draft.draftText.trimStart().startsWith('/')

  return (
    <footer className="composerDock">
      <div className="composerCard">
        <div className="composerActionRow">
          {iconActions.map((action) => (
            <button aria-label={action.label} className="composerIconButton" key={action.id} type="button">
              <WorkbenchIcon name={action.icon} />
            </button>
          ))}
        </div>

        <label className="composerInputShell">
          <textarea
            className="composerInput"
            onChange={(event) => onDraftChange(event.target.value)}
            placeholder={composer.placeholder}
            rows={3}
            value={draft.draftText}
          />
        </label>

        {showSlashMenu ? (
          <div className="composerSlashMenu">
            {slashCommands.map((command) => (
              <button className="composerSlashItem" key={command} type="button">
                <span>{command}</span>
              </button>
            ))}
          </div>
        ) : null}

        <div className="composerBottomRow">
          <div className="composerToggleRow">
            {composer.toggles.map((toggle) => (
              <button
                aria-label={toggle.label}
                className={`composerToggleButton ${toggle.enabled ? 'enabled' : ''}`}
                key={toggle.label}
                type="button"
              >
                <WorkbenchIcon name={toggleIcon(toggle.label)} />
              </button>
            ))}
          </div>

          <button aria-label={composer.primaryActionLabel} className="composerSendButton" type="button">
            <WorkbenchIcon name="send" />
          </button>
        </div>
      </div>
    </footer>
  )
}
