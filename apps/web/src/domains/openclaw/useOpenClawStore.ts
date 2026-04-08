import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useOpenClawStore() {
  const { state } = useWorkbenchContext()
  return state.openClaw
}
