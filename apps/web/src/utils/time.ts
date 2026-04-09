export function toTimestampMs(value?: string | null): number | null {
  if (!value) {
    return null
  }

  const timestamp = Date.parse(value)
  return Number.isNaN(timestamp) ? null : timestamp
}

export function formatDurationMs(durationMs: number): string {
  if (durationMs < 1000) {
    return `${durationMs}ms`
  }

  const seconds = durationMs / 1000
  if (seconds < 60) {
    return `${seconds.toFixed(seconds >= 10 ? 0 : 1)}s`
  }

  const minutes = Math.floor(seconds / 60)
  const remainderSeconds = Math.round(seconds % 60)
  return `${minutes}m ${remainderSeconds}s`
}

export function durationBetween(start?: string | null, end?: string | null, nowMs = Date.now()): number | null {
  const startedAtMs = toTimestampMs(start)
  if (startedAtMs == null) {
    return null
  }

  const completedAtMs = toTimestampMs(end)
  return Math.max(0, (completedAtMs ?? nowMs) - startedAtMs)
}
