import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useContextStore() {
  const { state } = useWorkbenchContext()
  return state.context
}
