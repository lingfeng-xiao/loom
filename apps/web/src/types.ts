export interface WorkspaceSettings {
  workspaceName: string
  supportEmail: string
  docsUrl: string
  defaultRefreshIntervalSeconds: number
  updatedAt: string
}

export interface ReleaseOverview {
  installRoot: string
  systemdUnit: string
  registry: string
}

export interface SetupTask {
  id: string
  title: string
  description: string
  docsUrl: string
}

export interface ExtensionPoint {
  name: string
  target: string
  description: string
}

export interface BootstrapPayload {
  appName: string
  description: string
  workspaceSettings: WorkspaceSettings
  releaseOverview: ReleaseOverview
  setupTasks: SetupTask[]
  extensionPoints: ExtensionPoint[]
}

export interface NodeProbe {
  name: string
  kind: string
  target: string
  status: 'up' | 'down' | 'degraded' | 'unknown'
  detail: string | null
  recordedAt: string
}

export interface NodeRecord {
  id: string
  name: string
  type: string
  host: string
  status: 'up' | 'down' | 'degraded' | 'unknown'
  tags: string[]
  capabilities: string[]
  lastHeartbeat: string | null
  createdAt: string
  updatedAt: string
  probes: NodeProbe[]
}

export interface ApiEnvelope<T> {
  data: T
}
