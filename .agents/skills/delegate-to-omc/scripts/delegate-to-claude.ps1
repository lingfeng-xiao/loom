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
if (-not ($taskFilePath.Equals($briefOutputPath, [System.StringComparison]::OrdinalIgnoreCase))) {
    Copy-Item -Path $taskFilePath -Destination $briefOutputPath -Force
}

$remoteCommand = @(
    "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH",
    "cd '$RemoteRepoRoot'",
    "bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-claude.sh' --task-id '$TaskId' --task-file '$remoteBriefPath' --repo-root '$RemoteRepoRoot' --base-ref '$BaseRef' --worktree-root '$RemoteWorktreeRoot' --delegation-root '$RemoteDelegationRoot'" + $(if ($DryRun) { " --dry-run" } else { "" })
) -join "; "
Set-Content -Path $commandFile -Value "ssh $SshHost `"$remoteCommand`"" -Encoding utf8

Send-FileToRemote -LocalFile $briefOutputPath -RemoteFile $remoteBriefPath -RemoteHost $SshHost
& ssh $SshHost $remoteCommand
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Remote Claude command exited with code $LASTEXITCODE. Pulling artifacts anyway."
}

& (Join-Path $PSScriptRoot "pull-delegation-artifacts.ps1") -TaskId $TaskId -DelegationRoot $delegationRootPath -SshHost $SshHost -RemoteDelegationRoot $RemoteDelegationRoot
Write-Host "Pulled remote artifacts into $taskDir"
