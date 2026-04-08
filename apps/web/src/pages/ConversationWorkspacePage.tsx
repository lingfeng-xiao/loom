import { ComposerDock } from '../components/conversation/ComposerDock'
import { ConversationSubbar } from '../components/conversation/ConversationSubbar'
import { MessageStream } from '../components/conversation/MessageStream'
import { useComposerStore } from '../domains/composer/useComposerStore'
import { useConversationStore } from '../domains/conversation/useConversationStore'
import { useUiStore } from '../domains/ui/useUiStore'

export function ConversationWorkspacePage() {
  const conversation = useConversationStore()
  const composer = useComposerStore()
  const ui = useUiStore()

  return (
    <section className="conversationWorkspace">
      <ConversationSubbar
        activeMode={conversation.activeMode}
        meta={conversation.subbarMeta}
        modes={conversation.conversationModes}
        onModeChange={conversation.setMode}
      />
      <MessageStream
        conversationId={conversation.activeConversationId}
        messages={conversation.messages}
        onScrollPositionChange={ui.setScrollPosition}
        restoredScrollTop={conversation.activeConversationId ? ui.scrollPositions[conversation.activeConversationId] : undefined}
      />
      <ComposerDock
        composer={composer.snapshot}
        draft={composer.currentDraft}
        onDraftChange={composer.updateDraft}
        onSubmit={composer.submitDraft}
      />
    </section>
  )
}
