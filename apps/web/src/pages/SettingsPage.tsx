import { useMemo, useState } from 'react'
import { useSettingsStore } from '../domains/settings/useSettingsStore'
import type { LlmConfigView, LlmProviderPresetView, UpdateLlmConfigRequest } from '../types'

interface EditableConfigState {
  profileId?: string
  presetId?: string
  displayName: string
  modelId: string
  apiBaseUrl: string
  apiKey: string
  timeoutMs: string
  systemPrompt: string
  activate: boolean
}

interface InlineFeedback {
  tone: 'success' | 'error'
  message: string
}

interface EditorState {
  mode: 'create' | 'edit'
  profileId: string | null
}

interface ProfileTestState {
  profileId: string
  success: boolean
  message: string
  latencyMs?: number | null
}

function buildDraftFromPreset(preset: LlmProviderPresetView | null, source?: LlmConfigView | null): EditableConfigState {
  return {
    profileId: source?.id,
    presetId: source?.presetId ?? preset?.id,
    displayName: source?.displayName ?? preset?.label ?? '',
    modelId: source?.modelId ?? preset?.defaultModelId ?? '',
    apiBaseUrl: source?.apiBaseUrl ?? preset?.apiBaseUrl ?? '',
    apiKey: '',
    timeoutMs: String(source?.timeoutMs ?? 60000),
    systemPrompt: source?.systemPrompt ?? '你是 Loom，一位谨慎、透明、执行过程可见的 AI 协作伙伴。',
    activate: source?.active ?? !source,
  }
}

export function SettingsPage() {
  const settings = useSettingsStore()
  const providerPresets = settings.providerPresets ?? []
  const savedConfigs = settings.llmConfigs ?? []
  const activeConfig = settings.activeConfig ?? null
  const recommendedPreset = useMemo(
    () => providerPresets.find((preset) => preset.recommended) ?? providerPresets[0] ?? null,
    [providerPresets],
  )

  const [editorState, setEditorState] = useState<EditorState | null>(null)
  const [formState, setFormState] = useState<EditableConfigState>(() => buildDraftFromPreset(recommendedPreset, activeConfig))
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [inlineFeedback, setInlineFeedback] = useState<InlineFeedback | null>(null)
  const [busyAction, setBusyAction] = useState<string | null>(null)
  const [editorTestState, setEditorTestState] = useState<ProfileTestState | null>(null)
  const [profileTestState, setProfileTestState] = useState<ProfileTestState | null>(null)

  const editingConfig = useMemo(
    () => savedConfigs.find((config) => config.id === editorState?.profileId) ?? null,
    [editorState?.profileId, savedConfigs],
  )

  const activePreset = useMemo(
    () => providerPresets.find((preset) => preset.id === formState.presetId) ?? recommendedPreset,
    [formState.presetId, providerPresets, recommendedPreset],
  )

  const modelOptions = activePreset?.modelOptions ?? []
  const isBusy = busyAction != null

  const configurationRequest = useMemo<UpdateLlmConfigRequest>(
    () => ({
      profileId: formState.profileId,
      presetId: formState.presetId,
      displayName: formState.displayName.trim() || undefined,
      modelId: formState.modelId,
      apiBaseUrl: formState.apiBaseUrl.trim() || undefined,
      apiKey: formState.apiKey.trim() ? formState.apiKey.trim() : undefined,
      timeoutMs: Number.isFinite(Number(formState.timeoutMs)) ? Number(formState.timeoutMs) : undefined,
      systemPrompt: formState.systemPrompt.trim() || undefined,
      activate: formState.activate,
    }),
    [formState],
  )

  const openCreateEditor = () => {
    setEditorState({ mode: 'create', profileId: null })
    setFormState(buildDraftFromPreset(recommendedPreset, null))
    setShowAdvanced(false)
    setEditorTestState(null)
    setInlineFeedback(null)
  }

  const openEditEditor = (config: LlmConfigView) => {
    const preset = providerPresets.find((item) => item.id === config.presetId) ?? recommendedPreset
    setEditorState({ mode: 'edit', profileId: config.id })
    setFormState(buildDraftFromPreset(preset, config))
    setShowAdvanced(false)
    setEditorTestState(null)
    setInlineFeedback(null)
  }

  const closeEditor = () => {
    setEditorState(null)
    setEditorTestState(null)
    setShowAdvanced(false)
  }

  const runEditorTest = async () => {
    if (!activePreset?.supported) {
      return
    }

    setBusyAction('editor-test')
    setInlineFeedback(null)

    try {
      const result = await settings.testLlmSettings(configurationRequest)
      setEditorTestState({
        profileId: formState.profileId ?? 'draft',
        success: result.success,
        message: result.message,
        latencyMs: result.latencyMs,
      })
      setInlineFeedback({
        tone: 'success',
        message: `${result.modelId} 测试通过${result.latencyMs != null ? `，耗时 ${result.latencyMs} ms` : ''}。`,
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : '模型测试失败。'
      setEditorTestState({
        profileId: formState.profileId ?? 'draft',
        success: false,
        message,
      })
      setInlineFeedback({
        tone: 'error',
        message,
      })
    } finally {
      setBusyAction(null)
    }
  }

  const saveSettings = async () => {
    if (!activePreset?.supported) {
      return
    }

    setBusyAction(editorState?.mode === 'create' ? 'create-model' : `save-model:${formState.profileId ?? 'draft'}`)
    setInlineFeedback(null)

    try {
      await settings.updateLlmSettings(configurationRequest)
      setInlineFeedback({
        tone: 'success',
        message: `${formState.displayName || activePreset?.label || '模型配置'}已保存。`,
      })
      closeEditor()
    } catch (error) {
      setInlineFeedback({
        tone: 'error',
        message: error instanceof Error ? error.message : '保存配置失败。',
      })
    } finally {
      setBusyAction(null)
    }
  }

  const activateConfig = async (config: LlmConfigView) => {
    setBusyAction(`activate:${config.id}`)
    setInlineFeedback(null)

    try {
      await settings.updateLlmSettings({
        profileId: config.id,
        activate: true,
      })
      setInlineFeedback({
        tone: 'success',
        message: `${config.displayName} 已切换为当前模型。`,
      })
    } catch (error) {
      setInlineFeedback({
        tone: 'error',
        message: error instanceof Error ? error.message : '切换模型失败。',
      })
    } finally {
      setBusyAction(null)
    }
  }

  const testConfiguredModel = async (config: LlmConfigView) => {
    setBusyAction(`profile-test:${config.id}`)
    setInlineFeedback(null)

    try {
      const result = await settings.testLlmSettings({
        profileId: config.id,
      })
      setProfileTestState({
        profileId: config.id,
        success: result.success,
        message: result.message,
        latencyMs: result.latencyMs,
      })
      setInlineFeedback({
        tone: 'success',
        message: `${config.displayName} 测试通过${result.latencyMs != null ? `，耗时 ${result.latencyMs} ms` : ''}。`,
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : '模型测试失败。'
      setProfileTestState({
        profileId: config.id,
        success: false,
        message,
      })
      setInlineFeedback({
        tone: 'error',
        message,
      })
    } finally {
      setBusyAction(null)
    }
  }

  return (
    <section className="toolSurface toolSurface-settings">
      <div className="settingsListShell">
        <div className="settingsHeaderBar">
          <h2>模型</h2>
          <button className="uiButton uiButton-primary settingsActionButton settingsActionButton-primary" onClick={openCreateEditor} type="button">
            新增模型
          </button>
        </div>

        {inlineFeedback ? <div className={`settingsInlineFeedback settingsInlineFeedback-${inlineFeedback.tone}`}>{inlineFeedback.message}</div> : null}

        {savedConfigs.length === 0 ? (
          <div className="settingsEmptyState">
            <strong>还没有可用模型</strong>
            <p>先新增一个模型配置，再进行测试、切换和编辑。</p>
            <button className="uiButton uiButton-primary settingsActionButton settingsActionButton-primary" onClick={openCreateEditor} type="button">
              立即新增
            </button>
          </div>
        ) : (
          <div className="settingsProfileList">
            {savedConfigs.map((config) => {
              const latestTest = profileTestState?.profileId === config.id ? profileTestState : null

              return (
                <article className={`settingsProfileCard ${config.active ? 'active' : ''}`} key={config.id}>
                  <div className="settingsProfileTopRow">
                    <div className="settingsProfileIdentity">
                      <strong>{config.displayName}</strong>
                      <div className="settingsProfileSubline">
                        <span>{config.provider}</span>
                        <span>{config.modelId}</span>
                      </div>
                    </div>

                    <div className="settingsProfileTags">
                      {config.active ? <span className="chatMetaTag chatMetaTag-live">当前</span> : null}
                      <span className="chatMetaTag">{config.configured ? '已配置' : '缺少密钥'}</span>
                    </div>
                  </div>

                  {latestTest ? (
                    <div className={`settingsProfileTest settingsProfileTest-${latestTest.success ? 'success' : 'error'}`}>
                      <span>{latestTest.message}</span>
                      {latestTest.latencyMs != null ? <strong>{latestTest.latencyMs} ms</strong> : null}
                    </div>
                  ) : null}

                  <div className="actionRow">
                    <button className="uiButton uiButton-secondary settingsActionButton" disabled={isBusy} onClick={() => void testConfiguredModel(config)} type="button">
                      {busyAction === `profile-test:${config.id}` ? '测试中...' : '测试'}
                    </button>
                    <button className="uiButton uiButton-secondary settingsActionButton" disabled={isBusy} onClick={() => openEditEditor(config)} type="button">
                      编辑
                    </button>
                    <button
                      className="uiButton uiButton-primary settingsActionButton settingsActionButton-primary"
                      disabled={config.active || isBusy}
                      onClick={() => void activateConfig(config)}
                      type="button"
                    >
                      {config.active ? '使用中' : '切换到此模型'}
                    </button>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </div>

      {editorState ? (
        <div className="settingsDialogBackdrop" role="presentation">
          <div aria-modal="true" className="uiDialog settingsDialog settingsEditorDialog" role="dialog">
            <div className="settingsDialogHeader">
              <h3>{editorState.mode === 'create' ? '新增模型' : '编辑模型'}</h3>
              <button className="uiButton uiButton-secondary settingsActionButton" onClick={closeEditor} type="button">
                关闭
              </button>
            </div>

            <div className="settingsEditorGrid">
              <label className="uiField formField">
                <span>配置名称</span>
                <input
                  className="uiFieldControl"
                  onChange={(event) => setFormState((current) => ({ ...current, displayName: event.target.value }))}
                  placeholder="例如：Kimi Coding"
                  value={formState.displayName}
                />
              </label>

              <label className="uiField formField">
                <span>预设提供方</span>
                <select
                  className="uiFieldControl"
                  onChange={(event) => {
                    const nextPreset = providerPresets.find((preset) => preset.id === event.target.value) ?? null
                    setFormState((current) => ({
                      ...current,
                      presetId: nextPreset?.id,
                      displayName: current.profileId ? current.displayName : nextPreset?.label ?? '',
                      modelId: nextPreset?.defaultModelId ?? '',
                      apiBaseUrl: nextPreset?.apiBaseUrl ?? '',
                    }))
                    setEditorTestState(null)
                  }}
                  value={formState.presetId ?? ''}
                >
                  {providerPresets.map((preset) => (
                    <option disabled={!preset.supported} key={preset.id} value={preset.id}>
                      {preset.label}
                      {preset.supported ? '' : '（即将支持）'}
                    </option>
                  ))}
                </select>
              </label>

              <label className="uiField formField">
                <span>模型</span>
                <select
                  className="uiFieldControl"
                  onChange={(event) => {
                    setFormState((current) => ({ ...current, modelId: event.target.value }))
                    setEditorTestState(null)
                  }}
                  value={formState.modelId}
                >
                  {modelOptions.map((model) => (
                    <option key={model.id} value={model.id}>
                      {model.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="uiField formField">
                <span>API Key</span>
                <input
                  className="uiFieldControl"
                  onChange={(event) => setFormState((current) => ({ ...current, apiKey: event.target.value }))}
                  placeholder={editingConfig?.configured ? `已保存密钥：${editingConfig.apiKeyHint}` : '粘贴提供方 API Key'}
                  type="password"
                  value={formState.apiKey}
                />
              </label>
            </div>

            <label className="settingsCheckboxRow">
              <input
                checked={formState.activate}
                onChange={(event) => setFormState((current) => ({ ...current, activate: event.target.checked }))}
                type="checkbox"
              />
              <span>保存后立刻设为当前模型</span>
            </label>

            <details
              className="settingsDisclosure"
              onToggle={(event) => setShowAdvanced((event.currentTarget as HTMLDetailsElement).open)}
              open={showAdvanced}
            >
              <summary>高级配置</summary>
              <div className="settingsDisclosureBody">
                <div className="settingsEditorGrid">
                  <label className="uiField formField">
                    <span>API Base URL</span>
                    <input
                      className="uiFieldControl"
                      onChange={(event) => setFormState((current) => ({ ...current, apiBaseUrl: event.target.value }))}
                      placeholder="https://api.moonshot.cn/v1"
                      value={formState.apiBaseUrl}
                    />
                  </label>

                  <label className="uiField formField">
                    <span>超时（ms）</span>
                    <input
                      className="uiFieldControl"
                      inputMode="numeric"
                      onChange={(event) => setFormState((current) => ({ ...current, timeoutMs: event.target.value }))}
                      value={formState.timeoutMs}
                    />
                  </label>

                  <label className="uiField formField settingsEditorWide">
                    <span>系统提示词</span>
                    <textarea
                      className="uiFieldControl"
                      onChange={(event) => setFormState((current) => ({ ...current, systemPrompt: event.target.value }))}
                      rows={5}
                      value={formState.systemPrompt}
                    />
                  </label>
                </div>
              </div>
            </details>

            {editorTestState ? (
              <div className={`settingsProfileTest settingsProfileTest-${editorTestState.success ? 'success' : 'error'}`}>
                <span>{editorTestState.message}</span>
                {editorTestState.latencyMs != null ? <strong>{editorTestState.latencyMs} ms</strong> : null}
              </div>
            ) : null}

            <div className="actionRow">
              <button className="uiButton uiButton-secondary settingsActionButton" disabled={!activePreset?.supported || isBusy} onClick={() => void runEditorTest()} type="button">
                {busyAction === 'editor-test' ? '测试中...' : '测试模型'}
              </button>
              <button
                className="uiButton uiButton-primary settingsActionButton settingsActionButton-primary"
                disabled={!activePreset?.supported || isBusy}
                onClick={() => void saveSettings()}
                type="button"
              >
                {busyAction?.startsWith('save-model') || busyAction === 'create-model' ? '保存中...' : '保存配置'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
