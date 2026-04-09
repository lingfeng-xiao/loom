import { useEffect, useRef } from 'react'
import type { ComposerDraftState } from '../../domains/workbenchTypes'
import type { ComposerState } from '../../types'
import { WorkbenchIcon } from '../WorkbenchIcon'

interface ComposerDockProps {
  composer: ComposerState
  draft: ComposerDraftState
  onDraftChange: (text: string) => void
  onSubmit: () => void | Promise<void>
  error: string | null
}

export function ComposerDock({ composer, draft, onDraftChange, onSubmit, error }: ComposerDockProps) {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const submitDisabled = draft.submitting || draft.draftText.trim().length === 0

  useEffect(() => {
    const textarea = textareaRef.current
    if (!textarea) {
      return
    }

    textarea.style.height = '0px'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 240)}px`
  }, [draft.draftText])

  return (
    <footer className="composerDock">
      <div className={`uiSurface uiSurface-elevated composerCard ${draft.submitting ? 'isSubmitting' : ''} ${error ? 'hasError' : ''}`}>
        <label className="composerInputShell">
          <textarea
            className="composerInput"
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey && !submitDisabled) {
                event.preventDefault()
                void onSubmit()
              }
            }}
            placeholder={composer.placeholder}
            ref={textareaRef}
            rows={1}
            value={draft.draftText}
          />
        </label>

        <div className="composerBottomRow">
          <div className="composerMetaBlock">
            {error ? <p className="composerError">{error}</p> : <span>Enter 发送，Shift + Enter 换行</span>}
          </div>

          <button
            aria-label={draft.submitting ? '发送中' : composer.primaryActionLabel}
            className="uiButton uiButton-primary uiButton-icon composerSendButton"
            disabled={submitDisabled}
            onClick={() => void onSubmit()}
            type="button"
          >
            <WorkbenchIcon name="arrowUp" size={16} />
          </button>
        </div>
      </div>
    </footer>
  )
}
