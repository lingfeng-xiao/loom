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

Require-Command "ssh"
Require-Command "scp"
Require-Command "tar"

$repoRootPath = Get-AbsolutePath $RepoRoot
if ([string]::IsNullOrWhiteSpace($DelegationRoot)) {
    $DelegationRoot = Join-Path $repoRootPath ".delegations"
}

$delegationRootPath = Get-AbsolutePath $DelegationRoot
$taskDir = Join-Path $delegationRootPath $TaskId
$remoteTaskDir = "$RemoteDelegationRoot/$TaskId"
$remoteTasksRoot = "$remoteTaskDir/tasks"
New-Item -ItemType Directory -Path $taskDir -Force | Out-Null

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
    & tar -cf - -C $tasksDirPath . | & ssh $SshHost "tar -xf - -C '$remoteTasksRoot'"
}

$remoteTaskArg = if ([string]::IsNullOrWhiteSpace($TaskFile)) { "--tasks-dir '$remoteTasksRoot'" } else { "--task-file '$remoteTaskDir/brief.md'" }
$remoteCommand = @(
    "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH",
    "cd '$RemoteRepoRoot'",
    "bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-omc-team.sh' --task-id '$TaskId' --repo-root '$RemoteRepoRoot' --base-ref '$BaseRef' --worktree-root '$RemoteWorktreeRoot' --delegation-root '$RemoteDelegationRoot' $remoteTaskArg" + $(if ($DryRun) { " --dry-run" } else { "" })
) -join "; "
Set-Content -Path (Join-Path $taskDir "command.preview.txt") -Value "ssh $SshHost `"$remoteCommand`"" -Encoding utf8

& ssh $SshHost $remoteCommand
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Remote OMC team command exited with code $LASTEXITCODE. Pulling artifacts anyway."
}

& (Join-Path $PSScriptRoot "pull-delegation-artifacts.ps1") -TaskId $TaskId -DelegationRoot $delegationRootPath -SshHost $SshHost -RemoteDelegationRoot $RemoteDelegationRoot
Write-Host "Pulled remote artifacts into $taskDir"
