import type { ConversationMode, WorkspacePageId } from '../types'

export type UiRightPanelTab = 'tasks' | 'commands'

export interface LoomRouteState {
  layout: 'app' | 'callback'
  page: WorkspacePageId | 'callback'
  projectId: string | null
  conversationId: string | null
  mode: ConversationMode | null
  traceTab: UiRightPanelTab
  settingsSection: string | null
  callbackKind: 'openclaw' | 'feishu' | null
}
