import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useCapabilitiesStore() {
  const { state } = useWorkbenchContext()
  return state.capabilities
}
