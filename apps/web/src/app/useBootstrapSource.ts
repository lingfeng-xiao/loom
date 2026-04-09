import { useEffect, useState } from 'react'
import type { BootstrapSourceViewModel, BootstrapSourceMode } from './bootstrapSource'
import {
  BOOTSTRAP_SOURCE_STORAGE_KEY,
  buildBootstrapSourceViewModel,
  cycleBootstrapSourceMode,
  normalizeBootstrapSourceMode,
} from './bootstrapSource'
import type { LoomBootstrapPayload } from '../types'
import { loadBootstrapSnapshot } from '../services/shellBootstrapService'

interface BootstrapSourceState {
  payload: LoomBootstrapPayload
  loading: boolean
  error: string | null
  source: BootstrapSourceViewModel
}

function readBootstrapMode(): BootstrapSourceMode {
  if (typeof window === 'undefined') {
    return 'auto'
  }

  return normalizeBootstrapSourceMode(window.localStorage.getItem(BOOTSTRAP_SOURCE_STORAGE_KEY))
}

export function useBootstrapSource(baseUrl: string, fallbackPayload: LoomBootstrapPayload, fallbackError: string) {
  const [mode, setMode] = useState<BootstrapSourceMode>(() => readBootstrapMode())
  const [refreshKey, setRefreshKey] = useState(0)
  const [state, setState] = useState<BootstrapSourceState>({
    payload: fallbackPayload,
    loading: true,
    error: null,
    source: buildBootstrapSourceViewModel('auto', 'fallback', true, null),
  })

  useEffect(() => {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(BOOTSTRAP_SOURCE_STORAGE_KEY, mode)
  }, [mode])

  useEffect(() => {
    let cancelled = false
    const controller = new AbortController()

    const run = async () => {
      setState((current) => ({
        ...current,
        loading: true,
        error: null,
        source: buildBootstrapSourceViewModel(mode, mode === 'fallback' ? 'fallback' : 'remote', true, null),
      }))

      const result = await loadBootstrapSnapshot(baseUrl, fallbackPayload, fallbackError, mode, controller.signal)
      if (cancelled) {
        return
      }

      setState({
        payload: result.payload,
        loading: false,
        error: result.error,
        source: buildBootstrapSourceViewModel(result.mode, result.resolution, false, result.error),
      })
    }

    void run()

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [baseUrl, fallbackError, fallbackPayload, mode, refreshKey])

  return {
    ...state,
    mode,
    setMode,
    cycleMode: () => setMode((current) => cycleBootstrapSourceMode(current)),
    refresh: () => setRefreshKey((current) => current + 1),
  }
}
