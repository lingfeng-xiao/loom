import { useEffect, useMemo, useState } from 'react'
import type { ConversationMessage } from '../../types'
import { durationBetween, formatDurationMs } from '../../utils/time'
import { WorkbenchIcon } from '../WorkbenchIcon'
import { MarkdownContent } from './MarkdownContent'

interface MessageItemProps {
  message: ConversationMessage
  thinkingMessage?: ConversationMessage | null
  pending?: boolean
  onOpenThinking?: (messageId: string) => void
}

function stripThinkBlocks(body: string) {
  return body.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()
}

function resolveDurationLabel(message: ConversationMessage | null | undefined, nowMs: number) {
  if (!message) {
    return null
  }

  if (message.latencyMs != null) {
    return formatDurationMs(message.latencyMs)
  }

  const durationMs = durationBetween(message.createdAt, message.completedAt, nowMs)
  return durationMs == null ? null : formatDurationMs(durationMs)
}

export function MessageItem({ message, thinkingMessage = null, pending = false, onOpenThinking }: MessageItemProps) {
  const isUser = message.kind === 'user'
  const isStreaming = !isUser && (pending || message.statusLabel === 'streaming' || message.statusLabel === 'pending')
  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    if (!isStreaming && !thinkingMessage) {
      return
    }

    const timer = window.setInterval(() => setNowMs(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [isStreaming, thinkingMessage])

  const displayBody = useMemo(() => (isUser ? message.body : stripThinkBlocks(message.body)), [isUser, message.body])
  const thinkingDurationLabel = useMemo(() => resolveDurationLabel(thinkingMessage, nowMs), [nowMs, thinkingMessage])
  const thoughtLabel = useMemo(() => {
    if (thinkingMessage == null) {
      return null
    }

    const prefix = thinkingMessage.statusLabel === 'streaming' ? '思考中' : '思考'
    return thinkingDurationLabel ? `${prefix} ${thinkingDurationLabel}` : prefix
  }, [thinkingDurationLabel, thinkingMessage])

  if (isUser) {
    return (
      <article className="chatMessage chatMessage-user">
        <div className="chatBubble chatBubble-user">
          <MarkdownContent content={displayBody} />
        </div>
      </article>
    )
  }

  return (
    <article className="chatMessage chatMessage-assistant">
      <div className={`chatBubble chatBubble-assistant ${isStreaming ? 'chatBubble-streaming' : ''} ${pending ? 'chatBubble-pending' : ''}`}>
        {displayBody ? (
          <MarkdownContent content={displayBody} />
        ) : (
          <div className="chatPendingState" aria-label="生成中">
            <span className="chatPendingLabel">{thinkingMessage ? '正在梳理答案' : '正在准备回复'}</span>
            <span className="chatPendingLine chatPendingLine-wide" />
            <span className="chatPendingLine" />
            <span className="chatPendingLine chatPendingLine-short" />
            <div className="chatPendingDots">
              <span />
              <span />
              <span />
            </div>
          </div>
        )}

        {thoughtLabel ? (
          <button className="uiButton uiButton-inline chatThoughtMeta" onClick={() => thinkingMessage && onOpenThinking?.(thinkingMessage.id)} type="button">
            <span>{thoughtLabel}</span>
            <WorkbenchIcon name="chevronRight" size={12} />
          </button>
        ) : null}
      </div>
    </article>
  )
}
