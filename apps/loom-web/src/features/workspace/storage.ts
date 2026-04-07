const LAST_CONVERSATION_KEY = 'loom:last-conversation'
const INSPECTOR_OPEN_KEY = 'loom:inspector-open'

type LastConversationMap = Record<string, string>

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

function writeJson(key: string, value: unknown) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // ignore storage failures in restricted environments
  }
}

export function getLastConversationId(projectId: string) {
  const map = readJson<LastConversationMap>(LAST_CONVERSATION_KEY, {})
  return map[projectId] ?? null
}

export function setLastConversationId(projectId: string, conversationId: string) {
  const map = readJson<LastConversationMap>(LAST_CONVERSATION_KEY, {})
  map[projectId] = conversationId
  writeJson(LAST_CONVERSATION_KEY, map)
}

export function getInspectorOpen(defaultValue: boolean) {
  const raw = window.localStorage.getItem(INSPECTOR_OPEN_KEY)
  if (raw === null) return defaultValue
  return raw === 'true'
}

export function setInspectorOpen(value: boolean) {
  window.localStorage.setItem(INSPECTOR_OPEN_KEY, String(value))
}
