import { useState } from 'react'
import type { UiRightPanelTab } from '../frontendTypes'

export function useWorkspaceUiState(initialTab: UiRightPanelTab) {
  const [leftSidebarCollapsed, setLeftSidebarCollapsed] = useState(false)
  const [rightPanelTab, setRightPanelTab] = useState<UiRightPanelTab>(initialTab)
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false)
  const [globalSearchOpen, setGlobalSearchOpen] = useState(false)

  return {
    leftSidebarCollapsed,
    setLeftSidebarCollapsed,
    rightPanelTab,
    setRightPanelTab,
    commandPaletteOpen,
    openCommandPalette: () => setCommandPaletteOpen(true),
    closeCommandPalette: () => setCommandPaletteOpen(false),
    globalSearchOpen,
    openGlobalSearch: () => setGlobalSearchOpen(true),
    closeGlobalSearch: () => setGlobalSearchOpen(false),
  }
}
