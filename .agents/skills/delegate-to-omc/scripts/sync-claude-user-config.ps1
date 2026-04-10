[CmdletBinding()]
param(
    [string]$SourceRoot = "$HOME\.claude",
    [string]$SshHost = "jd",
    [string]$RemoteRoot = "~/.claude",
    [string[]]$IncludePaths = @(".credentials.json", ".omc-config.json", "settings.json", "settings.local.json"),
    [switch]$SkipVerify,
    [Alias("h")]
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: ./sync-claude-user-config.ps1 [-SourceRoot <path>] [-SshHost <host>] [-RemoteRoot <path>] [-IncludePaths <paths...>] [-SkipVerify]"
    exit 0
}

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

Require-Command "ssh"
Require-Command "scp"

$sourceRootPath = Get-AbsolutePath $SourceRoot
if (-not (Test-Path $sourceRootPath)) {
    throw "Claude source root not found: $sourceRootPath"
}

$itemsToCopy = @()
foreach ($relativePath in $IncludePaths) {
    $fullPath = Join-Path $sourceRootPath $relativePath
    if (Test-Path $fullPath) {
        $itemsToCopy += [PSCustomObject]@{
            RelativePath = $relativePath
            FullPath = $fullPath
        }
    }
}

if ($itemsToCopy.Count -eq 0) {
    throw "No Claude config files matched the include list under $sourceRootPath"
}

& ssh $SshHost "mkdir -p $RemoteRoot"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create remote Claude config root: $RemoteRoot"
}

foreach ($item in $itemsToCopy) {
    $remotePath = "$RemoteRoot/$($item.RelativePath -replace '\\','/')"
    $remoteDir = [System.IO.Path]::GetDirectoryName($remotePath).Replace("\", "/")
    if (-not [string]::IsNullOrWhiteSpace($remoteDir)) {
        & ssh $SshHost "mkdir -p '$remoteDir'"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create remote Claude config directory: $remoteDir"
        }
    }

    & scp $item.FullPath "${SshHost}:$remotePath"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to upload $($item.RelativePath) to $remotePath"
    }
}

Write-Host "Synced $($itemsToCopy.Count) Claude user config file(s) to ${SshHost}:$RemoteRoot"

if (-not $SkipVerify) {
    $verifyCommand = "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH; printf 'Reply with CLAUDE_P_OK only.`n' | claude -p --setting-sources 'user,project'"
    & ssh $SshHost $verifyCommand
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Remote claude -p verification failed. Treat delegation as degraded until the preflight checks pass."
        exit 2
    }

    Write-Host "Remote claude -p verification succeeded."
}
