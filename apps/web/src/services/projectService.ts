import type { LoomBootstrapPayload, ProjectListItem } from '../types'
import type { ProjectDomainState } from '../domains/workbenchTypes'
import type { WorkspaceHeaderAction } from '../components/shell/shellTypes'

export function buildProjectState(
  payload: LoomBootstrapPayload,
  availableProjects: ProjectListItem[],
  workspaceTitle: string,
  workspaceMeta: string,
  headerActions: WorkspaceHeaderAction[],
): ProjectDomainState {
  return {
    currentProject: payload.project,
    availableProjects,
    workspaceTitle,
    workspaceMeta,
    environmentStatus: `OpenClaw ${payload.project.openClawStatus}`,
    headerActions,
  }
}
