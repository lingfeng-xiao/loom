import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useMemoryStore() {
  const { state } = useWorkbenchContext()
  return state.memory
}
