import { useWorkbenchContext } from '../../app/LoomWorkbenchProvider'

export function useSettingsStore() {
  const { state, actions } = useWorkbenchContext()
  return {
    ...state.settings,
    setActiveSection: actions.setSettingsSection,
    updateLlmSettings: actions.updateLlmSettings,
    testLlmSettings: actions.testLlmSettings,
  }
}
