import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useUiStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.ui,
    openGlobalSearch: actions.openGlobalSearch,
    closeGlobalSearch: actions.closeGlobalSearch,
    toggleLeftSidebar: actions.toggleLeftSidebar,
    cycleBootstrapSourceMode: actions.cycleBootstrapSourceMode,
    setScrollPosition: actions.setScrollPosition,
  }
}
