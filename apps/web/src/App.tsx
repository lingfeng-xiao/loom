import { AppShell } from './app/AppShell'
import { LoomWorkbenchProvider } from './app/LoomWorkbenchProvider'
import { ThemeProvider } from './app/theme'
import { useBootstrapSource } from './app/useBootstrapSource'
import { useWorkbenchRouter } from './app/useWorkbenchRouter'
import { DEFAULT_LOOM_ERROR, loomShellData } from './loomShellData'
import { PlaceholderPage } from './pages/PlaceholderPage'

function resolveApiBase() {
  const configured = import.meta.env.VITE_API_BASE
  if (configured) {
    return configured
  }

  const { port } = window.location
  if (port === '3000') {
    return ''
  }

  return ''
}

const API_BASE = resolveApiBase()

function defaultConversationId() {
  return loomShellData.pinnedConversations[0]?.id ?? loomShellData.recentConversations[0]?.id ?? null
}

export default function App() {
  const bootstrapSource = useBootstrapSource(API_BASE, loomShellData, DEFAULT_LOOM_ERROR)
  const router = useWorkbenchRouter({
    fallbackProjectId: bootstrapSource.payload.project.id,
    fallbackConversationId: bootstrapSource.payload.pinnedConversations[0]?.id ?? bootstrapSource.payload.recentConversations[0]?.id ?? defaultConversationId(),
    fallbackMode: bootstrapSource.payload.activeMode,
    fallbackSettingsSection: bootstrapSource.payload.settings.tabs[0] ?? 'Models',
  })

  if (router.route.layout === 'callback') {
    return (
      <div className="appViewport">
        <div className="appShell">
          <PlaceholderPage description="外部回调占位页" title={router.route.callbackKind === 'feishu' ? '飞书回调' : 'OpenClaw 回调'} />
        </div>
      </div>
    )
  }

  return (
    <div className="appViewport">
      <ThemeProvider>
        <LoomWorkbenchProvider
          apiBaseUrl={API_BASE}
          bootstrapSource={bootstrapSource.source}
          error={bootstrapSource.error}
          loading={bootstrapSource.loading}
          onCycleBootstrapSource={bootstrapSource.cycleMode}
          onRefreshPayload={bootstrapSource.refresh}
          navigate={router.navigate}
          payload={bootstrapSource.payload}
          route={router.route}
        >
          <AppShell />
        </LoomWorkbenchProvider>
      </ThemeProvider>
    </div>
  )
}
