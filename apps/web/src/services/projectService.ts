import type { LoomBootstrapPayload } from '../types'
import type { ProjectDomainState } from '../domains/workbenchTypes'
import type { WorkspaceHeaderAction } from '../components/shell/shellTypes'

export function buildProjectState(
  payload: LoomBootstrapPayload,
  workspaceTitle: string,
  workspaceMeta: string,
  headerActions: WorkspaceHeaderAction[],
): ProjectDomainState {
  return {
    currentProject: payload.project,
    workspaceTitle,
    workspaceMeta,
    environmentStatus: `OpenClaw ${payload.project.openClawStatus}`,
    headerActions,
  }
}
