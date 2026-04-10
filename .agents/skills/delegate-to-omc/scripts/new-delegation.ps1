[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$TaskId,

    [Parameter(Mandatory = $false)]
    [string]$Title,

    [Parameter(Mandatory = $false)]
    [string]$DelegationRoot = (Join-Path (Get-Location) ".delegations"),

    [Alias("h")]
    [switch]$Help
)

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId) -or [string]::IsNullOrWhiteSpace($Title)) {
    Write-Host "Usage: ./new-delegation.ps1 -TaskId <task-id> -Title <short title> [-DelegationRoot <path>]"
    exit $(if ($Help) { 0 } else { 1 })
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

$skillRoot = Split-Path $PSScriptRoot -Parent
$delegationRootPath = Get-AbsolutePath $DelegationRoot
$taskDir = Join-Path $delegationRootPath $TaskId

if (Test-Path $taskDir) {
    Write-Error "Delegation already exists: $taskDir"
    exit 1
}

New-Item -ItemType Directory -Path $taskDir -Force | Out-Null

$briefTemplatePath = Join-Path $skillRoot "assets/task-brief-template.md"
$briefTemplate = Get-Content $briefTemplatePath -Raw
$briefContent = $briefTemplate.Replace("{{TASK_ID}}", $TaskId).Replace("{{TASK_TITLE}}", $Title)

$briefPath = Join-Path $taskDir "brief.md"
$handoffPath = Join-Path $taskDir "handoff-to-claude.md"
$reviewPath = Join-Path $taskDir "review-notes.md"

Set-Content -Path $briefPath -Value $briefContent -Encoding utf8
Set-Content -Path $handoffPath -Encoding utf8 -Value @"
# Handoff To Claude

Use `brief.md` in this folder as the source of truth for task `$TaskId`.

Suggested next step:

```powershell
./.agents/skills/delegate-to-omc/scripts/delegate-to-claude.ps1 -TaskId "$TaskId" -TaskFile "$briefPath"
```
"@
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

Write-Host "Created delegation scaffold at $taskDir"
Write-Host "Next:"
Write-Host "  ./.agents/skills/delegate-to-omc/scripts/delegate-to-claude.ps1 -TaskId `"$TaskId`" -TaskFile `"$briefPath`""
