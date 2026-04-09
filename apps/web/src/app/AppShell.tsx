import { Suspense, lazy, useEffect, useState } from 'react'
import { WorkspaceRightRail } from '../components/rails/WorkspaceRightRail'
import { WorkspaceSidebar } from '../components/shell/WorkspaceSidebar'
import { WorkspaceTitleBar } from '../components/shell/WorkspaceTitleBar'
import { useContextStore } from '../domains/context/useContextStore'
import { useConversationStore } from '../domains/conversation/useConversationStore'
import { useProjectStore } from '../domains/project/useProjectStore'
import { useTraceStore } from '../domains/trace/useTraceStore'
import { useUiStore } from '../domains/ui/useUiStore'
import { ConversationWorkspacePage } from '../pages/ConversationWorkspacePage'
import { useTheme } from './theme'

const CapabilitiesPage = lazy(() => import('../pages/CapabilitiesPage').then((module) => ({ default: module.CapabilitiesPage })))
const OpenClawPage = lazy(() => import('../pages/OpenClawPage').then((module) => ({ default: module.OpenClawPage })))
const SettingsPage = lazy(() => import('../pages/SettingsPage').then((module) => ({ default: module.SettingsPage })))
const FilesPage = lazy(() => import('../pages/FilesPage').then((module) => ({ default: module.FilesPage })))
const MemoryPage = lazy(() => import('../pages/MemoryPage').then((module) => ({ default: module.MemoryPage })))

export function AppShell() {
  const theme = useTheme()
  const [rightRailCollapsed, setRightRailCollapsed] = useState(() => {
    const saved = window.localStorage.getItem('loom-right-rail-collapsed')
    return saved == null ? false : saved === 'true'
  })
  const [selectedThinkingMessageId, setSelectedThinkingMessageId] = useState<string | null>(null)
  const project = useProjectStore()
  const conversation = useConversationStore()
  const trace = useTraceStore()
  const context = useContextStore()
  const ui = useUiStore()
  const source = ui.bootstrapSource
  const showSourceNotice = !ui.loading && (source.mode !== 'auto' || source.resolution !== 'remote')
  const showRightRail = conversation.currentPage === 'conversation'

  useEffect(() => {
    window.localStorage.setItem('loom-right-rail-collapsed', String(rightRailCollapsed))
  }, [rightRailCollapsed])

  const selectedThinkingMessage =
    conversation.messages.find((message) => message.id === selectedThinkingMessageId && message.kind === 'thinking_summary') ??
    null

  const lazyPageFallback = <section className="infoBanner">正在加载页面模块...</section>

  return (
    <div className="appShell">
      <WorkspaceTitleBar
        bootstrapSourceDetail={source.detail}
        bootstrapSourceLabel={source.label}
        environmentStatus={project.environmentStatus}
        onOpenClaw={() => conversation.openPage('openclaw')}
        onThemeChange={theme.setPreference}
        onToggleBootstrapSource={ui.cycleBootstrapSourceMode}
        themePreference={theme.preference}
      />

      {ui.loading ? <section className="infoBanner">正在加载最新工作区快照：{source.detail}</section> : null}
      {showSourceNotice ? <section className="infoBanner">当前数据源：{source.label}</section> : null}
      {ui.error ? <section className="errorBanner">加载工作区快照失败：{ui.error}</section> : null}
      {ui.workspaceError ? <section className="errorBanner">远端工作区刷新失败：{ui.workspaceError}</section> : null}

      <div
        className={`workspace workspace-${conversation.currentPage} ${
          showRightRail ? '' : 'workspace-noRightRail'
        } ${showRightRail && rightRailCollapsed ? 'workspace-rightRailCollapsed' : ''}`}
      >
        <WorkspaceSidebar
          activeThreadId={conversation.activeConversationId}
          onCreateProject={() => conversation.handlePrimaryAction('new-thread')}
          onOpenConversation={conversation.openConversation}
          onPrimaryAction={conversation.handlePrimaryAction}
          onSystemSelect={conversation.handleSystemEntry}
          primaryActions={conversation.primaryActions}
          systemEntries={conversation.systemEntries}
          threadGroups={conversation.threadGroups}
        />

        <main className={`mainPanel mainPanel-${conversation.currentPage}`}>
          {conversation.currentPage === 'conversation' ? (
            <ConversationWorkspacePage
              onOpenThinkingMessage={(messageId) => {
                setSelectedThinkingMessageId(messageId)
                trace.setRightPanelTab('tasks')
                setRightRailCollapsed(false)
              }}
            />
          ) : null}
          {conversation.currentPage === 'capabilities' ? (
            <Suspense fallback={lazyPageFallback}>
              <CapabilitiesPage />
            </Suspense>
          ) : null}
          {conversation.currentPage === 'openclaw' ? (
            <Suspense fallback={lazyPageFallback}>
              <OpenClawPage />
            </Suspense>
          ) : null}
          {conversation.currentPage === 'settings' ? (
            <Suspense fallback={lazyPageFallback}>
              <SettingsPage />
            </Suspense>
          ) : null}
          {conversation.currentPage === 'files' ? (
            <Suspense fallback={lazyPageFallback}>
              <FilesPage />
            </Suspense>
          ) : null}
          {conversation.currentPage === 'memory' ? (
            <Suspense fallback={lazyPageFallback}>
              <MemoryPage />
            </Suspense>
          ) : null}
        </main>

        {showRightRail ? (
          <WorkspaceRightRail
            activeTab={trace.activeTab}
            collapsed={rightRailCollapsed}
            contextSections={context.sections}
            onClearThinkingSelection={() => setSelectedThinkingMessageId(null)}
            onTabChange={trace.setRightPanelTab}
            onToggleCollapsed={() => setRightRailCollapsed((current) => !current)}
            selectedThinkingMessage={selectedThinkingMessage}
            trace={trace}
          />
        ) : null}
      </div>
    </div>
  )
}
