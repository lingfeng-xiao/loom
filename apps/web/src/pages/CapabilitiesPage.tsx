import { useCapabilitiesStore } from '../domains/capabilities/useCapabilitiesStore'

export function CapabilitiesPage() {
  const capabilities = useCapabilitiesStore()

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>能力与应用</h2>
        <p>{capabilities.summary}</p>
      </div>

      <div className="toolPageGrid">
        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>能力面板</h3>
            <span>{capabilities.cards.length} 个区域</span>
          </div>
          <div className="toolMetricGrid">
            {capabilities.cards.map((card) => (
              <article className="toolMetricCard" key={card.id}>
                <span>{card.title}</span>
                <strong>{card.items.length}</strong>
                <small>{card.items.join(' / ')}</small>
              </article>
            ))}
          </div>
        </div>

        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>当前绑定</h3>
            <span>项目级</span>
          </div>
          <ul className="toolList">
            {capabilities.bindingRules.map((item) => (
              <li key={item.label}>
                <strong>{item.label}</strong>
                <span>{item.value}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
