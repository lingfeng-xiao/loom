export type BootstrapSourceMode = 'auto' | 'remote' | 'fallback'
export type BootstrapSourceResolution = 'remote' | 'fallback'

export interface BootstrapSourceViewModel {
  mode: BootstrapSourceMode
  resolution: BootstrapSourceResolution
  label: string
  detail: string
}

export const BOOTSTRAP_SOURCE_STORAGE_KEY = 'loom.bootstrapSourceMode'

const BOOTSTRAP_SOURCE_MODES: BootstrapSourceMode[] = ['auto', 'remote', 'fallback']

export function normalizeBootstrapSourceMode(value: string | null | undefined): BootstrapSourceMode {
  return BOOTSTRAP_SOURCE_MODES.includes(value as BootstrapSourceMode) ? (value as BootstrapSourceMode) : 'auto'
}

export function cycleBootstrapSourceMode(mode: BootstrapSourceMode): BootstrapSourceMode {
  const currentIndex = BOOTSTRAP_SOURCE_MODES.indexOf(mode)
  return BOOTSTRAP_SOURCE_MODES[(currentIndex + 1) % BOOTSTRAP_SOURCE_MODES.length] ?? 'auto'
}

export function buildBootstrapSourceViewModel(
  mode: BootstrapSourceMode,
  resolution: BootstrapSourceResolution,
  loading: boolean,
  error: string | null,
): BootstrapSourceViewModel {
  if (loading) {
    return {
      mode,
      resolution,
      label: '加载中',
      detail: mode === 'fallback' ? '正在读取本地快照' : '正在读取远端快照',
    }
  }

  if (error && resolution === 'fallback' && mode !== 'fallback') {
    return {
      mode,
      resolution,
      label: mode === 'remote' ? '远端失败' : '自动降级',
      detail: error,
    }
  }

  if (mode === 'fallback') {
    return {
      mode,
      resolution,
      label: '本地快照',
      detail: '仅使用本地快照',
    }
  }

  if (mode === 'remote') {
    return {
      mode,
      resolution,
      label: resolution === 'remote' ? '远端数据' : '远端降级',
      detail: resolution === 'remote' ? '直接读取后端工作区快照' : error ?? '远端数据不可用',
    }
  }

  return {
    mode,
    resolution,
    label: resolution === 'remote' ? '自动 / 远端' : '自动 / 本地',
    detail: resolution === 'remote' ? '自动优先读取远端工作区快照' : error ?? '远端失败后回退到本地快照',
  }
}
