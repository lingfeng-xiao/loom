import type { ContextDomainState, ContextSectionViewModel } from '../domains/workbenchTypes'
import type { LoomBootstrapPayload } from '../types'

function buildSections(payload: LoomBootstrapPayload): ContextSectionViewModel[] {
  return [
    {
      id: 'alignment',
      title: '目标与约束',
      blocks: payload.contextBlocks.filter((block) => block.id === 'context-goal' || block.id === 'context-constraints' || block.id === 'context-summary'),
    },
    {
      id: 'execution',
      title: '进行中的事项',
      blocks: payload.contextBlocks.filter((block) => block.id === 'context-active' || block.id === 'context-open'),
    },
    {
      id: 'references',
      title: '引用输入',
      blocks: payload.contextBlocks.filter((block) => block.id === 'context-files'),
    },
  ].filter((section) => section.blocks.length > 0)
}

export function buildContextState(payload: LoomBootstrapPayload): ContextDomainState {
  return {
    sections: buildSections(payload),
  }
}
