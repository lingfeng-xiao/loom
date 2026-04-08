import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useComposerStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.composer,
    updateDraft: actions.updateDraft,
  }
}
