import type { ContextSectionViewModel } from '../../domains/workbenchTypes'

interface ContextRailProps {
  sections: ContextSectionViewModel[]
}

export function ContextRail({ sections }: ContextRailProps) {
  return (
    <div className="railBody">
      {sections.map((section) => (
        <section className="railSection" key={section.id}>
          <div className="railSectionTitle">{section.title}</div>
          <div className="contextSectionList">
            {section.blocks.map((block) => (
              <div className="contextItem" key={block.id}>
                <strong>{block.label}</strong>
                <p>{block.value}</p>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
