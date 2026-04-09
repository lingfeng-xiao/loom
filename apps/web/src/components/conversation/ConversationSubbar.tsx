import type { ConversationMode, ProjectListItem } from '../../types'
import { WorkbenchIcon } from '../WorkbenchIcon'

interface ConversationSubbarProps {
  title: string
  activeMode: ConversationMode
  modes: Array<{ id: ConversationMode; label: string }>
  meta: string
  activeProjectId: string
  projects: ProjectListItem[]
  movingProject: boolean
  onModeChange: (mode: ConversationMode) => void
  onProjectChange: (projectId: string) => void
  onCreateProject: () => void
}

export function ConversationSubbar({
  title,
  activeMode,
  modes,
  meta,
  activeProjectId,
  projects,
  movingProject,
  onModeChange,
  onProjectChange,
  onCreateProject,
}: ConversationSubbarProps) {
  return (
    <div className="conversationSubbar">
      <div className="conversationSubbarTopRow">
        <div className="conversationTitleBlock">
          <h2>{title}</h2>
          <p>{meta}</p>
        </div>

        <div className="conversationToolbar">
          <label className="uiField conversationSelectField" title="会话模式">
            <WorkbenchIcon name="switch" size={15} />
            <select onChange={(event) => onModeChange(event.target.value as ConversationMode)} value={activeMode}>
              {modes.map((mode) => (
                <option key={mode.id} value={mode.id}>
                  {mode.label}
                </option>
              ))}
            </select>
          </label>

          <label className="uiField conversationSelectField conversationSelectField-project" title="所属项目">
            <WorkbenchIcon name="folderOpen" size={15} />
            <select disabled={movingProject} onChange={(event) => onProjectChange(event.target.value)} value={activeProjectId}>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>

          <button
            aria-label={movingProject ? '正在创建项目' : '新建项目'}
            className="uiButton uiButton-secondary uiButton-icon conversationIconAction"
            disabled={movingProject}
            onClick={onCreateProject}
            title="新建项目"
            type="button"
          >
            <WorkbenchIcon name="plus" size={15} />
          </button>
        </div>
      </div>
    </div>
  )
}
