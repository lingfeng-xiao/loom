import type { ConversationMessage } from '../../types'

interface MessageItemProps {
  message: ConversationMessage
}

export function MessageItem({ message }: MessageItemProps) {
  const isUser = message.kind === 'user'
  const latencyLabel = message.latencyLabel
    ? message.latencyLabel.replace('已处理', '已思考')
    : isUser
      ? '已思考 0s'
      : '已思考 0.8s'

  return (
    <article className={`chatMessage ${isUser ? 'chatMessage-user' : 'chatMessage-assistant'}`}>
      <div className="chatBubble">
        <p>{message.body}</p>
        {message.emphasis ? <div className="chatSupplement">{message.emphasis}</div> : null}
        {latencyLabel || message.statusLabel ? (
          <div className="chatFootnote">
            {latencyLabel ? <small>{latencyLabel}</small> : null}
            {message.statusLabel ? <span className="chatStatus">{message.statusLabel}</span> : null}
          </div>
        ) : null}
      </div>
    </article>
  )
}
