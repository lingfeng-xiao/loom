import type { ConversationSummary } from '../../types'

export type WorkbenchIconName =
  | 'compose'
  | 'spark'
  | 'automation'
  | 'pulse'
  | 'settings'
  | 'search'
  | 'switch'
  | 'more'
  | 'submit'
  | 'thread'
  | 'status'
  | 'files'
  | 'memory'
  | 'chat'
  | 'folder'
  | 'folderOpen'
  | 'chevronDown'
  | 'chevronRight'
  | 'plus'
  | 'sort'
  | 'paperclip'
  | 'slash'
  | 'bolt'
  | 'send'
  | 'tasks'
  | 'terminal'
  | 'panelCollapse'
  | 'panelExpand'

export interface ConversationThreadViewModel extends ConversationSummary {
  group: string
  lastActiveAt: string
  unread?: boolean
  selected?: boolean
}

export interface SidebarPrimaryAction {
  id: string
  label: string
  hint: string
  icon: WorkbenchIconName
  targetPage?: string
  active?: boolean
}

export interface SidebarThreadGroup {
  id: string
  label: string
  threads: ConversationThreadViewModel[]
}

export interface SidebarSystemEntry {
  id: string
  label: string
  meta: string
  icon: WorkbenchIconName
  active?: boolean
}

export interface WorkspaceHeaderAction {
  id: string
  label: string
  icon: WorkbenchIconName
  tone?: 'default' | 'primary'
}
