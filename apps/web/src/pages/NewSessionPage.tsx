import { useMemo, useState } from 'react'
import { useProjectStore } from '../domains/project/useProjectStore'

interface NewSessionPageProps {
  onClose: () => void
}

export function NewSessionPage({ onClose }: NewSessionPageProps) {
  const project = useProjectStore()
  const [pendingProjectId, setPendingProjectId] = useState<string | null>(null)
  const [creatingProject, setCreatingProject] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)

  const currentProject = project.availableProjects.find((item) => item.id === project.currentProject.id) ?? {
    id: project.currentProject.id,
    name: project.currentProject.name,
    description: project.currentProject.description,
    conversationCount: 0,
  }

  const otherProjects = useMemo(
    () => project.availableProjects.filter((item) => item.id !== currentProject.id),
    [currentProject.id, project.availableProjects],
  )

  const openSession = async (projectId: string) => {
    setPendingProjectId(projectId)
    setFeedback(null)
    try {
      await project.createConversation(projectId, {})
      onClose()
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '新建会话失败。')
    } finally {
      setPendingProjectId((current) => (current === projectId ? null : current))
    }
  }

  const createProjectAndOpenSession = async () => {
    setCreatingProject(true)
    setFeedback(null)
    try {
      const createdProject = await project.createProject({})
      await project.createConversation(createdProject.id, {})
      onClose()
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : '创建新项目失败。')
    } finally {
      setCreatingProject(false)
    }
  }

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>新建会话</h2>
        <p>先开一个新会话，再决定把它放进哪个项目。这里不做复杂设置，只保留最快的两个入口。</p>
      </div>

      <div className="toolPageGrid">
        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>继续留在当前项目</h3>
            <span>最快路径</span>
          </div>

          <div className="sessionHeroCard">
            <strong>{currentProject.name}</strong>
            <p>{currentProject.description}</p>
            <small>这个项目里已经有 {currentProject.conversationCount} 个会话。</small>
          </div>

          <div className="actionRow">
            <button
              className="actionButton"
              disabled={pendingProjectId === currentProject.id}
              onClick={() => void openSession(currentProject.id)}
              type="button"
            >
              {pendingProjectId === currentProject.id ? '正在打开...' : '新建会话'}
            </button>
            <button
              className="actionButton actionButton-secondary"
              disabled={creatingProject}
              onClick={() => void createProjectAndOpenSession()}
              type="button"
            >
              {creatingProject ? '正在创建...' : '新建项目'}
            </button>
          </div>

          <p className="inlineFeedback">新项目会自动命名，并直接打开一个空白会话。</p>
        </div>

        <div className="toolPanel">
          <div className="toolPanelHeader">
            <h3>切换到其他项目</h3>
            <span>还有 {otherProjects.length} 个项目</span>
          </div>

          <div className="presetList">
            {otherProjects.length > 0 ? (
              otherProjects.map((item) => (
                <button
                  className={`presetCard ${pendingProjectId === item.id ? 'presetCard-active' : ''}`}
                  disabled={pendingProjectId !== null}
                  key={item.id}
                  onClick={() => void openSession(item.id)}
                  type="button"
                >
                  <strong>{item.name}</strong>
                  <span>{item.description}</span>
                  <small>{pendingProjectId === item.id ? '正在打开会话...' : `${item.conversationCount} 个会话`}</small>
                </button>
              ))
            ) : (
              <div className="sessionEmptyState">
                <strong>还没有其他项目</strong>
                <span>点击“新建项目”，Loom 会自动创建一个新项目并打开新会话。</span>
              </div>
            )}
          </div>

          <div className="actionRow">
            <button className="actionButton actionButton-secondary" onClick={onClose} type="button">
              取消
            </button>
          </div>

          {feedback ? <p className="inlineFeedback">{feedback}</p> : null}
        </div>
      </div>
    </section>
  )
}
