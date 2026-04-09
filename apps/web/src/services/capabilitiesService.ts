import type { CapabilitiesDomainState } from '../domains/workbenchTypes'
import type { LoomBootstrapPayload } from '../types'

export function buildCapabilitiesState(payload: LoomBootstrapPayload): CapabilitiesDomainState {
  return payload.capabilities
}
