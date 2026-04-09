import { useOpenClawStore } from '../domains/openclaw/useOpenClawStore'

function displayRunStatus(status: string) {
  switch (status) {
    case 'success':
      return '成功'
    case 'waiting':
      return '等待中'
    case 'failed':
      return '失败'
    default:
      return status
  }
}

export function OpenClawPage() {
  const openClaw = useOpenClawStore()

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>自动化</h2>
        <p>{openClaw.summary}</p>
      </div>

      <div className="toolPageGrid toolPageGrid-wide">
        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>连接状态</h3>
            <span>执行器</span>
          </div>
          <div className="toolDetailStack">
            {openClaw.connection.map((item) => (
              <div className="toolDetailRow" key={item.label}>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
          <div className="toolMetricGrid">
            {openClaw.discovery.map((stat) => (
              <article className="toolMetricCard" key={stat.label}>
                <span>{stat.label}</span>
                <strong>{stat.value}</strong>
                <small>{stat.tone ?? 'neutral'}</small>
              </article>
            ))}
          </div>
        </div>

        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>路由与运行</h3>
            <span>{openClaw.recentActivity.length} 条</span>
          </div>
          <ul className="toolList">
            {openClaw.routing.map((item) => (
              <li key={item.label}>
                <strong>{item.label}</strong>
                <span>{item.value}</span>
              </li>
            ))}
          </ul>
          <div className="toolPanelHeader nestedHeader">
            <h3>最近运行</h3>
            <span>实时</span>
          </div>
          <div className="toolRunList">
            {openClaw.recentActivity.map((run) => (
              <div className="toolRunItem" key={run.label}>
                <div>
                  <strong>{run.label}</strong>
                  <small>{run.tone ?? 'neutral'}</small>
                </div>
                <span className={`inlineStatus inlineStatus-${run.value}`}>{displayRunStatus(run.value)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
