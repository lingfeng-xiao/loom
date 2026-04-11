[CmdletBinding()]
param(
    [string]$TaskId,
    [string]$TaskFile,
    [string]$RepoRoot = (Get-Location).Path,
    [string]$BaseRef = "main",
    [string]$DelegationRoot,
    [string]$SshHost = "jd",
    [string]$RemoteRepoRoot = "/home/lingfeng/loom",
    [string]$RemoteWorktreeRoot = "/home/lingfeng/worktrees",
    [string]$RemoteDelegationRoot = "/home/lingfeng/loom/.delegations",
    [int]$TimeoutSeconds = 1800,
    [int]$IdleTimeoutSeconds = 300,
    [int]$MaxFixAttempts = 3,
    [switch]$SkipEnsureClaudeReady,
    [switch]$ForceSyncClaudeConfig,
    [switch]$DryRun,
    [Alias("h")]
    [switch]$Help
)

function Show-Usage {
    Write-Host "Usage: ./delegate-to-claude.ps1 -TaskId <task-id> -TaskFile <brief.md> [-RepoRoot <repo>] [-BaseRef <ref>] [-DelegationRoot <path>] [-SshHost <host>] [-RemoteRepoRoot <path>] [-RemoteWorktreeRoot <path>] [-RemoteDelegationRoot <path>] [-TimeoutSeconds <seconds>] [-IdleTimeoutSeconds <seconds>] [-MaxFixAttempts <n>] [-SkipEnsureClaudeReady] [-ForceSyncClaudeConfig] [-DryRun]"
}

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId) -or [string]::IsNullOrWhiteSpace($TaskFile)) {
    Show-Usage
    exit $(if ($Help) { 0 } else { 1 })
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Send-FileToRemote {
    param(
        [string]$LocalFile,
        [string]$RemoteFile,
        [string]$RemoteHost
    )

    $remoteDir = [System.IO.Path]::GetDirectoryName($RemoteFile).Replace("\", "/")
    & ssh $RemoteHost "mkdir -p '$remoteDir'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create remote directory ${RemoteHost}:$remoteDir"
    }

    & scp $LocalFile "${RemoteHost}:$RemoteFile"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to upload $LocalFile to ${RemoteHost}:$RemoteFile"
    }
}

function ConvertTo-BashSingleQuoted {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'\''") + "'"
}

function Ensure-ReviewScaffold {
    param(
        [string]$TaskDir
    )

    $reviewPath = Join-Path $TaskDir "review-notes.md"
    $closeoutPath = Join-Path $TaskDir "closeout.json"
    if (-not (Test-Path $reviewPath)) {
        Set-Content -Path $reviewPath -Encoding utf8 -Value @"
# Review Notes

REVIEW_RESULT: PENDING

## Scope check

- TODO

## Validation check

- TODO

## Risk check

- TODO

## Recommended corrections

- TODO
"@
    }

    if (-not (Test-Path $closeoutPath)) {
        @{
            task_id = [System.IO.Path]::GetFileName($TaskDir)
            review_result = "PENDING"
            worker_status = "PENDING"
            preflight_status = "PENDING"
            closeable = $false
            closed = $false
            final_state = "PENDING_REVIEW"
            release_id = ""
            rollback_ref = ""
            review_file = $reviewPath
            result_file = (Join-Path $TaskDir "result.json")
            preflight_file = (Join-Path $TaskDir "preflight.json")
            closed_at = ""
        } | ConvertTo-Json | Set-Content -Path $closeoutPath -Encoding utf8
    }
}

function Save-AttemptSnapshot {
    param(
        [string]$TaskDir,
        [int]$Attempt
    )

    $snapshotDir = Join-Path $TaskDir ("attempts\attempt-{0:D2}" -f $Attempt)
    New-Item -ItemType Directory -Path $snapshotDir -Force | Out-Null
    foreach ($name in @(
        "brief.md",
        "claude.response.md",
        "git.status.txt",
        "git.diff.stat.txt",
        "preflight.json",
        "result.json",
        "review-notes.md",
        "review-result.json",
        "command.preview.txt"
    )) {
        $sourcePath = Join-Path $TaskDir $name
        if (Test-Path $sourcePath) {
            Copy-Item -Path $sourcePath -Destination (Join-Path $snapshotDir $name) -Force
        }
    }
}

function New-FixBrief {
    param(
        [string]$TaskDir,
        [int]$Attempt,
        [object]$Triage
    )

    $originalBriefPath = Join-Path $TaskDir "brief.original.md"
    $reviewResultPath = Join-Path $TaskDir "review-result.json"
    $fixBriefPath = Join-Path $TaskDir ("brief.fix-{0:D2}.md" -f $Attempt)

    if (-not (Test-Path $originalBriefPath)) {
        throw "Original brief not found: $originalBriefPath"
    }

    if (-not (Test-Path $reviewResultPath)) {
        throw "Review result not found: $reviewResultPath"
    }

    $baseBrief = (Get-Content $originalBriefPath -Raw).TrimEnd()
    $reviewResult = Get-Content $reviewResultPath -Raw | ConvertFrom-Json
    $issues = @($reviewResult.issues)
    $fixes = if ($reviewResult.recommended_corrections) { @($reviewResult.recommended_corrections) } else { @($reviewResult.minimal_fix_list) }

    $content = @(
        $baseBrief,
        "",
        "## Auto repair pass",
        "",
        "This is automated retry attempt $Attempt after Codex review triage selected `$($Triage.recommended_action)` with repair size `$($Triage.repair_size)`.",
        "",
        "### Issues to fix",
        ""
    ) + ($issues | ForEach-Object { "- $_" }) + @(
        "",
        "### Required corrections",
        ""
    ) + ($fixes | ForEach-Object { "- $_" }) + @(
        "",
        "### Retry rules",
        "",
        "- Keep the original goal, constraints, done-when items, and non-goals unchanged.",
        "- Apply only the bounded changes justified by the repair size and review findings.",
        "- If the selected repair size is no longer accurate after inspection, stop and report why instead of widening scope silently.",
        "- Re-run concrete validation and report it in TESTS_RUN.",
        "- Return the full output contract again."
    )

    Set-Content -Path $fixBriefPath -Value ($content -join "`r`n") -Encoding utf8
    return $fixBriefPath
}

function New-FailureTriage {
    param(
        [string]$TaskDir,
        [int]$Attempt
    )

    $resultPath = Join-Path $TaskDir "result.json"
    $preflightPath = Join-Path $TaskDir "preflight.json"
    $reviewResultPath = Join-Path $TaskDir "review-result.json"
    $triagePath = Join-Path $TaskDir "failure-triage.json"

    $result = if (Test-Path $resultPath) { Get-Content $resultPath -Raw | ConvertFrom-Json } else { [pscustomobject]@{} }
    $preflight = if (Test-Path $preflightPath) { Get-Content $preflightPath -Raw | ConvertFrom-Json } else { [pscustomobject]@{} }
    $review = if (Test-Path $reviewResultPath) { Get-Content $reviewResultPath -Raw | ConvertFrom-Json } else { [pscustomobject]@{ issues = @(); recommended_corrections = @(); minimal_fix_list = @() } }
    $issues = @($review.issues)
    $failedGates = New-Object System.Collections.Generic.List[string]
    $likelyCauses = New-Object System.Collections.Generic.List[string]

    if ($preflight.status -ne "PASS") {
        $failedGates.Add("preflight")
        $likelyCauses.Add("environment_or_infrastructure")
    }
    if ($result.worker_status -ne "SUCCESS") {
        $failedGates.Add("worker_status")
    }
    if ($null -ne $result.contract_complete -and -not $result.contract_complete) {
        $failedGates.Add("contract")
        $likelyCauses.Add("prompt_contract_or_worker_runtime")
    }
    if ($null -ne $result.diff_present -and -not $result.diff_present) {
        $failedGates.Add("diff")
        $likelyCauses.Add("task_execution_or_prompt_scope")
    }
    if ($null -ne $result.validation_reported -and -not $result.validation_reported) {
        $failedGates.Add("validation")
        $likelyCauses.Add("validation_instruction_or_worker_followthrough")
    }
    if ($null -ne $result.timed_out -and $result.timed_out) {
        $failedGates.Add("timeout")
        $likelyCauses.Add("task_too_large_or_worker_stalled")
    }
    if ($null -ne $result.idle_timed_out -and $result.idle_timed_out) {
        $failedGates.Add("idle_timeout")
        $likelyCauses.Add("task_too_large_or_worker_stalled")
    }
    if ($issues -match "outside Relevant files") {
        $failedGates.Add("scope")
        $likelyCauses.Add("scope_drift_or_quality_risk")
    }
    if ($issues -match "missing concrete Done when|Relevant files is empty") {
        $failedGates.Add("brief_quality")
        $likelyCauses.Add("brief_ambiguity")
    }

    $uniqueGates = @($failedGates | Select-Object -Unique)
    $uniqueCauses = @($likelyCauses | Select-Object -Unique)
    $repairSize = "small"
    $recommendedAction = "small_fix"
    $confidence = "medium"
    $needsUserInput = $false

    if ($uniqueGates -contains "preflight") {
        $repairSize = "not_repairable_in_current_task"
        $recommendedAction = "fix_infrastructure"
        $confidence = "high"
    } elseif ($uniqueGates -contains "scope") {
        $repairSize = "large"
        $recommendedAction = "codex_takeover"
        $confidence = "high"
    } elseif ($uniqueGates -contains "brief_quality") {
        $repairSize = "not_repairable_in_current_task"
        $recommendedAction = "clarify_brief"
        $confidence = "high"
        $needsUserInput = $true
    } elseif ($uniqueGates -contains "timeout" -or $uniqueGates -contains "idle_timeout") {
        $repairSize = "large"
        $recommendedAction = "resplit_task"
        $confidence = "medium"
    } elseif (($uniqueGates -contains "contract") -and ($uniqueGates -contains "diff")) {
        $repairSize = "medium"
        $recommendedAction = "medium_fix"
        $confidence = "medium"
    } elseif ($uniqueGates -contains "contract") {
        $repairSize = "tiny"
        $recommendedAction = "tiny_fix"
        $confidence = "medium"
    } elseif ($uniqueGates -contains "validation") {
        $repairSize = "small"
        $recommendedAction = "small_fix"
        $confidence = "medium"
    }

    $payload = [ordered]@{
        task_id = (Split-Path $TaskDir -Leaf)
        attempt = $Attempt
        failed_gates = $uniqueGates
        likely_causes = $uniqueCauses
        repair_size = $repairSize
        recommended_action = $recommendedAction
        confidence = $confidence
        needs_user_input = $needsUserInput
        evidence_files = [ordered]@{
            result = $resultPath
            preflight = $preflightPath
            review_result = $reviewResultPath
            review_notes = (Join-Path $TaskDir "review-notes.md")
            response = (Join-Path $TaskDir "claude.response.md")
            status = (Join-Path $TaskDir "git.status.txt")
            diff = (Join-Path $TaskDir "git.diff.stat.txt")
        }
        generated_at = (Get-Date).ToString("o")
    }
    $payload | ConvertTo-Json -Depth 5 | Set-Content -Path $triagePath -Encoding utf8
    return $payload
}

function Invoke-RemoteAttempt {
    param(
        [string]$AttemptTaskFilePath,
        [string]$TaskDir,
        [string]$DelegationRootPath
    )

    $briefOutputPath = Join-Path $TaskDir "brief.md"
    $commandFile = Join-Path $TaskDir "command.preview.txt"
    $remoteTaskDir = "$RemoteDelegationRoot/$TaskId"
    $remoteBriefPath = "$remoteTaskDir/brief.md"
    $remoteRunnerPath = "$remoteTaskDir/runner.sh"

    if (-not ($AttemptTaskFilePath.Equals($briefOutputPath, [System.StringComparison]::OrdinalIgnoreCase))) {
        Copy-Item -Path $AttemptTaskFilePath -Destination $briefOutputPath -Force
    }

    $runnerLines = @(
        "#!/usr/bin/env bash",
        "set -euo pipefail",
        "",
        'export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"',
        "cd $(ConvertTo-BashSingleQuoted $RemoteRepoRoot)",
        "",
        "args=(",
        "  --task-id $(ConvertTo-BashSingleQuoted $TaskId)",
        "  --task-file $(ConvertTo-BashSingleQuoted $remoteBriefPath)",
        "  --repo-root $(ConvertTo-BashSingleQuoted $RemoteRepoRoot)",
        "  --base-ref $(ConvertTo-BashSingleQuoted $BaseRef)",
        "  --worktree-root $(ConvertTo-BashSingleQuoted $RemoteWorktreeRoot)",
        "  --delegation-root $(ConvertTo-BashSingleQuoted $RemoteDelegationRoot)",
        "  --timeout-seconds $(ConvertTo-BashSingleQuoted ([string]$TimeoutSeconds))",
        "  --idle-timeout-seconds $(ConvertTo-BashSingleQuoted ([string]$IdleTimeoutSeconds))",
        ")"
    )
    if ($DryRun) {
        $runnerLines += "args+=(--dry-run)"
    }
    $runnerLines += 'exec bash "./.agents/skills/delegate-to-omc/scripts/server-delegate-to-claude.sh" "${args[@]}"'
    $runnerScript = ($runnerLines -join "`n") + "`n"
    Set-Content -Path $commandFile -Value "# Remote runner script: $remoteRunnerPath`n$runnerScript" -Encoding utf8

    Send-FileToRemote -LocalFile $briefOutputPath -RemoteFile $remoteBriefPath -RemoteHost $SshHost

    $localRunnerPath = Join-Path ([System.IO.Path]::GetTempPath()) ("loom-remote-runner-" + [System.Guid]::NewGuid().ToString("N") + ".sh")
    try {
        [System.IO.File]::WriteAllText($localRunnerPath, $runnerScript, [System.Text.Encoding]::UTF8)
        Send-FileToRemote -LocalFile $localRunnerPath -RemoteFile $remoteRunnerPath -RemoteHost $SshHost
    } finally {
        if (Test-Path $localRunnerPath) {
            Remove-Item -LiteralPath $localRunnerPath -Force
        }
    }

    $remoteRunnerQuoted = ConvertTo-BashSingleQuoted $remoteRunnerPath
    & ssh $SshHost "bash $remoteRunnerQuoted; rc=`$?; rm -f -- $remoteRunnerQuoted; exit `$rc"
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Remote Claude command exited with code $LASTEXITCODE. Pulling artifacts anyway."
    }

    & (Join-Path $PSScriptRoot "pull-delegation-artifacts.ps1") -TaskId $TaskId -DelegationRoot $DelegationRootPath -SshHost $SshHost -RemoteDelegationRoot $RemoteDelegationRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to pull delegation artifacts for $TaskId"
    }
}

Require-Command "ssh"
Require-Command "scp"

$repoRootPath = Get-AbsolutePath $RepoRoot
$taskFilePath = Get-AbsolutePath $TaskFile
if (-not (Test-Path $taskFilePath)) {
    throw "Task file not found: $taskFilePath"
}

if ([string]::IsNullOrWhiteSpace($DelegationRoot)) {
    $DelegationRoot = Join-Path $repoRootPath ".delegations"
}

$delegationRootPath = Get-AbsolutePath $DelegationRoot
$taskDir = Join-Path $delegationRootPath $TaskId

New-Item -ItemType Directory -Path $taskDir -Force | Out-Null
Ensure-ReviewScaffold -TaskDir $taskDir

$originalBriefPath = Join-Path $taskDir "brief.original.md"
if (-not (Test-Path $originalBriefPath)) {
    Copy-Item -Path $taskFilePath -Destination $originalBriefPath -Force
}

if (-not $SkipEnsureClaudeReady) {
    $ensureArgs = @{
        SshHost = $SshHost
        RemoteRepoRoot = $RemoteRepoRoot
    }
    if ($ForceSyncClaudeConfig) {
        $ensureArgs["ForceSync"] = $true
    }

    & (Join-Path $PSScriptRoot "ensure-remote-claude-ready.ps1") @ensureArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Remote Claude environment is not ready on ${SshHost}"
    }
}

Invoke-RemoteAttempt -AttemptTaskFilePath $taskFilePath -TaskDir $taskDir -DelegationRootPath $delegationRootPath
if ($DryRun) {
    Write-Host "Pulled remote artifacts into $taskDir"
    exit 0
}

$reviewScript = Join-Path $PSScriptRoot "review-delegation.ps1"
$finalReviewResult = "NEEDS_FIX"
$totalAttempts = [Math]::Max(1, $MaxFixAttempts + 1)
$attemptTaskFile = $taskFilePath

for ($attempt = 1; $attempt -le $totalAttempts; $attempt++) {
    if ($attempt -gt 1) {
        Invoke-RemoteAttempt -AttemptTaskFilePath $attemptTaskFile -TaskDir $taskDir -DelegationRootPath $delegationRootPath
    }

    & $reviewScript -TaskId $TaskId -DelegationRoot $delegationRootPath -Attempt $attempt
    $reviewPath = Join-Path $taskDir "review-result.json"
    $reviewResult = Get-Content $reviewPath -Raw | ConvertFrom-Json
    $finalReviewResult = $reviewResult.review_result
    Save-AttemptSnapshot -TaskDir $taskDir -Attempt $attempt

    if ($finalReviewResult -eq "PASS") {
        break
    }

    if ($attempt -ge $totalAttempts) {
        break
    }

    $triage = New-FailureTriage -TaskDir $taskDir -Attempt $attempt
    $retryableActions = @("tiny_fix", "small_fix", "medium_fix")
    if ($retryableActions -notcontains $triage.recommended_action) {
        Write-Warning "Codex review failed and triage selected $($triage.recommended_action) with repair size $($triage.repair_size). Not re-dispatching automatically."
        break
    }

    $attemptTaskFile = New-FixBrief -TaskDir $taskDir -Attempt ($attempt + 1) -Triage $triage
    Write-Warning "Codex review requested $($triage.recommended_action) ($($triage.repair_size)). Re-dispatching attempt $($attempt + 1) for task $TaskId."
}

Write-Host "Pulled remote artifacts into $taskDir"
if ($finalReviewResult -ne "PASS") {
    Write-Warning "Task $TaskId still needs fixes after $totalAttempts attempt(s). Inspect review-notes.md and review-result.json."
    exit 2
}
