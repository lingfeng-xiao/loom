import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { BootstrapSourceViewModel } from './bootstrapSource'
import { adaptBootstrapToWorkbench } from './bootstrapAdapter'
import type { LoomRouteState, UiRightPanelTab } from './routeTypes'
import type { WorkbenchDomainState, ComposerDraftState } from '../domains/workbenchTypes'
import type { ConversationMode, LoomBootstrapPayload, WorkspacePageId } from '../types'

interface LoomWorkbenchProviderProps {
  payload: LoomBootstrapPayload
  route: LoomRouteState
  loading: boolean
  error: string | null
  bootstrapSource: BootstrapSourceViewModel
  onCycleBootstrapSource: () => void
  navigate: (next: LoomRouteState, replace?: boolean) => void
  children: ReactNode
}

interface WorkbenchActions {
  openConversation: (conversationId: string) => void
  openPage: (page: WorkspacePageId) => void
  setMode: (mode: ConversationMode) => void
  setRightPanelTab: (tab: UiRightPanelTab) => void
  setSettingsSection: (section: string) => void
  updateDraft: (text: string) => void
  setScrollPosition: (conversationId: string, scrollTop: number) => void
  openGlobalSearch: () => void
  closeGlobalSearch: () => void
  toggleLeftSidebar: () => void
  cycleBootstrapSourceMode: () => void
  handlePrimaryAction: (actionId: string) => void
  handleHeaderAction: (actionId: string) => void
  handleSystemEntry: (entryId: string) => void
}

interface WorkbenchContextValue {
  state: WorkbenchDomainState
  actions: WorkbenchActions
}

const WorkbenchContext = createContext<WorkbenchContextValue | null>(null)

function defaultConversationId(payload: LoomBootstrapPayload): string | null {
  return payload.pinnedConversations[0]?.id ?? payload.recentConversations[0]?.id ?? null
}

function findConversationMode(payload: LoomBootstrapPayload, conversationId: string | null) {
  return [...payload.pinnedConversations, ...payload.recentConversations].find((item) => item.id === conversationId)?.mode ?? payload.activeMode
}

export function LoomWorkbenchProvider({
  payload,
  route,
  loading,
  error,
  bootstrapSource,
  onCycleBootstrapSource,
  navigate,
  children,
}: LoomWorkbenchProviderProps) {
  const [globalSearchOpen, setGlobalSearchOpen] = useState(false)
  const [commandPaletteOpen] = useState(false)
  const [leftSidebarCollapsed, setLeftSidebarCollapsed] = useState(false)
  const [draftsByConversation, setDraftsByConversation] = useState<Record<string, ComposerDraftState>>({})
  const [scrollPositions, setScrollPositions] = useState<Record<string, number>>({})

  const openConversation = (conversationId: string) => {
    navigate({
      ...route,
      layout: 'app',
      page: 'conversation',
      projectId: payload.project.id,
      conversationId,
      mode: findConversationMode(payload, conversationId),
      callbackKind: null,
    })
  }

  const openPage = (page: WorkspacePageId) => {
    navigate({
      ...route,
      layout: 'app',
      page,
      projectId: payload.project.id,
      conversationId: page === 'conversation' ? route.conversationId ?? defaultConversationId(payload) : route.conversationId ?? defaultConversationId(payload),
      settingsSection: page === 'settings' ? route.settingsSection ?? payload.settings.tabs[0] ?? 'Models' : route.settingsSection,
      callbackKind: null,
    })
  }

  const actions: WorkbenchActions = {
    openConversation,
    openPage,
    setMode(mode) {
      navigate({
        ...route,
        layout: 'app',
        page: 'conversation',
        projectId: payload.project.id,
        conversationId: route.conversationId ?? defaultConversationId(payload),
        mode,
        callbackKind: null,
      })
    },
    setRightPanelTab(tab) {
      navigate({
        ...route,
        layout: 'app',
        traceTab: tab,
        callbackKind: null,
      })
    },
    setSettingsSection(section) {
      navigate({
        ...route,
        layout: 'app',
        page: 'settings',
        projectId: payload.project.id,
        settingsSection: section,
        callbackKind: null,
      })
    },
    updateDraft(text) {
      const key = route.conversationId ?? 'global'
      setDraftsByConversation((current) => {
        const existing = current[key]
        return {
          ...current,
          [key]: {
            draftText: text,
            attachments: existing?.attachments ?? [],
            allowActions: existing?.allowActions ?? true,
            allowMemory: existing?.allowMemory ?? true,
            submitting: false,
          },
        }
      })
    },
    setScrollPosition(conversationId, scrollTop) {
      setScrollPositions((current) => ({
        ...current,
        [conversationId]: scrollTop,
      }))
    },
    openGlobalSearch() {
      setGlobalSearchOpen(true)
    },
    closeGlobalSearch() {
      setGlobalSearchOpen(false)
    },
    toggleLeftSidebar() {
      setLeftSidebarCollapsed((current) => !current)
    },
    cycleBootstrapSourceMode() {
      onCycleBootstrapSource()
    },
    handlePrimaryAction(actionId) {
      if (actionId === 'new-thread') {
        openConversation(defaultConversationId(payload) ?? 'conversation-shell')
        return
      }

      if (actionId === 'capabilities') {
        openPage('capabilities')
        return
      }

      if (actionId === 'automation') {
        openPage('openclaw')
        return
      }

      openConversation(payload.recentConversations[2]?.id ?? defaultConversationId(payload) ?? 'conversation-shell')
    },
    handleHeaderAction(actionId) {
      if (actionId === 'workspace') {
        openConversation(defaultConversationId(payload) ?? 'conversation-shell')
        return
      }

      if (actionId === 'submit') {
        navigate({
          ...route,
          layout: 'app',
          page: 'conversation',
          projectId: payload.project.id,
          conversationId: route.conversationId ?? defaultConversationId(payload),
          mode: 'action',
          callbackKind: null,
        })
        return
      }

      openPage('settings')
    },
    handleSystemEntry(entryId) {
      if (entryId === 'settings') {
        openPage('settings')
        return
      }

      if (entryId === 'files') {
        openPage('files')
        return
      }

      openPage('memory')
    },
  }

  const state = useMemo(
    () =>
      adaptBootstrapToWorkbench(payload, {
        route,
        loading,
        error,
        bootstrapSource,
        draftsByConversation,
        scrollPositions,
        globalSearchOpen,
        commandPaletteOpen,
        leftSidebarCollapsed,
      }),
    [bootstrapSource, commandPaletteOpen, draftsByConversation, error, globalSearchOpen, leftSidebarCollapsed, loading, payload, route, scrollPositions],
  )

  return <WorkbenchContext.Provider value={{ state, actions }}>{children}</WorkbenchContext.Provider>
}

export function useWorkbenchContext() {
  const context = useContext(WorkbenchContext)
  if (!context) {
    throw new Error('useWorkbenchContext must be used inside LoomWorkbenchProvider')
  }
  return context
}
