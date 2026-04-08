import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useConversationStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.conversation,
    openConversation: actions.openConversation,
    openPage: actions.openPage,
    handlePrimaryAction: actions.handlePrimaryAction,
    handleSystemEntry: actions.handleSystemEntry,
    setMode: actions.setMode,
  }
}
