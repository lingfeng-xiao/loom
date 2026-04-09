import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useTraceStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.trace,
    setRightPanelTab: actions.setRightPanelTab,
  }
}
