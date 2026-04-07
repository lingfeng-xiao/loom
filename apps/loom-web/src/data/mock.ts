import type {
  AssetRecord,
  BootstrapPayload,
  CommandId,
  ConversationSummary,
  MemoryRecord,
  MessageRecord,
  NodeRecord,
  PlanRecord,
  ProjectSummary,
  SkillRecord,
  WorkspaceSettings,
} from '@loom/contracts'
import { DEFAULT_SKILLS, DEFAULT_WORKSPACE_SETTINGS, SAMPLE_PROJECTS } from '@loom/contracts'

const iso = (minutesAgo: number) => new Date(Date.now() - minutesAgo * 60_000).toISOString()

const commands: CommandId[] = [
  '/project-new',
  '/project-switch',
  '/project-status',
  '/plan',
  '/plan-run',
  '/save-card',
  '/memory-show',
  '/memory-save',
  '/skill-list',
  '/node-status',
  '/logs',
]

const projects: ProjectSummary[] = SAMPLE_PROJECTS.map((project, index) => ({
  ...project,
  updatedAt: iso(index * 13),
}))

const conversationsByProject: Record<string, ConversationSummary[]> = {
  'project-knowledge-base': [
    {
      id: 'conv-kb-1',
      projectId: 'project-knowledge-base',
      title: 'Loom Phase 1 交付方案',
      mode: 'chat',
      status: 'active',
      summary: '整理当前 MVP 的边界、工程风险和后续修复顺序。',
      createdAt: iso(220),
      updatedAt: iso(10),
    },
    {
      id: 'conv-kb-2',
      projectId: 'project-knowledge-base',
      title: '项目级工作台重构',
      mode: 'plan',
      status: 'active',
      summary: '把单页式前端重构为项目级会话工作台。',
      createdAt: iso(160),
      updatedAt: iso(22),
    },
    {
      id: 'conv-kb-3',
      projectId: 'project-knowledge-base',
      title: '知识卡片模板整理',
      mode: 'chat',
      status: 'archived',
      summary: '沉淀知识卡片的字段模板和写入规则。',
      createdAt: iso(480),
      updatedAt: iso(300),
    },
  ],
  'project-ops-console': [
    {
      id: 'conv-ops-1',
      projectId: 'project-ops-console',
      title: 'JD 部署验收',
      mode: 'plan',
      status: 'active',
      summary: '确认中心节点部署和冒烟测试项目。',
      createdAt: iso(140),
      updatedAt: iso(12),
    },
    {
      id: 'conv-ops-2',
      projectId: 'project-ops-console',
      title: '节点状态巡检',
      mode: 'chat',
      status: 'active',
      summary: '检查心跳、服务状态和快照质量。',
      createdAt: iso(90),
      updatedAt: iso(25),
    },
  ],
  'project-english-lab': [],
}

const messagesByConversation: Record<string, MessageRecord[]> = {
  'conv-kb-1': [
    {
      id: 'msg-kb-1',
      conversationId: 'conv-kb-1',
      projectId: 'project-knowledge-base',
      role: 'user',
      content: '把这次 Loom Phase 1 的实现、问题和验收都整理清楚。',
      createdAt: iso(220),
    },
    {
      id: 'msg-kb-2',
      conversationId: 'conv-kb-1',
      projectId: 'project-knowledge-base',
      role: 'assistant',
      content: '已经完成基础交付，接下来重点是前端工作台重构和工程化补齐。',
      createdAt: iso(210),
    },
    {
      id: 'msg-kb-3',
      conversationId: 'conv-kb-1',
      projectId: 'project-knowledge-base',
      role: 'user',
      content: '前端需要更像成熟的项目级产品，不要再像单页 demo。',
      createdAt: iso(200),
    },
  ],
  'conv-kb-2': [
    {
      id: 'msg-kb-4',
      conversationId: 'conv-kb-2',
      projectId: 'project-knowledge-base',
      role: 'user',
      content: '重构成项目级会话工作台，强调项目、会话、多轮切换和设置中心。',
      createdAt: iso(160),
    },
    {
      id: 'msg-kb-5',
      conversationId: 'conv-kb-2',
      projectId: 'project-knowledge-base',
      role: 'assistant',
      content: '计划已经整理完成，下一步是补齐 bootstrap、settings、conversation patch 和新的前端壳层。',
      createdAt: iso(150),
    },
  ],
  'conv-kb-3': [
    {
      id: 'msg-kb-6',
      conversationId: 'conv-kb-3',
      projectId: 'project-knowledge-base',
      role: 'assistant',
      content: '知识卡片模板已确认“来源、结论、下一步动作”三段式结构。',
      createdAt: iso(470),
    },
  ],
  'conv-ops-1': [
    {
      id: 'msg-ops-1',
      conversationId: 'conv-ops-1',
      projectId: 'project-ops-console',
      role: 'user',
      content: '帮我确认 jd 服务器上服务、节点和 Web 入口是否都能验收。',
      createdAt: iso(140),
    },
    {
      id: 'msg-ops-2',
      conversationId: 'conv-ops-1',
      projectId: 'project-ops-console',
      role: 'assistant',
      content: '已完成发布，Web 对外走 3000，server 内部使用 18080，节点正在持续回报心跳。',
      createdAt: iso(132),
    },
  ],
  'conv-ops-2': [
    {
      id: 'msg-ops-3',
      conversationId: 'conv-ops-2',
      projectId: 'project-ops-console',
      role: 'assistant',
      content: '当前 Node 快照仍是最小实现，CPU、内存和服务状态的精度后续还要补。',
      createdAt: iso(88),
    },
  ],
}

const memoriesByProject: Record<string, MemoryRecord[]> = {
  'project-knowledge-base': [
    {
      id: 'mem-global-1',
      scope: 'global',
      projectId: null,
      title: '输出风格',
      content: '优先给出结构清晰、能直接执行的答案，少做泛泛而谈的总结。',
      priority: 95,
      status: 'active',
      sourceType: 'user-preference',
      sourceRef: null,
      createdAt: iso(600),
      updatedAt: iso(18),
    },
    {
      id: 'mem-kb-1',
      scope: 'project',
      projectId: 'project-knowledge-base',
      title: '前端重构方向',
      content: '工作台必须像成熟项目管理产品，重点突出项目级会话管理和多轮切换。',
      priority: 90,
      status: 'active',
      sourceType: 'conversation',
      sourceRef: 'conv-kb-2',
      createdAt: iso(155),
      updatedAt: iso(20),
    },
  ],
  'project-ops-console': [
    {
      id: 'mem-global-1-copy',
      scope: 'global',
      projectId: null,
      title: '输出风格',
      content: '优先给出结构清晰、能直接执行的答案，少做泛泛而谈的总结。',
      priority: 95,
      status: 'active',
      sourceType: 'user-preference',
      sourceRef: null,
      createdAt: iso(600),
      updatedAt: iso(18),
    },
    {
      id: 'mem-ops-1',
      scope: 'project',
      projectId: 'project-ops-console',
      title: '节点安全边界',
      content: 'Phase 1 只允许只读节点状态，不开放任意 shell 和自由命令执行。',
      priority: 98,
      status: 'active',
      sourceType: 'design-doc',
      sourceRef: 'loom_formal_plan',
      createdAt: iso(240),
      updatedAt: iso(12),
    },
  ],
  'project-english-lab': [
    {
      id: 'mem-global-1-learning',
      scope: 'global',
      projectId: null,
      title: '输出风格',
      content: '优先给出结构清晰、能直接执行的答案，少做泛泛而谈的总结。',
      priority: 95,
      status: 'active',
      sourceType: 'user-preference',
      sourceRef: null,
      createdAt: iso(600),
      updatedAt: iso(18),
    },
  ],
}

const plansByProject: Record<string, PlanRecord[]> = {
  'project-knowledge-base': [
    {
      id: 'plan-kb-1',
      projectId: 'project-knowledge-base',
      conversationId: 'conv-kb-2',
      goal: '把 Loom Web 重构成专业的项目级会话工作台。',
      constraints: ['项目优先', '真实设置中心', '保持中文优先', '允许必要接口扩展'],
      status: 'running',
      approvalRequired: true,
      steps: [
        {
          id: 'plan-kb-step-1',
          title: '补齐 contracts 与后端接口',
          description: '新增 bootstrap、workspace settings、conversation patch 和过滤能力。',
          status: 'completed',
          result: '后端 API 骨架已补齐。',
          sortOrder: 1,
        },
        {
          id: 'plan-kb-step-2',
          title: '重建前端壳层与路由',
          description: '引入项目级导航、会话管理器、主画布和右侧 Inspector。',
          status: 'running',
          result: '',
          sortOrder: 2,
        },
        {
          id: 'plan-kb-step-3',
          title: '联调与回归',
          description: '完成构建、接口验证和远端重新部署。',
          status: 'pending',
          result: '',
          sortOrder: 3,
        },
      ],
      executionResult: null,
      createdAt: iso(160),
      updatedAt: iso(14),
    },
  ],
  'project-ops-console': [
    {
      id: 'plan-ops-1',
      projectId: 'project-ops-console',
      conversationId: 'conv-ops-1',
      goal: '完成 jd 环境的部署验收并沉淀问题台账。',
      constraints: ['保留现有服务', '不破坏 8080 占用者', '记录异常并可回归验证'],
      status: 'approved',
      approvalRequired: true,
      steps: [
        {
          id: 'plan-ops-step-1',
          title: '检查服务入口',
          description: '确认 Web、Health、Nodes 接口都能访问。',
          status: 'completed',
          result: '3000 对外入口已验证。',
          sortOrder: 1,
        },
        {
          id: 'plan-ops-step-2',
          title: '整理问题台账',
          description: '把非正常项、风险和建议动作维护进文档。',
          status: 'completed',
          result: '已建立已知问题清单。',
          sortOrder: 2,
        },
      ],
      executionResult: {
        summary: '部署已可验收，但仍存在内存存储、Python 代理和 Node 指标精度不足等工程问题。',
        outputAssetIds: ['asset-ops-1'],
        logs: ['health 已通过', 'nodes 接口已返回', '问题台账已更新'],
      },
      createdAt: iso(140),
      updatedAt: iso(12),
    },
  ],
  'project-english-lab': [],
}

const assetsByProject: Record<string, AssetRecord[]> = {
  'project-knowledge-base': [
    {
      id: 'asset-kb-1',
      projectId: 'project-knowledge-base',
      type: 'knowledge_card',
      title: '项目级工作台重构原则',
      contentRef: 'markdown://vault/knowledge/workspace-redesign-principles.md',
      storagePath: '/vault/knowledge/knowledge-base/knowledge_card/2026/04/workspace-redesign-principles.md',
      sourceConversationId: 'conv-kb-2',
      sourcePlanId: 'plan-kb-1',
      sourceNodeId: null,
      tags: ['frontend', 'workspace', 'architecture'],
      createdAt: iso(30),
    },
  ],
  'project-ops-console': [
    {
      id: 'asset-ops-1',
      projectId: 'project-ops-console',
      type: 'ops_note',
      title: 'jd 部署验收记录',
      contentRef: 'markdown://vault/ops/jd-runtime-acceptance.md',
      storagePath: '/vault/ops/ops-console/ops_note/2026/04/jd-runtime-acceptance.md',
      sourceConversationId: 'conv-ops-1',
      sourcePlanId: 'plan-ops-1',
      sourceNodeId: 'node-jd',
      tags: ['deploy', 'jd', 'runtime'],
      createdAt: iso(26),
    },
  ],
  'project-english-lab': [],
}

const nodes: NodeRecord[] = [
  {
    id: 'node-local',
    name: '本地工作站',
    type: 'local_pc',
    host: 'localhost',
    tags: ['desktop', 'development'],
    status: 'online',
    lastHeartbeat: iso(3),
    snapshot: {
      hostname: 'loom-local',
      cpuUsage: 0.22,
      memoryUsage: 0.58,
      diskUsage: 0.41,
      services: [
        { name: 'loom-web', status: 'up' },
        { name: 'loom-server', status: 'up' },
      ],
      recordedAt: iso(3),
    },
    capabilities: ['browser', 'editor', 'vscode'],
  },
  {
    id: 'node-jd',
    name: 'JD 中心节点',
    type: 'server',
    host: '114.67.156.250:2222',
    tags: ['center', 'production'],
    status: 'online',
    lastHeartbeat: iso(1),
    snapshot: {
      hostname: 'jd-center',
      cpuUsage: 0.37,
      memoryUsage: 0.64,
      diskUsage: 0.52,
      services: [
        { name: 'loom-server', status: 'unknown' },
        { name: 'loom-web', status: 'unknown' },
      ],
      recordedAt: iso(1),
    },
    capabilities: ['heartbeat', 'snapshot', 'vault-write'],
  },
]

const bootstrap: BootstrapPayload = {
  projects,
  commands,
  skills: DEFAULT_SKILLS as SkillRecord[],
  nodes,
  defaultProjectId: 'project-knowledge-base',
  workspaceSettings: {
    ...DEFAULT_WORKSPACE_SETTINGS,
    defaultProjectId: 'project-knowledge-base',
    updatedAt: iso(5),
  } as WorkspaceSettings,
}

export function createMockWorkspace() {
  return {
    bootstrap,
    conversationsByProject,
    messagesByConversation,
    memoriesByProject,
    plansByProject,
    assetsByProject,
  }
}
