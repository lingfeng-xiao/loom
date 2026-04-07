import { Route, Routes } from 'react-router-dom'
import { WorkspaceShell } from '@/features/workspace/workspace-shell'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<WorkspaceShell />} />
      <Route path="/projects/:projectId/*" element={<WorkspaceShell />} />
    </Routes>
  )
}
