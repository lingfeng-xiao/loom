import { useEffect, useMemo, useState } from 'react'
import { WorkbenchIcon } from '../WorkbenchIcon'
import type { ConversationThreadViewModel, SidebarPrimaryAction, SidebarSystemEntry, SidebarThreadGroup } from './shellTypes'

interface WorkspaceSidebarProps {
  primaryActions: SidebarPrimaryAction[]
  threadGroups: SidebarThreadGroup[]
  systemEntries: SidebarSystemEntry[]
  activeThreadId: string | null
  onPrimaryAction: (actionId: string) => void
  onOpenConversation: (conversationId: string) => void
  onSystemSelect: (entryId: string) => void
  onCreateProject: () => void
}

type ThreadSortMode = 'recent' | 'title'

function sortThreads(threads: ConversationThreadViewModel[], mode: ThreadSortMode) {
  if (mode === 'title') {
    return [...threads].sort((left, right) => left.title.localeCompare(right.title, 'zh-CN'))
  }

  return threads
}

function createExpandedState(groups: SidebarThreadGroup[]) {
  return Object.fromEntries(groups.map((group) => [group.id, true])) as Record<string, boolean>
}

export function WorkspaceSidebar({
  primaryActions,
  threadGroups,
  systemEntries,
  activeThreadId,
  onPrimaryAction,
  onOpenConversation,
  onSystemSelect,
  onCreateProject,
}: WorkspaceSidebarProps) {
  const [expandedProjects, setExpandedProjects] = useState<Record<string, boolean>>(() => createExpandedState(threadGroups))
  const [sortMode, setSortMode] = useState<ThreadSortMode>('recent')

  useEffect(() => {
    setExpandedProjects((current) => {
      const next = { ...current }
      threadGroups.forEach((group) => {
        if (!(group.id in next)) {
          next[group.id] = true
        }
      })
      return next
    })
  }, [threadGroups])

  const sortedGroups = useMemo(
    () =>
      threadGroups.map((group) => ({
        ...group,
        threads: sortThreads(group.threads, sortMode),
      })),
    [sortMode, threadGroups],
  )

  const navigationEntries = [...primaryActions, ...systemEntries.filter((entry) => entry.id !== 'settings')]
  const settingsEntry = systemEntries.find((entry) => entry.id === 'settings')

  return (
    <aside className="codexSidebar">
      <nav aria-label="主导航" className="sidebarNavList">
        {navigationEntries.map((entry) => (
          <button
            className={`uiRowItem sidebarNavItem ${entry.active ? 'active' : ''}`}
            key={entry.id}
            onClick={() => ('targetPage' in entry ? onPrimaryAction(entry.id) : onSystemSelect(entry.id))}
            type="button"
          >
            <WorkbenchIcon name={entry.icon} size={16} />
            <span>{entry.label}</span>
          </button>
        ))}
      </nav>

      <section aria-label="项目与会话" className="sidebarHistoryShell">
        <div className="sidebarSectionHeader">
          <span className="sidebarSectionTitle">项目</span>
          <div className="sidebarSectionActions">
            <button
              aria-label={sortMode === 'recent' ? '切换到按标题排序' : '切换到按最近排序'}
              className="uiButton uiButton-ghost uiButton-icon sidebarUtilityButton"
              onClick={() => setSortMode((current) => (current === 'recent' ? 'title' : 'recent'))}
              type="button"
            >
              <WorkbenchIcon name="sort" size={14} />
            </button>
            <button aria-label="新建项目" className="uiButton uiButton-ghost uiButton-icon sidebarUtilityButton" onClick={onCreateProject} type="button">
              <WorkbenchIcon name="plus" size={14} />
            </button>
          </div>
        </div>

        <div className="sidebarProjectGroups">
          {sortedGroups.map((group) => {
            const expanded = expandedProjects[group.id] !== false
            const containsActiveThread = group.threads.some((thread) => thread.id === activeThreadId)

            return (
              <div className="sidebarProjectGroup" key={group.id}>
                <button
                  aria-expanded={expanded}
                  className={`uiRowItem sidebarProjectRow ${containsActiveThread ? 'sidebarProjectRow-current' : ''}`}
                  onClick={() =>
                    setExpandedProjects((current) => ({
                      ...current,
                      [group.id]: !expanded,
                    }))
                  }
                  type="button"
                >
                  <span className="sidebarProjectLeading">
                    <WorkbenchIcon name={expanded ? 'chevronDown' : 'chevronRight'} size={14} />
                    <WorkbenchIcon name={expanded ? 'folderOpen' : 'folder'} size={15} />
                  </span>
                  <span className="sidebarProjectLabel">{group.label}</span>
                </button>

                {expanded ? (
                  <div className="sidebarProjectThreadList">
                    {group.threads.map((thread) => (
                      <button
                        className={`uiRowItem sidebarProjectThread sidebarProjectThread-textOnly ${thread.id === activeThreadId ? 'active' : ''}`}
                        key={thread.id}
                        onClick={() => onOpenConversation(thread.id)}
                        type="button"
                      >
                        <span>{thread.title}</span>
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            )
          })}
        </div>
      </section>

      {settingsEntry ? (
        <button className="uiRowItem sidebarSettingsButton" onClick={() => onSystemSelect(settingsEntry.id)} type="button">
          <WorkbenchIcon name={settingsEntry.icon} size={16} />
          <span>{settingsEntry.label}</span>
        </button>
      ) : null}
    </aside>
  )
}
