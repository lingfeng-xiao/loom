import { useEffect, useMemo, useState } from 'react'
import type { ApiEnvelope, BootstrapPayload, NodeRecord } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''

async function loadEnvelope<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`)
  if (!response.ok) {
    throw new Error(`Request failed for ${path}: ${response.status}`)
  }
  const payload = (await response.json()) as ApiEnvelope<T>
  return payload.data
}

export default function App() {
  const [bootstrap, setBootstrap] = useState<BootstrapPayload | null>(null)
  const [nodes, setNodes] = useState<NodeRecord[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const refresh = async () => {
      try {
        const [bootstrapPayload, nodePayload] = await Promise.all([
          loadEnvelope<BootstrapPayload>('/api/bootstrap'),
          loadEnvelope<NodeRecord[]>('/api/nodes'),
        ])
        if (!cancelled) {
          setBootstrap(bootstrapPayload)
          setNodes(nodePayload)
          setError(null)
        }
      } catch (refreshError) {
        if (!cancelled) {
          setError(refreshError instanceof Error ? refreshError.message : 'Unknown request failure')
        }
      }
    }

    void refresh()
    const timer = window.setInterval(() => void refresh(), 30000)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [])

  const statusCounts = useMemo(() => {
    return nodes.reduce(
      (counts, node) => {
        counts[node.status] += 1
        return counts
      },
      { up: 0, down: 0, degraded: 0, unknown: 0 },
    )
  }, [nodes])

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Infrastructure Template</p>
          <h1>{bootstrap?.appName ?? 'Template Infrastructure Stack'}</h1>
          <p className="lede">
            {bootstrap?.description ??
              'A neutral monorepo starter with API, web shell, node agent, Docker, and CI/CD ready to customize.'}
          </p>
        </div>
        <div className="heroCard">
          <h2>Release Profile</h2>
          <dl>
            <div>
              <dt>Install root</dt>
              <dd>{bootstrap?.releaseOverview.installRoot ?? '/opt/template'}</dd>
            </div>
            <div>
              <dt>Systemd unit</dt>
              <dd>{bootstrap?.releaseOverview.systemdUnit ?? 'template.service'}</dd>
            </div>
            <div>
              <dt>Registry</dt>
              <dd>{bootstrap?.releaseOverview.registry ?? 'ghcr.io/example'}</dd>
            </div>
          </dl>
        </div>
      </section>

      {error ? <section className="errorBanner">API refresh failed: {error}</section> : null}

      <section className="metrics">
        <article>
          <span>Nodes Up</span>
          <strong>{statusCounts.up}</strong>
        </article>
        <article>
          <span>Degraded</span>
          <strong>{statusCounts.degraded}</strong>
        </article>
        <article>
          <span>Down</span>
          <strong>{statusCounts.down}</strong>
        </article>
        <article>
          <span>Refresh</span>
          <strong>{bootstrap?.workspaceSettings.defaultRefreshIntervalSeconds ?? 30}s</strong>
        </article>
      </section>

      <section className="grid">
        <article className="panel">
          <div className="panelHeader">
            <h2>Setup Checklist</h2>
            <a href={bootstrap?.workspaceSettings.docsUrl ?? '#'} target="_blank" rel="noreferrer">
              Open docs
            </a>
          </div>
          <ul className="stackList">
            {bootstrap?.setupTasks.map((task) => (
              <li key={task.id}>
                <h3>{task.title}</h3>
                <p>{task.description}</p>
              </li>
            )) ?? <li>Waiting for bootstrap payload…</li>}
          </ul>
        </article>

        <article className="panel">
          <div className="panelHeader">
            <h2>Extension Points</h2>
            <span>{bootstrap?.extensionPoints.length ?? 0} ready</span>
          </div>
          <ul className="stackList">
            {bootstrap?.extensionPoints.map((point) => (
              <li key={point.name}>
                <h3>{point.name}</h3>
                <p>{point.description}</p>
                <code>{point.target}</code>
              </li>
            )) ?? <li>Waiting for bootstrap payload…</li>}
          </ul>
        </article>
      </section>

      <section className="panel">
        <div className="panelHeader">
          <h2>Node Inventory</h2>
          <span>{nodes.length} nodes</span>
        </div>
        <div className="nodeTable">
          <div className="nodeTableHead">
            <span>Name</span>
            <span>Status</span>
            <span>Host</span>
            <span>Capabilities</span>
            <span>Last heartbeat</span>
          </div>
          {nodes.length === 0 ? (
            <div className="nodeRow empty">No node registrations yet. Start `apps/node` to populate this view.</div>
          ) : (
            nodes.map((node) => (
              <div className="nodeRow" key={node.id}>
                <span>
                  <strong>{node.name}</strong>
                  <small>{node.type}</small>
                </span>
                <span className={`status status-${node.status}`}>{node.status}</span>
                <span>{node.host}</span>
                <span>{node.capabilities.join(', ')}</span>
                <span>{node.lastHeartbeat ? new Date(node.lastHeartbeat).toLocaleString() : 'Never'}</span>
              </div>
            ))
          )}
        </div>
      </section>
    </main>
  )
}
