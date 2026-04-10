[CmdletBinding()]
param(
    [string]$TaskId,
    [string]$TaskFile,
    [string]$TasksDir,
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
    Write-Host "Usage: ./delegate-to-omc-team.ps1 -TaskId <task-id> (-TaskFile <brief.md> | -TasksDir <dir>) [-RepoRoot <repo>] [-BaseRef <ref>] [-DelegationRoot <path>] [-SshHost <host>] [-RemoteRepoRoot <path>] [-RemoteWorktreeRoot <path>] [-RemoteDelegationRoot <path>] [-DryRun]"
}

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId) -or ([string]::IsNullOrWhiteSpace($TaskFile) -and [string]::IsNullOrWhiteSpace($TasksDir))) {
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

function Ensure-ReviewScaffold {
    param(
        [string]$TaskDir,
        [string]$TaskId
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
            task_id = $TaskId
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
if ([string]::IsNullOrWhiteSpace($DelegationRoot)) {
    $DelegationRoot = Join-Path $repoRootPath ".delegations"
}

$delegationRootPath = Get-AbsolutePath $DelegationRoot
$taskDir = Join-Path $delegationRootPath $TaskId
$remoteTaskDir = "$RemoteDelegationRoot/$TaskId"
$remoteTasksRoot = "$remoteTaskDir/tasks"
New-Item -ItemType Directory -Path $taskDir -Force | Out-Null
Ensure-ReviewScaffold -TaskDir $taskDir -TaskId $TaskId

if (-not [string]::IsNullOrWhiteSpace($TaskFile)) {
    $taskFilePath = Get-AbsolutePath $TaskFile
    $briefOutputPath = Join-Path $taskDir "brief.md"
    if (-not ($taskFilePath.Equals($briefOutputPath, [System.StringComparison]::OrdinalIgnoreCase))) {
        Copy-Item -Path $taskFilePath -Destination $briefOutputPath -Force
    }
    & ssh $SshHost "mkdir -p '$remoteTaskDir'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create remote directory ${SshHost}:$remoteTaskDir"
    }
    & scp $taskFilePath "${SshHost}:$remoteTaskDir/brief.md"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to upload $taskFilePath to ${SshHost}:$remoteTaskDir/brief.md"
    }
} else {
    $tasksDirPath = Get-AbsolutePath $TasksDir
    & ssh $SshHost "rm -rf '$remoteTasksRoot' && mkdir -p '$remoteTasksRoot'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to prepare remote tasks directory ${SshHost}:$remoteTasksRoot"
    }

    $taskFiles = Get-ChildItem -LiteralPath $tasksDirPath -Filter *.md | Sort-Object Name
    if ($taskFiles.Count -eq 0) {
        throw "No markdown task files found under $tasksDirPath"
    }

    foreach ($task in $taskFiles) {
        & scp $task.FullName "${SshHost}:$remoteTasksRoot/$($task.Name)"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to upload $($task.FullName) to ${SshHost}:$remoteTasksRoot/$($task.Name)"
        }
    }
}

$remoteTaskArg = if ([string]::IsNullOrWhiteSpace($TaskFile)) { "--tasks-dir '$remoteTasksRoot'" } else { "--task-file '$remoteTaskDir/brief.md'" }
$serverCommand = "bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-omc-team.sh' --task-id '$TaskId' --repo-root '$RemoteRepoRoot' --base-ref '$BaseRef' --worktree-root '$RemoteWorktreeRoot' --delegation-root '$RemoteDelegationRoot' $remoteTaskArg"
if ($DryRun) {
    $serverCommand += " --dry-run"
}

$remoteCommand = @(
    "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH",
    "cd '$RemoteRepoRoot'",
    $serverCommand
) -join "; "
Set-Content -Path (Join-Path $taskDir "command.preview.txt") -Value "ssh $SshHost `"$remoteCommand`"" -Encoding utf8

& ssh $SshHost $remoteCommand
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Remote OMC team command exited with code $LASTEXITCODE. Pulling artifacts anyway."
}

& (Join-Path $PSScriptRoot "pull-delegation-artifacts.ps1") -TaskId $TaskId -DelegationRoot $delegationRootPath -SshHost $SshHost -RemoteDelegationRoot $RemoteDelegationRoot
Write-Host "Pulled remote artifacts into $taskDir"
