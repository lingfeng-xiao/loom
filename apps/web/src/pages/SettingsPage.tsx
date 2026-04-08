import { useSettingsStore } from '../domains/settings/useSettingsStore'

export function SettingsPage() {
  const settings = useSettingsStore()

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>设置</h2>
        <p>{settings.summary}</p>
      </div>

      <div className="toolPageGrid">
        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>配置范围</h3>
            <span>{settings.tabs.length} 个分区</span>
          </div>
          <div className="scopeChips">
            {settings.tabs.map((scope) => (
              <button className={`docChip ${scope === settings.activeSection ? 'docChip-active' : ''}`} key={scope} onClick={() => settings.setActiveSection(scope)} type="button">
                {scope}
              </button>
            ))}
          </div>

          <div className="toolPanelHeader nestedHeader">
            <h3>当前配置</h3>
            <span>{settings.activeSection}</span>
          </div>
          <div className="toolDetailStack">
            {settings.profile.map((item) => (
              <div className="toolDetailRow" key={item.label}>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>
        </div>

        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>默认策略</h3>
            <span>系统</span>
          </div>
          <ul className="toolList">
            {settings.guidance.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          <div className="toolPanelHeader nestedHeader">
            <h3>保护规则</h3>
            <span>必检</span>
          </div>
          <ul className="toolList toolList-danger">
            {settings.riskNotes.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
