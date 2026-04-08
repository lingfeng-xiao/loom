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
      <nav aria-label="工作区导航" className="sidebarNavList">
        {navigationEntries.map((entry) => (
          <button
            className={`sidebarNavItem ${entry.active ? 'active' : ''}`}
            key={entry.id}
            onClick={() => ('targetPage' in entry ? onPrimaryAction(entry.id) : onSystemSelect(entry.id))}
            type="button"
          >
            <WorkbenchIcon name={entry.icon} />
            <span>{entry.label}</span>
          </button>
        ))}
      </nav>

      <section className="sidebarHistoryShell" aria-label="项目会话">
        <div className="sidebarSectionHeader">
          <span className="sidebarSectionTitle">项目</span>

          <div className="sidebarSectionActions">
            <button
              aria-label={sortMode === 'recent' ? '当前按最近更新排序，点击改为标题排序' : '当前按标题排序，点击改为最近更新排序'}
              className="sidebarUtilityButton"
              onClick={() => setSortMode((current) => (current === 'recent' ? 'title' : 'recent'))}
              type="button"
            >
              <WorkbenchIcon name="sort" />
            </button>
            <button aria-label="新建项目" className="sidebarUtilityButton" onClick={onCreateProject} type="button">
              <WorkbenchIcon name="plus" />
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
                  className={`sidebarProjectRow ${containsActiveThread ? 'active' : ''}`}
                  onClick={() =>
                    setExpandedProjects((current) => ({
                      ...current,
                      [group.id]: !expanded,
                    }))
                  }
                  type="button"
                >
                  <WorkbenchIcon name={expanded ? 'folderOpen' : 'folder'} />
                  <span className="sidebarProjectLabel">{group.label}</span>
                </button>

                {expanded ? (
                  <div className="sidebarProjectThreadList">
                    {group.threads.map((thread) => (
                      <button
                        className={`sidebarProjectThread ${thread.id === activeThreadId ? 'active' : ''}`}
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
        <button className={`sidebarSettingsButton ${settingsEntry.active ? 'active' : ''}`} onClick={() => onSystemSelect(settingsEntry.id)} type="button">
          <WorkbenchIcon name={settingsEntry.icon} />
          <span>{settingsEntry.label}</span>
        </button>
      ) : null}
    </aside>
  )
}
