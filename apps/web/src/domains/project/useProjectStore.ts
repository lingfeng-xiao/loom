import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useProjectStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.project,
    createProject: actions.createProject,
    createConversation: actions.createConversation,
    moveConversation: actions.moveConversation,
    handleHeaderAction: actions.handleHeaderAction,
  }
}
