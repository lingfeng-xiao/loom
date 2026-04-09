import { useEffect, useMemo, useRef } from 'react'
import type { ConversationMessage } from '../../types'
import { MessageItem } from './MessageItem'

interface MessageStreamProps {
  conversationId: string | null
  messages: ConversationMessage[]
  restoredScrollTop: number | undefined
  showPendingAssistant?: boolean
  onScrollPositionChange: (conversationId: string, scrollTop: number) => void
  onOpenThinking?: (messageId: string) => void
}

interface RenderItem {
  key: string
  message: ConversationMessage
  thinkingMessage?: ConversationMessage | null
  pending?: boolean
}

function buildRenderItems(messages: ConversationMessage[], showPendingAssistant: boolean): RenderItem[] {
  const items: RenderItem[] = []
  let hasVisiblePendingAssistant = false

  for (let index = 0; index < messages.length; index += 1) {
    const message = messages[index]

    if (message.kind === 'thinking_summary') {
      const nextMessage = messages[index + 1]
      if (nextMessage?.kind === 'assistant') {
        continue
      }

      hasVisiblePendingAssistant = true
      items.push({
        key: `pending-thinking-${message.id}`,
        message: {
          id: `pending-thinking-${message.id}`,
          kind: 'assistant',
          label: '',
          body: '',
          statusLabel: 'pending',
          createdAt: message.createdAt,
          completedAt: null,
        },
        thinkingMessage: message,
        pending: true,
      })
      continue
    }

    if (message.kind === 'assistant') {
      if (message.statusLabel === 'streaming' || message.statusLabel === 'pending') {
        hasVisiblePendingAssistant = true
      }

      const previousMessage = messages[index - 1]
      items.push({
        key: message.id,
        message,
        thinkingMessage: previousMessage?.kind === 'thinking_summary' ? previousMessage : null,
      })
      continue
    }

    items.push({
      key: message.id,
      message,
    })
  }

  if (showPendingAssistant && !hasVisiblePendingAssistant) {
    items.push({
      key: 'pending-assistant',
      message: {
        id: 'pending-assistant',
        kind: 'assistant',
        label: '',
        body: '',
        statusLabel: 'pending',
        createdAt: new Date().toISOString(),
        completedAt: null,
      },
      pending: true,
    })
  }

  return items
}

export function MessageStream({
  conversationId,
  messages,
  restoredScrollTop,
  showPendingAssistant = false,
  onScrollPositionChange,
  onOpenThinking,
}: MessageStreamProps) {
  const streamRef = useRef<HTMLDivElement | null>(null)
  const stickToBottomRef = useRef(true)
  const renderItems = useMemo(() => buildRenderItems(messages, showPendingAssistant), [messages, showPendingAssistant])

  useEffect(() => {
    if (!streamRef.current) {
      return
    }

    if (restoredScrollTop != null && restoredScrollTop > 0) {
      streamRef.current.scrollTop = restoredScrollTop
      stickToBottomRef.current = false
      return
    }

    stickToBottomRef.current = true
    streamRef.current.scrollTop = streamRef.current.scrollHeight
  }, [restoredScrollTop, conversationId])

  useEffect(() => {
    if (!streamRef.current || !stickToBottomRef.current) {
      return
    }

    streamRef.current.scrollTop = streamRef.current.scrollHeight
  }, [conversationId, renderItems])

  return (
    <div
      className="chatStream"
      onScroll={() => {
        if (!conversationId || !streamRef.current) {
          return
        }

        const distanceToBottom = streamRef.current.scrollHeight - streamRef.current.scrollTop - streamRef.current.clientHeight
        stickToBottomRef.current = distanceToBottom < 120
        onScrollPositionChange(conversationId, streamRef.current.scrollTop)
      }}
      ref={streamRef}
    >
      <div className="chatStreamLane">
        {renderItems.map((item) => (
          <MessageItem
            key={item.key}
            message={item.message}
            onOpenThinking={onOpenThinking}
            pending={item.pending}
            thinkingMessage={item.thinkingMessage}
          />
        ))}
      </div>
    </div>
  )
}
