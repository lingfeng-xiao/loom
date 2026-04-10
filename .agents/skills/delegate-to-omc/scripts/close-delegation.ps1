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
    Write-Host "Usage: ./close-delegation.ps1 -TaskId <task-id> [-DelegationRoot <path>] [-ReleaseId <id>] [-RollbackRef <ref>]"
    exit $(if ($Help) { 0 } else { 1 })
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

$taskDir = Join-Path (Get-AbsolutePath $DelegationRoot) $TaskId
$reviewPath = Join-Path $taskDir "review-notes.md"
$resultPath = Join-Path $taskDir "result.json"
$preflightPath = Join-Path $taskDir "preflight.json"
$closeoutPath = Join-Path $taskDir "closeout.json"

if (-not (Test-Path $reviewPath)) { throw "Review notes not found: $reviewPath" }
if (-not (Test-Path $resultPath)) { throw "Result file not found: $resultPath" }
if (-not (Test-Path $preflightPath)) { throw "Preflight file not found: $preflightPath" }

$reviewText = Get-Content $reviewPath -Raw
$reviewMatch = [regex]::Match($reviewText, 'REVIEW_RESULT:\s*(\S+)')
if (-not $reviewMatch.Success) {
    throw "REVIEW_RESULT not found in $reviewPath"
}

$reviewResult = $reviewMatch.Groups[1].Value.Trim().ToUpperInvariant()
$result = Get-Content $resultPath -Raw | ConvertFrom-Json
$preflight = Get-Content $preflightPath -Raw | ConvertFrom-Json
$closeable = ($reviewResult -eq "PASS" -and $result.worker_status -eq "SUCCESS" -and $preflight.status -eq "PASS")

$payload = [ordered]@{
    task_id = $TaskId
    review_result = $reviewResult
    worker_status = $result.worker_status
    preflight_status = $preflight.status
    closeable = $closeable
    closed = $closeable
    final_state = $(if ($closeable) { "CLOSED" } else { "BLOCKED" })
    release_id = $ReleaseId
    rollback_ref = $RollbackRef
    review_file = $reviewPath
    result_file = $resultPath
    preflight_file = $preflightPath
    closed_at = (Get-Date).ToString("o")
}

$payload | ConvertTo-Json | Set-Content -Path $closeoutPath -Encoding utf8

if (-not $closeable) {
    Write-Error "Delegation cannot be closed until REVIEW_RESULT=PASS, worker_status=SUCCESS, and preflight.status=PASS."
    exit 1
}

Write-Host "Closeout written to $closeoutPath"
