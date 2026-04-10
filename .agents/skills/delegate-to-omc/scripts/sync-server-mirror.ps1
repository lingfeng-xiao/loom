[CmdletBinding()]
param(
    [string]$SshHost = "jd",
    [string]$RemoteRepoRoot = "/home/lingfeng/loom",
    [string]$MirrorRoot = ".mirror/server-head",
    [switch]$DryRun,
    [Alias("h")]
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: ./sync-server-mirror.ps1 [-SshHost <host>] [-RemoteRepoRoot <path>] [-MirrorRoot <path>] [-DryRun]"
    exit 0
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    throw "Required command not found: ssh"
}

if (-not (Get-Command scp -ErrorAction SilentlyContinue)) {
    throw "Required command not found: scp"
}

$repoRootPath = Get-AbsolutePath "."
$mirrorRootPath = Get-AbsolutePath $MirrorRoot
if (-not $mirrorRootPath.StartsWith($repoRootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "MirrorRoot must stay inside the current repository."
}

$tmpDir = "$mirrorRootPath.tmp"
New-Item -ItemType Directory -Path (Split-Path $mirrorRootPath -Parent) -Force | Out-Null
if (Test-Path $tmpDir) { Remove-Item -LiteralPath $tmpDir -Recurse -Force }
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null

$remoteHead = (& ssh $SshHost "cd '$RemoteRepoRoot' && git rev-parse HEAD").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remoteHead)) {
    if (Test-Path $tmpDir) { Remove-Item -LiteralPath $tmpDir -Recurse -Force }
    throw "Remote repo at $RemoteRepoRoot does not have a valid git HEAD. Bootstrap the server repo before syncing the mirror."
}

$localRsync = Get-Command rsync -ErrorAction SilentlyContinue
$remoteHasRsync = $false
if ($localRsync) {
    & ssh $SshHost "command -v rsync >/dev/null 2>&1"
    $remoteHasRsync = ($LASTEXITCODE -eq 0)
}

if (-not $DryRun) {
    if ($localRsync -and $remoteHasRsync) {
        & rsync -a --delete `
            --exclude=".git" `
            --exclude="node_modules" `
            --exclude="target" `
            --exclude="logs" `
            --exclude="vault" `
            --exclude=".env" `
            --exclude=".delegations" `
            --exclude=".release" `
            --exclude=".tmp" `
            --exclude="*.log" `
            -e "ssh" `
            "${SshHost}:${RemoteRepoRoot.TrimEnd('/')}/" `
            "${tmpDir}\"
        if ($LASTEXITCODE -ne 0) { throw "Mirror sync failed via rsync." }
    } else {
        $remoteArchive = (& ssh $SshHost "mktemp /tmp/loom-mirror.XXXXXX.tar").Trim()
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remoteArchive)) {
            throw "Failed to allocate remote mirror archive."
        }

        $localArchive = Join-Path ([System.IO.Path]::GetTempPath()) ("loom-mirror-" + [System.Guid]::NewGuid().ToString("N") + ".tar")
        try {
            & ssh $SshHost "cd '$RemoteRepoRoot' && tar --exclude='.git' --exclude='node_modules' --exclude='target' --exclude='logs' --exclude='vault' --exclude='.env' --exclude='.delegations' --exclude='.release' --exclude='.tmp' --exclude='*.log' -cf '$remoteArchive' ."
            if ($LASTEXITCODE -ne 0) { throw "Failed to create remote mirror archive." }

            & scp "${SshHost}:${remoteArchive}" $localArchive
            if ($LASTEXITCODE -ne 0) { throw "Failed to download remote mirror archive." }

            & tar.exe -xf $localArchive -C $tmpDir
            if ($LASTEXITCODE -ne 0) { throw "Failed to extract remote mirror archive." }
        } finally {
            & ssh $SshHost "rm -f '$remoteArchive'" | Out-Null
            if (Test-Path $localArchive) { Remove-Item -LiteralPath $localArchive -Force }
        }
    }
}

([ordered]@{
    remote_repo = $RemoteRepoRoot
    remote_head = $remoteHead
    synced_at = (Get-Date).ToString("o")
    mode = if ($DryRun) { "dry-run" } else { "full-sync" }
    transport = if ($DryRun) { "none" } elseif ($localRsync -and $remoteHasRsync) { "rsync" } else { "tar" }
} | ConvertTo-Json) | Set-Content -Path (Join-Path $tmpDir ".mirror-meta.json") -Encoding utf8

if (Test-Path $mirrorRootPath) { Remove-Item -LiteralPath $mirrorRootPath -Recurse -Force }
Move-Item -LiteralPath $tmpDir -Destination $mirrorRootPath
Write-Host "Mirror refreshed at $mirrorRootPath"
