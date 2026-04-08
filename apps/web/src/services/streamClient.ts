export type LoomStreamEventType =
  | 'message.delta'
  | 'message.done'
  | 'thinking.summary.delta'
  | 'thinking.summary.done'
  | 'action.started'
  | 'action.step.updated'
  | 'action.waiting'
  | 'action.done'
  | 'context.updated'
  | 'memory.suggested'
  | 'error'

export interface LoomStreamEvent {
  type: LoomStreamEventType
  payload: Record<string, unknown>
}

export interface StreamClient {
  subscribe(conversationId: string, onEvent: (event: LoomStreamEvent) => void): () => void
}

export function createNoopStreamClient(): StreamClient {
  return {
    subscribe(_conversationId, _onEvent) {
      return () => undefined
    },
  }
}
