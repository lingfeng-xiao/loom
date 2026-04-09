import type { ConversationStreamEvent } from '../types'

const STREAM_EVENT_NAMES = [
  'message.delta',
  'message.done',
  'thinking.summary.delta',
  'thinking.summary.done',
  'trace.step.created',
  'trace.step.updated',
  'trace.step.completed',
  'context.updated',
  'memory.suggested',
  'run.completed',
  'run.failed',
] as const

export interface StreamClient {
  subscribe(streamPath: string, onEvent: (event: ConversationStreamEvent) => void, onClose?: () => void): () => void
}

export function createNoopStreamClient(): StreamClient {
  return {
    subscribe(_streamPath, _onEvent, _onClose) {
      return () => undefined
    },
  }
}

export function createBrowserStreamClient(baseUrl: string): StreamClient {
  return {
    subscribe(streamPath, onEvent, onClose) {
      if (typeof window === 'undefined' || typeof EventSource === 'undefined') {
        return () => undefined
      }

      const source = new EventSource(`${baseUrl}${streamPath}`)
      const handleClose = () => {
        source.close()
        onClose?.()
      }

      for (const eventName of STREAM_EVENT_NAMES) {
        source.addEventListener(eventName, (rawEvent) => {
          const messageEvent = rawEvent as MessageEvent<string>
          onEvent(JSON.parse(messageEvent.data) as ConversationStreamEvent)
        })
      }

      source.onerror = () => {
        handleClose()
      }

      return () => {
        source.close()
      }
    },
  }
}
