import type { ConversationMode } from '../../types'

interface ConversationSubbarProps {
  activeMode: ConversationMode
  modes: Array<{ id: ConversationMode; label: string }>
  meta: string
  onModeChange: (mode: ConversationMode) => void
}

export function ConversationSubbar({ activeMode, modes, meta, onModeChange }: ConversationSubbarProps) {
  return (
    <div className="conversationSubbar">
      <div className="conversationModeTabs">
        {modes.map((mode) => (
          <button className={`titleModePill ${mode.id === activeMode ? 'active' : ''}`} key={mode.id} onClick={() => onModeChange(mode.id)} type="button">
            {mode.label}
          </button>
        ))}
      </div>
      <div className="conversationSubbarMeta">{meta}</div>
    </div>
  )
}
