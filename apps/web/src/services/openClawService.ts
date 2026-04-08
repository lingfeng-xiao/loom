import type { OpenClawDomainState } from '../domains/workbenchTypes'
import type { LoomBootstrapPayload } from '../types'

export function buildOpenClawState(payload: LoomBootstrapPayload): OpenClawDomainState {
  return payload.openClaw
}
