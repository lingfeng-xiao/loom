import '@fontsource-variable/geist'
import '@fontsource/jetbrains-mono'
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { AppProviders } from './app/providers'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </React.StrictMode>,
)
