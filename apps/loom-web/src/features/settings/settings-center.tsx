import { useEffect, useMemo, useState } from 'react'
import type { CommandId, NodeRecord, ProjectSummary, SkillRecord, WorkspaceSettings } from '@loom/contracts'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Panel } from '@/features/workspace/presentation'

export function SettingsCenter({
  project,
  settings,
  skills,
  nodes,
  commandCatalog,
  onSaveWorkspace,
  onSaveProjectDefaults,
}: {
  project: ProjectSummary
  settings: WorkspaceSettings
  skills: SkillRecord[]
  nodes: NodeRecord[]
  commandCatalog: CommandId[]
  onSaveWorkspace: (draft: WorkspaceSettings) => Promise<void>
  onSaveProjectDefaults: (draft: ProjectSummary) => Promise<void>
}) {
  const [workspaceDraft, setWorkspaceDraft] = useState(settings)
  const [projectDraft, setProjectDraft] = useState(project)
  const [savingWorkspace, setSavingWorkspace] = useState(false)
  const [savingProject, setSavingProject] = useState(false)

  useEffect(() => {
    setWorkspaceDraft(settings)
  }, [settings])

  useEffect(() => {
    setProjectDraft(project)
  }, [project])

  const activeNodeLabels = useMemo(() => new Set(projectDraft.boundNodeIds), [projectDraft.boundNodeIds])
  const activeCommandLabels = useMemo(() => new Set(workspaceDraft.enabledCommands), [workspaceDraft.enabledCommands])
  const activeSkillLabels = useMemo(() => new Set(workspaceDraft.enabledSkills), [workspaceDraft.enabledSkills])

  async function saveWorkspace() {
    setSavingWorkspace(true)
    try {
      await onSaveWorkspace(workspaceDraft)
    } finally {
      setSavingWorkspace(false)
    }
  }

  async function saveProject() {
    setSavingProject(true)
    try {
      await onSaveProjectDefaults(projectDraft)
    } finally {
      setSavingProject(false)
    }
  }

  return (
    <div className="grid gap-6 2xl:grid-cols-[220px_minmax(0,1fr)]">
      <aside className="rounded-3xl border border-slate-200 bg-white/94 px-4 py-5 shadow-[0_12px_40px_rgba(15,23,42,0.06)]">
        <div className="space-y-2 text-sm">
          {['工作区', '项目默认项', '模型与 Provider', 'Vault 与资产规则', 'Nodes 与心跳策略', 'Commands / Skills', '诊断与关于'].map((item) => (
            <div key={item} className="rounded-2xl px-3 py-2 font-medium text-slate-700">
              {item}
            </div>
          ))}
        </div>
      </aside>

      <div className="space-y-6">
        <Panel
          eyebrow="Workspace"
          title="工作区"
          description="控制工作区名称、语言、默认落点和右侧 Inspector 的默认行为。"
          actions={<Button onClick={() => void saveWorkspace()} disabled={savingWorkspace}>{savingWorkspace ? '保存中...' : '保存工作区'}</Button>}
        >
          <div className="grid gap-4 xl:grid-cols-2">
            <Field label="工作区名称">
              <Input value={workspaceDraft.workspaceName} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, workspaceName: event.target.value })} />
            </Field>
            <Field label="默认项目 ID">
              <Input value={workspaceDraft.defaultProjectId ?? ''} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, defaultProjectId: event.target.value || null })} />
            </Field>
            <Field label="语言">
              <select className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm" value={workspaceDraft.language} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, language: event.target.value as WorkspaceSettings['language'] })}>
                <option value="zh-CN">中文</option>
                <option value="en-US">English</option>
              </select>
            </Field>
            <Field label="默认落点">
              <select className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm" value={workspaceDraft.defaultLandingView} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, defaultLandingView: event.target.value as WorkspaceSettings['defaultLandingView'] })}>
                <option value="last_conversation">上次激活会话</option>
                <option value="project_home">项目首页</option>
              </select>
            </Field>
          </div>
        </Panel>

        <Panel
          eyebrow="Project Defaults"
          title="项目默认项"
          description="编辑当前项目的描述、默认技能、默认命令、知识根路径和绑定节点。"
          actions={<Button variant="outline" onClick={() => void saveProject()} disabled={savingProject}>{savingProject ? '保存中...' : '保存项目设置'}</Button>}
        >
          <div className="grid gap-4 xl:grid-cols-2">
            <Field label="项目名称">
              <Input value={projectDraft.name} onChange={(event) => setProjectDraft({ ...projectDraft, name: event.target.value })} />
            </Field>
            <Field label="项目类型">
              <select className="h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm" value={projectDraft.type} onChange={(event) => setProjectDraft({ ...projectDraft, type: event.target.value as ProjectSummary['type'] })}>
                <option value="knowledge">知识</option>
                <option value="ops">运维</option>
                <option value="learning">学习</option>
              </select>
            </Field>
            <Field label="项目描述" className="xl:col-span-2">
              <textarea className="min-h-[112px] rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 outline-none" value={projectDraft.description} onChange={(event) => setProjectDraft({ ...projectDraft, description: event.target.value })} />
            </Field>
            <Field label="知识根路径" className="xl:col-span-2">
              <Input value={projectDraft.knowledgeRoots.join(', ')} onChange={(event) => setProjectDraft({ ...projectDraft, knowledgeRoots: event.target.value.split(',').map((item) => item.trim()).filter(Boolean) })} />
            </Field>
          </div>

          <div className="mt-6 grid gap-4 xl:grid-cols-2">
            <ToggleGroup
              title="绑定节点"
              items={nodes.map((node) => ({ id: node.id, label: `${node.name} · ${node.host}` }))}
              activeIds={activeNodeLabels}
              onToggle={(id) => setProjectDraft((current) => ({ ...current, boundNodeIds: current.boundNodeIds.includes(id) ? current.boundNodeIds.filter((item) => item !== id) : [...current.boundNodeIds, id] }))}
            />

            <ToggleGroup
              title="默认技能"
              items={skills.map((skill) => ({ id: skill.id, label: skill.name }))}
              activeIds={new Set(projectDraft.defaultSkills)}
              onToggle={(id) => setProjectDraft((current) => ({ ...current, defaultSkills: current.defaultSkills.includes(id as ProjectSummary['defaultSkills'][number]) ? current.defaultSkills.filter((item) => item !== id) : [...current.defaultSkills, id as ProjectSummary['defaultSkills'][number]] }))}
            />
          </div>
        </Panel>

        <Panel eyebrow="Model & Provider" title="模型与 Provider" description="配置默认 Provider、Base URL、模型名与温度。">
          <div className="grid gap-4 xl:grid-cols-2">
            <Field label="Provider">
              <Input value={workspaceDraft.model.providerLabel} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, model: { ...workspaceDraft.model, providerLabel: event.target.value } })} />
            </Field>
            <Field label="模型名">
              <Input value={workspaceDraft.model.model} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, model: { ...workspaceDraft.model, model: event.target.value } })} />
            </Field>
            <Field label="Base URL" className="xl:col-span-2">
              <Input value={workspaceDraft.model.baseUrl} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, model: { ...workspaceDraft.model, baseUrl: event.target.value } })} />
            </Field>
            <Field label="Temperature">
              <Input type="number" step="0.1" value={workspaceDraft.model.temperature} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, model: { ...workspaceDraft.model, temperature: Number(event.target.value) } })} />
            </Field>
          </div>
        </Panel>

        <Panel eyebrow="Vault" title="Vault 与资产规则" description="定义服务器 Vault、本地 Vault 和资产路径模板。">
          <div className="grid gap-4 xl:grid-cols-2">
            <Field label="服务器 Vault 根路径">
              <Input value={workspaceDraft.vault.serverVaultRoot} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, vault: { ...workspaceDraft.vault, serverVaultRoot: event.target.value } })} />
            </Field>
            <Field label="本地 Vault 根路径">
              <Input value={workspaceDraft.vault.localVaultRoot} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, vault: { ...workspaceDraft.vault, localVaultRoot: event.target.value } })} />
            </Field>
            <Field label="资产路径模板" className="xl:col-span-2">
              <Input value={workspaceDraft.vault.assetPathTemplate} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, vault: { ...workspaceDraft.vault, assetPathTemplate: event.target.value } })} />
            </Field>
          </div>
        </Panel>

        <Panel eyebrow="Nodes" title="Nodes 与心跳策略" description="设置心跳超时、中心节点标签以及 Inspector 的离线节点显示策略。">
          <div className="grid gap-4 xl:grid-cols-3">
            <Field label="心跳超时（秒）">
              <Input type="number" value={workspaceDraft.nodes.heartbeatTimeoutSeconds} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, nodes: { ...workspaceDraft.nodes, heartbeatTimeoutSeconds: Number(event.target.value) } })} />
            </Field>
            <Field label="中心节点标签">
              <Input value={workspaceDraft.nodes.centerNodeLabel} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, nodes: { ...workspaceDraft.nodes, centerNodeLabel: event.target.value } })} />
            </Field>
            <Field label="显示离线节点">
              <div className="flex h-11 items-center rounded-xl border border-slate-200 bg-white px-3">
                <input checked={workspaceDraft.nodes.inspectorShowOffline} onChange={(event) => setWorkspaceDraft({ ...workspaceDraft, nodes: { ...workspaceDraft.nodes, inspectorShowOffline: event.target.checked } })} type="checkbox" className="h-4 w-4" />
              </div>
            </Field>
          </div>
        </Panel>

        <Panel eyebrow="Commands / Skills" title="命令与技能开关" description="控制全局命令目录与技能中心中哪些入口默认可用。">
          <div className="grid gap-4 xl:grid-cols-2">
            <ToggleGroup
              title="启用命令"
              items={commandCatalog.map((command) => ({ id: command, label: command }))}
              activeIds={activeCommandLabels}
              onToggle={(id) => setWorkspaceDraft((current) => ({ ...current, enabledCommands: current.enabledCommands.includes(id as CommandId) ? current.enabledCommands.filter((item) => item !== id) : [...current.enabledCommands, id as CommandId] }))}
            />

            <ToggleGroup
              title="启用技能"
              items={skills.map((skill) => ({ id: skill.id, label: `${skill.name} · ${skill.version}` }))}
              activeIds={activeSkillLabels}
              onToggle={(id) => setWorkspaceDraft((current) => ({ ...current, enabledSkills: current.enabledSkills.includes(id as WorkspaceSettings['enabledSkills'][number]) ? current.enabledSkills.filter((item) => item !== id) : [...current.enabledSkills, id as WorkspaceSettings['enabledSkills'][number]] }))}
            />
          </div>
        </Panel>

        <Panel eyebrow="Diagnostics" title="诊断与关于" description="展示当前界面基线与运行信息，方便后续工程化排查。">
          <div className="grid gap-3 text-sm text-slate-600 xl:grid-cols-2">
            <DiagnosticRow label="主题基线" value="Light-first / 冷灰石墨 / 蓝绿点缀" />
            <DiagnosticRow label="字体" value={`${workspaceDraft.appearance.fontSans} / ${workspaceDraft.appearance.fontMono}`} />
            <DiagnosticRow label="默认密度" value={workspaceDraft.density} />
            <DiagnosticRow label="Inspector 默认状态" value={workspaceDraft.inspectorDefaultOpen ? '打开' : '关闭'} />
            <DiagnosticRow label="当前项目" value={project.name} />
            <DiagnosticRow label="更新时间" value={settings.updatedAt} />
          </div>
        </Panel>
      </div>
    </div>
  )
}

function Field({ label, className, children }: { label: string; className?: string; children: React.ReactNode }) {
  return (
    <div className={className}>
      <div className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
      {children}
    </div>
  )
}

function ToggleGroup({
  title,
  items,
  activeIds,
  onToggle,
}: {
  title: string
  items: Array<{ id: string; label: string }>
  activeIds: Set<string>
  onToggle: (id: string) => void
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4">
      <div className="text-sm font-semibold text-slate-950">{title}</div>
      <div className="mt-4 space-y-2">
        {items.map((item) => (
          <label key={item.id} className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm text-slate-700">
            <input checked={activeIds.has(item.id)} onChange={() => onToggle(item.id)} type="checkbox" className="h-4 w-4" />
            <span>{item.label}</span>
          </label>
        ))}
      </div>
    </section>
  )
}

function DiagnosticRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-2 text-sm text-slate-900">{value}</div>
    </div>
  )
}
