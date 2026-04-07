export interface ApiEnvelope<T> {
  data: T
  meta?: Record<string, unknown>
}

export type ProbeStatus = 'up' | 'down' | 'degraded' | 'unknown'

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

export interface ProbeRecord {
  name: string
  kind: string
  target: string
  status: ProbeStatus
  detail: string | null
  recordedAt: string
}

export interface NodeRecord {
  id: string
  name: string
  type: string
  host: string
  status: ProbeStatus
  tags: string[]
  capabilities: string[]
  lastHeartbeat: string | null
  createdAt: string
  updatedAt: string
  probes: ProbeRecord[]
}

export interface WorkspaceSettingsUpdateRequest {
  workspaceName: string
  supportEmail: string
  docsUrl: string
  defaultRefreshIntervalSeconds: number
}

export interface NodeRegistrationRequest {
  name: string
  type: string
  host: string
  tags: string[]
  capabilities: string[]
}

export interface NodeRegistrationResponse {
  nodeId: string
  registeredAt: string
}

export interface NodeHeartbeatProbeRequest {
  name: string
  kind: string
  target: string
  status: ProbeStatus
  detail?: string | null
}

export interface NodeHeartbeatRequest {
  status: ProbeStatus
  probes: NodeHeartbeatProbeRequest[]
}

export interface NodeHeartbeatResponse {
  nodeId: string
  acknowledgedAt: string
}
