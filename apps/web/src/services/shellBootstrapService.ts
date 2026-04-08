import type { LoomBootstrapPayload } from '../types'
import type { BootstrapSourceMode, BootstrapSourceResolution } from '../app/bootstrapSource'
import { LoomApiError, createLoomSdk } from '../sdk/loomApiClient'

export interface BootstrapLoadResult {
  payload: LoomBootstrapPayload
  mode: BootstrapSourceMode
  resolution: BootstrapSourceResolution
  error: string | null
}

export async function loadBootstrapSnapshot(
  baseUrl: string,
  fallbackPayload: LoomBootstrapPayload,
  fallbackError: string,
  mode: BootstrapSourceMode,
  signal?: AbortSignal,
): Promise<BootstrapLoadResult> {
  const sdk = createLoomSdk({ baseUrl })

  if (mode === 'fallback') {
    return {
      payload: fallbackPayload,
      mode,
      resolution: 'fallback',
      error: null,
    }
  }

  try {
    const payload = await sdk.shell.bootstrap(signal)
    return {
      payload,
      mode,
      resolution: 'remote',
      error: null,
    }
  } catch (error) {
    return {
      payload: fallbackPayload,
      mode,
      resolution: 'fallback',
      error: error instanceof LoomApiError || error instanceof Error ? error.message : fallbackError,
    }
  }
}
