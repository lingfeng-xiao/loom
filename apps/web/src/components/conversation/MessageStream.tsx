import { useEffect, useRef } from 'react'
import type { ConversationMessage } from '../../types'
import { MessageItem } from './MessageItem'

interface MessageStreamProps {
  conversationId: string | null
  messages: ConversationMessage[]
  restoredScrollTop: number | undefined
  onScrollPositionChange: (conversationId: string, scrollTop: number) => void
}

export function MessageStream({ conversationId, messages, restoredScrollTop, onScrollPositionChange }: MessageStreamProps) {
  const streamRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (streamRef.current && restoredScrollTop != null) {
      streamRef.current.scrollTop = restoredScrollTop
    }
  }, [restoredScrollTop, conversationId])

  return (
    <div
      className="chatStream"
      onScroll={() => {
        if (!conversationId || !streamRef.current) {
          return
        }

        onScrollPositionChange(conversationId, streamRef.current.scrollTop)
      }}
      ref={streamRef}
    >
      {messages.map((message) => (
        <MessageItem key={message.id} message={message} />
      ))}
    </div>
  )
}
