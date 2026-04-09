import { useState } from 'react'
import { ComposerDock } from '../components/conversation/ComposerDock'
import { ConversationSubbar } from '../components/conversation/ConversationSubbar'
import { MessageStream } from '../components/conversation/MessageStream'
import { useComposerStore } from '../domains/composer/useComposerStore'
import { useConversationStore } from '../domains/conversation/useConversationStore'
import { useProjectStore } from '../domains/project/useProjectStore'
import { useUiStore } from '../domains/ui/useUiStore'

interface ConversationWorkspacePageProps {
  onOpenThinkingMessage?: (messageId: string) => void
}

export function ConversationWorkspacePage({ onOpenThinkingMessage }: ConversationWorkspacePageProps) {
  const conversation = useConversationStore()
  const composer = useComposerStore()
  const project = useProjectStore()
  const ui = useUiStore()
  const [movingProject, setMovingProject] = useState(false)

  const handleProjectChange = async (projectId: string) => {
    if (!conversation.activeConversationId || projectId === project.currentProject.id) {
      return
    }

    setMovingProject(true)
    try {
      await project.moveConversation(conversation.activeConversationId, projectId)
    } finally {
      setMovingProject(false)
    }
  }

  const handleCreateProject = async () => {
    if (!conversation.activeConversationId) {
      return
    }

    setMovingProject(true)
    try {
      const createdProject = await project.createProject({})
      await project.moveConversation(conversation.activeConversationId, createdProject.id)
    } finally {
      setMovingProject(false)
    }
  }

  return (
    <section className="conversationWorkspace">
      <ConversationSubbar
        activeMode={conversation.activeMode}
        activeProjectId={project.currentProject.id}
        meta={conversation.subbarMeta}
        modes={conversation.conversationModes}
        movingProject={movingProject}
        onCreateProject={() => void handleCreateProject()}
        onModeChange={conversation.setMode}
        onProjectChange={(projectId) => void handleProjectChange(projectId)}
        projects={project.availableProjects}
        title={conversation.activeThread?.title ?? '新会话'}
      />

      <MessageStream
        conversationId={conversation.activeConversationId}
        messages={conversation.messages}
        onOpenThinking={onOpenThinkingMessage}
        onScrollPositionChange={ui.setScrollPosition}
        restoredScrollTop={conversation.activeConversationId ? ui.scrollPositions[conversation.activeConversationId] : undefined}
        showPendingAssistant={composer.currentDraft.submitting}
      />

      <ComposerDock
        composer={composer.snapshot}
        draft={composer.currentDraft}
        error={ui.error}
        onDraftChange={composer.updateDraft}
        onSubmit={composer.submitDraft}
      />
    </section>
  )
}
