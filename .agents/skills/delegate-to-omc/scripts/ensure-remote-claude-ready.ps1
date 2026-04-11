[CmdletBinding()]
param(
    [string]$SshHost = "jd",
    [string]$RemoteRepoRoot = "/home/lingfeng/loom",
    [string]$SourceRoot = "$HOME\.claude",
    [string]$RemoteRoot = "~/.claude",
    [string[]]$IncludePaths = @(".credentials.json", ".omc-config.json", "settings.json", "settings.local.json"),
    [switch]$ForceSync,
    [Alias("h")]
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: ./ensure-remote-claude-ready.ps1 [-SshHost <host>] [-RemoteRepoRoot <path>] [-SourceRoot <path>] [-RemoteRoot <path>] [-IncludePaths <paths...>] [-ForceSync]"
    exit 0
}

function Test-RemoteClaudeReady {
    param(
        [string]$RemoteHost,
        [string]$RepoRoot
    )

    $verifyCommand = @(
        "export PATH=`$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:`$PATH",
        "cd '$RepoRoot'",
        "printf 'Reply with CLAUDE_P_OK only.`n' | claude -p --setting-sources 'user,project'"
    ) -join "; "

    $output = & ssh $RemoteHost $verifyCommand 2>&1
    $exitCode = $LASTEXITCODE
    $verified = ($exitCode -eq 0 -and ($output -join "`n") -match "CLAUDE_P_OK")

    [PSCustomObject]@{
        Ready = $verified
        ExitCode = $exitCode
        Output = ($output -join "`n")
    }
}

$verification = Test-RemoteClaudeReady -RemoteHost $SshHost -RepoRoot $RemoteRepoRoot
if ($verification.Ready -and -not $ForceSync) {
    Write-Host "Remote claude -p is already ready on ${SshHost}. Skipping config sync."
    exit 0
}

if (-not $verification.Ready) {
    Write-Warning "Remote claude -p verification failed on ${SshHost}. Syncing local Claude config."
}

& (Join-Path $PSScriptRoot "sync-claude-user-config.ps1") `
    -SourceRoot $SourceRoot `
    -SshHost $SshHost `
    -RemoteRoot $RemoteRoot `
    -IncludePaths $IncludePaths `
    -ForceSync
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$postSyncVerification = Test-RemoteClaudeReady -RemoteHost $SshHost -RepoRoot $RemoteRepoRoot
if (-not $postSyncVerification.Ready) {
    throw "Remote claude -p is still unavailable after config sync on ${SshHost}. Output: $($postSyncVerification.Output)"
}

Write-Host "Remote claude -p is ready on ${SshHost}."
