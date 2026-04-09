import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/loomDesignTokens.css'
import './index.css'

interface RuntimeBoundaryState {
  error: Error | null
}

class RuntimeBoundary extends React.Component<React.PropsWithChildren, RuntimeBoundaryState> {
  state: RuntimeBoundaryState = {
    error: null,
  }

  static getDerivedStateFromError(error: Error): RuntimeBoundaryState {
    return { error }
  }

  componentDidCatch(error: Error) {
    console.error('RuntimeBoundary caught an error:', error)
  }

  render() {
    if (this.state.error) {
      return (
        <div
          style={{
            minHeight: '100dvh',
            padding: '24px',
            background: 'var(--bg-shell)',
            color: 'var(--text-primary)',
            fontFamily: '"Segoe UI", "PingFang SC", sans-serif',
          }}
        >
          <h1 style={{ marginTop: 0, fontSize: '20px' }}>前端运行时错误</h1>
          <p style={{ color: 'var(--text-secondary)' }}>应用在渲染时崩溃，下面展示首次捕获到的错误。</p>
          <pre
            style={{
              overflow: 'auto',
              padding: '16px',
              borderRadius: '12px',
              background: 'var(--bg-code)',
              border: '1px solid var(--line-default)',
              whiteSpace: 'pre-wrap',
            }}
          >
            {this.state.error.stack ?? this.state.error.message}
          </pre>
        </div>
      )
    }

    return this.props.children
  }
}

function renderFatalError(title: string, detail: string) {
  const root = document.getElementById('root')
  if (!root) {
    return
  }

  root.innerHTML = `
    <div style="min-height:100dvh;padding:24px;background:var(--bg-shell);color:var(--text-primary);font-family:'Segoe UI','PingFang SC',sans-serif;">
      <h1 style="margin-top:0;font-size:20px;">${title}</h1>
      <p style="color:var(--text-secondary);">React 恢复前出现了致命前端错误。</p>
      <pre style="overflow:auto;padding:16px;border-radius:12px;background:var(--bg-code);border:1px solid var(--line-default);white-space:pre-wrap;">${detail}</pre>
    </div>
  `
}

window.addEventListener('error', (event) => {
  const detail = event.error instanceof Error ? event.error.stack ?? event.error.message : String(event.message)
  console.error('Global runtime error:', event.error ?? event.message)
  renderFatalError('前端运行时错误', detail)
})

window.addEventListener('unhandledrejection', (event) => {
  const reason = event.reason instanceof Error ? event.reason.stack ?? event.reason.message : String(event.reason)
  console.error('Unhandled promise rejection:', event.reason)
  renderFatalError('未处理的 Promise 拒绝', reason)
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RuntimeBoundary>
      <App />
    </RuntimeBoundary>
  </React.StrictMode>,
)
