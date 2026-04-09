import type { ConversationNavItem, SidebarPrimaryAction, SidebarSystemEntry, SidebarThreadGroup } from '../frontendTypes'
import { WorkbenchIcon } from './WorkbenchIcon'

interface LoomGlobalSidebarProps {
  projectName: string
  projectEyebrow: string
  primaryActions: SidebarPrimaryAction[]
  threadGroups: SidebarThreadGroup[]
  systemEntries: SidebarSystemEntry[]
  activeThreadId: string | null
  onPrimaryAction: (actionId: string) => void
  onOpenConversation: (conversationId: string) => void
  onSystemSelect: (entryId: string) => void
}

function statusLabel(item: ConversationNavItem) {
  if (item.pinned) {
    return '置顶'
  }

  switch (item.status) {
    case 'active':
      return '进行中'
    case 'blocked':
      return '阻塞'
    case 'idle':
    default:
      return '空闲'
  }
}

export function LoomGlobalSidebar({
  projectName,
  projectEyebrow,
  primaryActions,
  threadGroups,
  systemEntries,
  activeThreadId,
  onPrimaryAction,
  onOpenConversation,
  onSystemSelect,
}: LoomGlobalSidebarProps) {
  return (
    <aside className="sidebar codexSidebar">
      <div className="sidebarBrand">
        <div className="sidebarBrandMark">L</div>
        <div className="sidebarBrandCopy">
          <p>{projectEyebrow}</p>
          <strong>{projectName}</strong>
        </div>
      </div>

      <div className="sidebarPrimaryActions">
        {primaryActions.map((action) => (
          <button
            className={`sidebarPrimaryAction ${action.active ? 'active' : ''}`}
            key={action.id}
            onClick={() => onPrimaryAction(action.id)}
            type="button"
          >
            <WorkbenchIcon name={action.icon} />
            <span>{action.label}</span>
          </button>
        ))}
      </div>

      <div className="sidebarThreadShell">
        {threadGroups.map((group) => (
          <section className="sidebarThreadGroup" key={group.id}>
            <div className="sidebarGroupTitle">{group.label}</div>
            <div className="sidebarThreadList">
              {group.threads.map((thread) => (
                <button
                  className={`sidebarThreadItem ${activeThreadId === thread.id ? 'active' : ''}`}
                  key={thread.id}
                  onClick={() => onOpenConversation(thread.id)}
                  type="button"
                >
                  <div className="sidebarThreadTitleRow">
                    <span>{thread.title}</span>
                    <small>{thread.lastActiveAt}</small>
                  </div>
                  <div className="sidebarThreadMetaRow">
                    <small>{statusLabel(thread)}</small>
                    <small>{thread.group ?? group.label}</small>
                  </div>
                </button>
              ))}
            </div>
          </section>
        ))}
      </div>

      <div className="sidebarSystemArea">
        {systemEntries.map((entry) => (
          <button
            className={`sidebarSystemEntry ${entry.active ? 'active' : ''}`}
            key={entry.id}
            onClick={() => onSystemSelect(entry.id)}
            type="button"
          >
            <WorkbenchIcon name={entry.icon} />
            <span>{entry.label}</span>
            <small>{entry.meta}</small>
          </button>
        ))}
      </div>
    </aside>
  )
}
