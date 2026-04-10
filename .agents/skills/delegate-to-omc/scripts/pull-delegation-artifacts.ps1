[CmdletBinding()]
param(
    [string]$TaskId,
    [string]$DelegationRoot = ".delegations",
    [string]$SshHost = "jd",
    [string]$RemoteDelegationRoot = "/home/lingfeng/loom/.delegations",
    [Alias("h")]
    [switch]$Help
)

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId)) {
    Write-Host "Usage: ./pull-delegation-artifacts.ps1 -TaskId <task-id> [-DelegationRoot <path>] [-SshHost <host>] [-RemoteDelegationRoot <path>]"
    exit $(if ($Help) { 0 } else { 1 })
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

$taskDir = Join-Path (Get-AbsolutePath $DelegationRoot) $TaskId
New-Item -ItemType Directory -Path $taskDir -Force | Out-Null
$remoteArchive = (& ssh $SshHost "mktemp /tmp/loom-delegation.XXXXXX.tar").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remoteArchive)) {
    throw "Failed to allocate remote archive for $TaskId"
}

try {
    & ssh $SshHost "cd '$RemoteDelegationRoot/$TaskId' && tar -cf '$remoteArchive' ."
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create remote artifact archive for $TaskId"
    }

    $localArchive = Join-Path ([System.IO.Path]::GetTempPath()) ("loom-delegation-" + [System.Guid]::NewGuid().ToString("N") + ".tar")
    & scp "${SshHost}:$remoteArchive" $localArchive
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to download artifacts for $TaskId"
    }

    & tar.exe -xf $localArchive -C $taskDir
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to extract artifacts for $TaskId"
    }
} finally {
    & ssh $SshHost "rm -f '$remoteArchive'" | Out-Null
    if ($localArchive -and (Test-Path $localArchive)) {
        Remove-Item -LiteralPath $localArchive -Force
    }
}

Write-Host "Artifacts pulled to $taskDir"
