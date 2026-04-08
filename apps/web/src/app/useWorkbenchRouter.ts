import { useEffect, useMemo, useState } from 'react'
import type { LoomRouteState } from './routeTypes'
import { buildWorkspacePath, parseLoomLocation } from '../routing/loomRoutes'

interface UseWorkbenchRouterOptions {
  fallbackProjectId: string
  fallbackConversationId: string | null
  fallbackMode: string | null
  fallbackSettingsSection: string | null
}

function readCurrentRoute(options: UseWorkbenchRouterOptions): LoomRouteState {
  return parseLoomLocation(window.location.pathname, window.location.search, options.fallbackProjectId, options.fallbackConversationId)
}

export function useWorkbenchRouter(options: UseWorkbenchRouterOptions) {
  const normalizedOptions = useMemo(
    () => ({
      fallbackProjectId: options.fallbackProjectId,
      fallbackConversationId: options.fallbackConversationId,
      fallbackMode: options.fallbackMode,
      fallbackSettingsSection: options.fallbackSettingsSection,
    }),
    [options.fallbackConversationId, options.fallbackMode, options.fallbackProjectId, options.fallbackSettingsSection],
  )

  const [route, setRoute] = useState<LoomRouteState>(() => readCurrentRoute(normalizedOptions))

  useEffect(() => {
    setRoute(readCurrentRoute(normalizedOptions))
  }, [normalizedOptions])

  useEffect(() => {
    const onPopState = () => setRoute(readCurrentRoute(normalizedOptions))
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [normalizedOptions])

  const navigate = (next: LoomRouteState, replace = false) => {
    const path = buildWorkspacePath(next)
    if (replace) {
      window.history.replaceState({}, '', path)
    } else {
      window.history.pushState({}, '', path)
    }
    setRoute(next)
  }

  useEffect(() => {
    if (window.location.pathname === '/') {
      navigate(
        {
          layout: 'app',
          page: 'conversation',
          projectId: normalizedOptions.fallbackProjectId,
          conversationId: normalizedOptions.fallbackConversationId,
          mode: (normalizedOptions.fallbackMode as LoomRouteState['mode']) ?? null,
          traceTab: 'tasks',
          settingsSection: normalizedOptions.fallbackSettingsSection,
          callbackKind: null,
        },
        true,
      )
    }
  }, [normalizedOptions])

  return {
    route,
    navigate,
  }
}
