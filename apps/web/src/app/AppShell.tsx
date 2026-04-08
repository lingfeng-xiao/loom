import { useState } from 'react'
import { WorkspaceRightRail } from '../components/rails/WorkspaceRightRail'
import { WorkspaceSidebar } from '../components/shell/WorkspaceSidebar'
import { WorkspaceTitleBar } from '../components/shell/WorkspaceTitleBar'
import { useContextStore } from '../domains/context/useContextStore'
import { useConversationStore } from '../domains/conversation/useConversationStore'
import { useProjectStore } from '../domains/project/useProjectStore'
import { useTraceStore } from '../domains/trace/useTraceStore'
import { useUiStore } from '../domains/ui/useUiStore'
import { CapabilitiesPage } from '../pages/CapabilitiesPage'
import { ConversationWorkspacePage } from '../pages/ConversationWorkspacePage'
import { FilesPage } from '../pages/FilesPage'
import { MemoryPage } from '../pages/MemoryPage'
import { OpenClawPage } from '../pages/OpenClawPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { SettingsPage } from '../pages/SettingsPage'

export function AppShell() {
  const [projectCreationOpen, setProjectCreationOpen] = useState(false)
  const [rightRailCollapsed, setRightRailCollapsed] = useState(false)
  const project = useProjectStore()
  const conversation = useConversationStore()
  const trace = useTraceStore()
  const context = useContextStore()
  const ui = useUiStore()
  const source = ui.bootstrapSource
  const showSourceNotice = !ui.loading && (source.mode !== 'auto' || source.resolution !== 'remote')

  const openConversation = (conversationId: string) => {
    setProjectCreationOpen(false)
    conversation.openConversation(conversationId)
  }

  const handlePrimaryAction = (actionId: string) => {
    setProjectCreationOpen(false)
    conversation.handlePrimaryAction(actionId)
  }

  const handleSystemEntry = (entryId: string) => {
    setProjectCreationOpen(false)
    conversation.handleSystemEntry(entryId)
  }

  return (
    <div className="appShell">
      <WorkspaceTitleBar
        bootstrapSourceDetail={source.detail}
        bootstrapSourceLabel={source.label}
        environmentStatus={project.environmentStatus}
        onOpenClaw={() => conversation.openPage('openclaw')}
        onToggleBootstrapSource={ui.cycleBootstrapSourceMode}
      />

      {ui.loading ? <section className="infoBanner">正在加载最新工作台快照：{source.detail}</section> : null}
      {showSourceNotice ? <section className="infoBanner">当前工作台数据源：{source.label}</section> : null}
      {ui.error ? <section className="errorBanner">获取 bootstrap 快照失败：{ui.error}</section> : null}

      <div className={`workspace workspace-${conversation.currentPage} ${rightRailCollapsed ? 'workspace-rightRailCollapsed' : ''}`}>
        <WorkspaceSidebar
          activeThreadId={conversation.activeConversationId}
          onCreateProject={() => setProjectCreationOpen(true)}
          onOpenConversation={openConversation}
          onPrimaryAction={handlePrimaryAction}
          onSystemSelect={handleSystemEntry}
          primaryActions={conversation.primaryActions}
          systemEntries={conversation.systemEntries}
          threadGroups={conversation.threadGroups}
        />

        <main className={`mainPanel mainPanel-${conversation.currentPage}`}>
          {projectCreationOpen ? <PlaceholderPage description="项目创建流程占位，后续接入真实创建能力。" title="新建项目" /> : null}
          {!projectCreationOpen && conversation.currentPage === 'conversation' ? <ConversationWorkspacePage /> : null}
          {!projectCreationOpen && conversation.currentPage === 'capabilities' ? <CapabilitiesPage /> : null}
          {!projectCreationOpen && conversation.currentPage === 'openclaw' ? <OpenClawPage /> : null}
          {!projectCreationOpen && conversation.currentPage === 'settings' ? <SettingsPage /> : null}
          {!projectCreationOpen && conversation.currentPage === 'files' ? <FilesPage /> : null}
          {!projectCreationOpen && conversation.currentPage === 'memory' ? <MemoryPage /> : null}
        </main>

        <WorkspaceRightRail
          activeTab={trace.activeTab}
          collapsed={rightRailCollapsed}
          contextSections={context.sections}
          onTabChange={trace.setRightPanelTab}
          onToggleCollapsed={() => setRightRailCollapsed((current) => !current)}
          trace={trace}
        />
      </div>
    </div>
  )
}
