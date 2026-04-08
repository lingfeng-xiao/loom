import { AppShell } from './app/AppShell'
import { LoomWorkbenchProvider } from './app/LoomWorkbenchProvider'
import { useBootstrapSource } from './app/useBootstrapSource'
import { useWorkbenchRouter } from './app/useWorkbenchRouter'
import { DEFAULT_LOOM_ERROR, loomShellData } from './loomShellData'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { WelcomePage } from './pages/WelcomePage'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''

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

  if (router.route.layout === 'welcome') {
    return (
      <div className="appViewport">
        <WelcomePage
          onEnter={() =>
            router.navigate({
              layout: 'app',
              page: 'conversation',
              projectId: bootstrapSource.payload.project.id,
              conversationId: defaultConversationId(),
              mode: bootstrapSource.payload.activeMode,
              traceTab: 'tasks',
              settingsSection: bootstrapSource.payload.settings.tabs[0] ?? 'Models',
              callbackKind: null,
            })
          }
        />
      </div>
    )
  }

  if (router.route.layout === 'callback') {
    return (
      <div className="appViewport">
        <div className="appShell">
          <PlaceholderPage description="外部回调占位页" title={router.route.callbackKind === 'feishu' ? 'Feishu Callback' : 'OpenClaw Callback'} />
        </div>
      </div>
    )
  }

  return (
    <div className="appViewport">
      <LoomWorkbenchProvider
        bootstrapSource={bootstrapSource.source}
        error={bootstrapSource.error}
        loading={bootstrapSource.loading}
        onCycleBootstrapSource={bootstrapSource.cycleMode}
        navigate={router.navigate}
        payload={bootstrapSource.payload}
        route={router.route}
      >
        <AppShell />
      </LoomWorkbenchProvider>
    </div>
  )
}
