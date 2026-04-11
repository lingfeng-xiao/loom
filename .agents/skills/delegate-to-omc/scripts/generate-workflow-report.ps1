[CmdletBinding()]
param(
    [string]$TaskId,
    [string]$DelegationRoot = ".delegations",
    [string]$ReleaseId = "",
    [string]$RollbackRef = "",
    [Alias("h")]
    [switch]$Help
)

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId)) {
    Write-Host "Usage: ./generate-workflow-report.ps1 -TaskId <task-id> [-DelegationRoot <path>] [-ReleaseId <id>] [-RollbackRef <ref>]"
    exit $(if ($Help) { 0 } else { 1 })
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Read-JsonOrDefault {
    param([string]$PathValue, $DefaultValue)
    if (-not (Test-Path $PathValue)) { return $DefaultValue }
    try {
        return Get-Content $PathValue -Raw | ConvertFrom-Json
    } catch {
        return [pscustomobject]@{ _read_error = $_.Exception.Message }
    }
}

function Add-Unique {
    param([System.Collections.Generic.List[string]]$List, [string]$Value)
    if (-not [string]::IsNullOrWhiteSpace($Value) -and -not $List.Contains($Value)) {
        $List.Add($Value)
    }
}

$taskDir = Join-Path (Get-AbsolutePath $DelegationRoot) $TaskId
$resultPath = Join-Path $taskDir "result.json"
$preflightPath = Join-Path $taskDir "preflight.json"
$reviewPath = Join-Path $taskDir "review-notes.md"
$reviewResultPath = Join-Path $taskDir "review-result.json"
$closeoutPath = Join-Path $taskDir "closeout.json"
$reportJsonPath = Join-Path $taskDir "workflow-report.json"
$reportMdPath = Join-Path $taskDir "workflow-report.md"

$result = Read-JsonOrDefault -PathValue $resultPath -DefaultValue ([pscustomobject]@{})
$preflight = Read-JsonOrDefault -PathValue $preflightPath -DefaultValue ([pscustomobject]@{})
$reviewData = Read-JsonOrDefault -PathValue $reviewResultPath -DefaultValue ([pscustomobject]@{})
$closeout = Read-JsonOrDefault -PathValue $closeoutPath -DefaultValue ([pscustomobject]@{})
$reviewText = if (Test-Path $reviewPath) { Get-Content $reviewPath -Raw } else { "" }
$reviewMatch = [regex]::Match($reviewText, 'REVIEW_RESULT:\s*(\S+)')

$reviewResult = if ($closeout.review_result) { $closeout.review_result } elseif ($reviewData.review_result) { $reviewData.review_result } elseif ($reviewMatch.Success) { $reviewMatch.Groups[1].Value.Trim().ToUpperInvariant() } else { "UNKNOWN" }
$workerStatus = if ($closeout.worker_status) { $closeout.worker_status } elseif ($result.worker_status) { $result.worker_status } else { "UNKNOWN" }
$preflightStatus = if ($closeout.preflight_status) { $closeout.preflight_status } elseif ($preflight.status) { $preflight.status } else { "UNKNOWN" }
$closeable = ($reviewResult -eq "PASS" -and $workerStatus -eq "SUCCESS" -and $preflightStatus -eq "PASS")
$closed = $closeable
$finalState = if ($closeable) { "CLOSED" } else { "BLOCKED" }

$issues = [System.Collections.Generic.List[string]]::new()
foreach ($issue in @($reviewData.issues)) { Add-Unique -List $issues -Value ([string]$issue) }
if ($preflightStatus -ne "PASS") { Add-Unique -List $issues -Value "Preflight status is $preflightStatus." }
if ($workerStatus -ne "SUCCESS") { Add-Unique -List $issues -Value "Worker status is $workerStatus." }
if ($reviewResult -ne "PASS") { Add-Unique -List $issues -Value "Review result is $reviewResult." }
if ($result.timed_out) { Add-Unique -List $issues -Value "Worker hit overall timeout." }
if ($result.idle_timed_out) { Add-Unique -List $issues -Value "Worker hit idle-output timeout." }

$mismatches = [System.Collections.Generic.List[string]]::new()
if ($result.declared_result -eq "SUCCESS" -and $workerStatus -ne "SUCCESS") { Add-Unique -List $mismatches -Value "Worker declared SUCCESS but closeout checks did not accept it." }
if ($null -ne $result.contract_complete -and -not $result.contract_complete) { Add-Unique -List $mismatches -Value "Worker result contract was incomplete." }
if ($null -ne $result.diff_present -and -not $result.diff_present) { Add-Unique -List $mismatches -Value "No real diff was detected." }
if ($null -ne $result.validation_reported -and -not $result.validation_reported) { Add-Unique -List $mismatches -Value "Validation was not meaningfully reported." }

$nextActions = @($reviewData.minimal_fix_list)
if ($nextActions.Count -eq 0) {
    $nextActions = if ($closeable) { @("No follow-up required.") } else { @("Fix the blocking issues and rerun closeout.") }
}

$evidenceFiles = [ordered]@{
    brief = (Join-Path $taskDir "brief.md")
    result = $resultPath
    preflight = $preflightPath
    review_notes = $reviewPath
    review_result = $reviewResultPath
    closeout = $closeoutPath
    workflow_report_json = $reportJsonPath
    workflow_report_md = $reportMdPath
}

$payload = [ordered]@{
    task_id = $TaskId
    final_state = $finalState
    closed = $closed
    closeable = $closeable
    review_result = $reviewResult
    worker_status = $workerStatus
    preflight_status = $preflightStatus
    release_id = $ReleaseId
    rollback_ref = $RollbackRef
    issues = @($issues)
    expectation_mismatches = @($mismatches)
    auto_recovered = ($issues.Count -gt 0 -and $closeable)
    residual_risk = $(if ($closeable) { "None recorded." } else { "Closeout is blocked; inspect issues and evidence." })
    next_actions = @($nextActions)
    evidence_files = $evidenceFiles
    generated_at = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
}

$payload | ConvertTo-Json -Depth 6 | Set-Content -Path $reportJsonPath -Encoding utf8

$md = @(
    "# Workflow Report",
    "",
    "- Task ID: $TaskId",
    "- Final state: $finalState",
    "- Closed: $($closed.ToString().ToLowerInvariant())",
    "- Review result: $reviewResult",
    "- Worker status: $workerStatus",
    "- Preflight status: $preflightStatus",
    "- Release ID: $ReleaseId",
    "- Rollback ref: $RollbackRef",
    "",
    "## Issues",
    ""
) + ($(if ($issues.Count -gt 0) { $issues | ForEach-Object { "- $_" } } else { @("- None") })) + @(
    "",
    "## Expectation Mismatches",
    ""
) + ($(if ($mismatches.Count -gt 0) { $mismatches | ForEach-Object { "- $_" } } else { @("- None") })) + @(
    "",
    "## Residual Risk",
    "",
    $payload.residual_risk,
    "",
    "## Next Actions",
    ""
) + ($nextActions | ForEach-Object { "- $_" }) + @(
    "",
    "## Evidence",
    ""
) + ($evidenceFiles.GetEnumerator() | ForEach-Object { "- $($_.Key): $($_.Value)" })

Set-Content -Path $reportMdPath -Value ($md -join "`r`n") -Encoding utf8
Write-Host "Workflow report written to $reportMdPath and $reportJsonPath"
