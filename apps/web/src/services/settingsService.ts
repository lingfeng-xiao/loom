import type { SettingsDomainState } from '../domains/workbenchTypes'
import type { LoomBootstrapPayload } from '../types'

export function buildSettingsState(payload: LoomBootstrapPayload, activeSection: string): SettingsDomainState {
  return {
    ...payload.settings,
    activeSection,
  }
}
