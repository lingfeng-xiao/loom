interface PlaceholderPageProps {
  title: string
  description: string
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <div className="emptyPage">
        <p className="eyebrow">{title}</p>
        <h3>{description}</h3>
        <p>该区域已纳入新的工作台结构，下一阶段会接入真实领域数据。</p>
      </div>
    </section>
  )
}
