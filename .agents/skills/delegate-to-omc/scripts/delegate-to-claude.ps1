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
    [switch]$DryRun,
    [Alias("h")]
    [switch]$Help
)

function Show-Usage {
    Write-Host "Usage: ./delegate-to-claude.ps1 -TaskId <task-id> -TaskFile <brief.md> [-RepoRoot <repo>] [-BaseRef <ref>] [-DelegationRoot <path>] [-SshHost <host>] [-RemoteRepoRoot <path>] [-RemoteWorktreeRoot <path>] [-RemoteDelegationRoot <path>] [-DryRun]"
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

## Minimal fix list

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
$briefOutputPath = Join-Path $taskDir "brief.md"
$commandFile = Join-Path $taskDir "command.preview.txt"
$remoteTaskDir = "$RemoteDelegationRoot/$TaskId"
$remoteBriefPath = "$remoteTaskDir/brief.md"

New-Item -ItemType Directory -Path $taskDir -Force | Out-Null
Ensure-ReviewScaffold -TaskDir $taskDir
if (-not ($taskFilePath.Equals($briefOutputPath, [System.StringComparison]::OrdinalIgnoreCase))) {
    Copy-Item -Path $taskFilePath -Destination $briefOutputPath -Force
}

$serverCommand = "bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-claude.sh' --task-id '$TaskId' --task-file '$remoteBriefPath' --repo-root '$RemoteRepoRoot' --base-ref '$BaseRef' --worktree-root '$RemoteWorktreeRoot' --delegation-root '$RemoteDelegationRoot'"
if ($DryRun) {
    $serverCommand += " --dry-run"
}

$remoteCommand = @(
    "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH",
    "cd '$RemoteRepoRoot'",
    $serverCommand
) -join "; "
Set-Content -Path $commandFile -Value "ssh $SshHost `"$remoteCommand`"" -Encoding utf8

Send-FileToRemote -LocalFile $briefOutputPath -RemoteFile $remoteBriefPath -RemoteHost $SshHost
& ssh $SshHost $remoteCommand
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Remote Claude command exited with code $LASTEXITCODE. Pulling artifacts anyway."
}

& (Join-Path $PSScriptRoot "pull-delegation-artifacts.ps1") -TaskId $TaskId -DelegationRoot $delegationRootPath -SshHost $SshHost -RemoteDelegationRoot $RemoteDelegationRoot
Write-Host "Pulled remote artifacts into $taskDir"
