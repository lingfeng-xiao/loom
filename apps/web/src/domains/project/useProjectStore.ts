import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useProjectStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.project,
    handleHeaderAction: actions.handleHeaderAction,
  }
}
