import { useEffect, useMemo, useState } from 'react'
import { useProjectStore } from '../domains/project/useProjectStore'
import { createLoomSdk } from '../sdk/loomApiClient'
import type { FileAssetSummary } from '../types'

const API_BASE = import.meta.env.VITE_API_BASE ?? ''
const sdk = createLoomSdk({ baseUrl: API_BASE })

const fallbackFiles: FileAssetSummary[] = [
  {
    id: 'fallback-prd',
    projectId: 'project-loom',
    displayName: 'loom-prd.md',
    mimeType: 'text/markdown',
    sizeBytes: 18_240,
    parseStatus: 'ready',
    uploadedAt: 'fallback',
  },
  {
    id: 'fallback-contract',
    projectId: 'project-loom',
    displayName: 'phase1-contract-freeze.md',
    mimeType: 'text/markdown',
    sizeBytes: 24_512,
    parseStatus: 'ready',
    uploadedAt: 'fallback',
  },
]

function formatBytes(sizeBytes: number) {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`
  }
  return `${(sizeBytes / 1024).toFixed(1)} KB`
}

export function FilesPage() {
  const project = useProjectStore()
  const projectId = project.currentProject.id
  const [files, setFiles] = useState<FileAssetSummary[]>(fallbackFiles)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const sourceLabel = useMemo(() => (error ? '本地回退' : '远端数据'), [error])

  useEffect(() => {
    if (!projectId) {
      setFiles([])
      setLoading(false)
      return
    }

    const controller = new AbortController()
    setLoading(true)
    setError(null)

    sdk.workspace
      .getFiles(projectId, controller.signal)
      .then((response) => {
        setFiles(response.items)
        setLoading(false)
      })
      .catch((fetchError) => {
        setFiles(fallbackFiles)
        setError(fetchError instanceof Error ? fetchError.message : '文件数据读取失败')
        setLoading(false)
      })

    return () => controller.abort()
  }, [projectId])

  return (
    <section className="toolSurface">
      <div className="toolPageHeader">
        <h2>文件</h2>
        <p>项目文件池与引用资产，优先展示可供会话与上下文引用的核心文件。</p>
      </div>

      {error ? <section className="infoBanner">文件远程读取失败，已回退到 {sourceLabel}：{error}</section> : null}

      {loading ? (
        <div className="emptyPage">
          <p className="eyebrow">文件</p>
          <h3>正在加载项目文件池</h3>
          <p>优先读取工作区 API，失败时回退到本地基线。</p>
        </div>
      ) : files.length === 0 ? (
        <div className="emptyPage">
          <p className="eyebrow">文件</p>
          <h3>当前项目还没有文件资产</h3>
          <p>下一步可以接入上传、解析和引用链路。</p>
        </div>
      ) : (
        <div className="toolPageGrid toolPageGrid-wide">
          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>文件列表</h3>
              <span>
                {files.length} 个文件 / {sourceLabel}
              </span>
            </div>
            <ul className="toolList">
              {files.map((file) => (
                <li key={file.id}>
                  <strong>{file.displayName}</strong>
                  <span>{file.mimeType}</span>
                  <small>{formatBytes(file.sizeBytes)}</small>
                </li>
              ))}
            </ul>
          </div>

          <div className="toolPanel">
            <div className="toolPanelHeader">
              <h3>解析状态</h3>
              <span>按文件查看</span>
            </div>
            <div className="toolDetailStack">
              {files.map((file) => (
                <div className="toolDetailRow" key={`${file.id}-status`}>
                  <span>{file.displayName}</span>
                  <strong>{file.parseStatus}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
