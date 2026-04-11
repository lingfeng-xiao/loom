[CmdletBinding()]
param(
    [string]$TaskId,
    [string]$DelegationRoot = ".delegations",
    [int]$Attempt = 1,
    [Alias("h")]
    [switch]$Help
)

if ($Help -or [string]::IsNullOrWhiteSpace($TaskId)) {
    Write-Host "Usage: ./review-delegation.ps1 -TaskId <task-id> [-DelegationRoot <path>] [-Attempt <n>]"
    exit $(if ($Help) { 0 } else { 1 })
}

function Get-AbsolutePath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $PathValue))
}

function Get-BriefSectionLines {
    param(
        [string]$Text,
        [string]$Header
    )

    $pattern = "(?ms)^##\s+$([regex]::Escape($Header))\s*(?<body>.*?)(?=^##\s+|\z)"
    $match = [regex]::Match($Text, $pattern)
    if (-not $match.Success) {
        return @()
    }

    $lines = $match.Groups["body"].Value -split "`r?`n" |
        ForEach-Object {
            $_.Trim() -replace '^[\-\*\d\.\s]+' -replace '`', ''
        } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and -not $_.StartsWith("<!--") }
    return $lines
}

function Get-ChangedFiles {
    param([string]$StatusPath)

    if (-not (Test-Path $StatusPath)) {
        return @()
    }

    $files = New-Object System.Collections.Generic.List[string]
    foreach ($line in Get-Content $StatusPath) {
        $trimmed = $line.Trim()
        if ($trimmed -match '^[A-Z\?]{1,2}\s+(.+)$') {
            $pathValue = $Matches[1].Trim()
            if ($pathValue -like "* -> *") {
                $pathValue = ($pathValue -split ' -> ')[-1].Trim()
            }
            if (-not [string]::IsNullOrWhiteSpace($pathValue)) {
                $files.Add(($pathValue -replace '\\', '/'))
            }
        }
    }

    return $files.ToArray()
}

function Test-PathInScope {
    param(
        [string]$ChangedPath,
        [string[]]$ScopePatterns
    )

    if ($ScopePatterns.Count -eq 0) {
        return $false
    }

    foreach ($rawPattern in $ScopePatterns) {
        $pattern = ($rawPattern -replace '\\', '/').Trim()
        if ([string]::IsNullOrWhiteSpace($pattern)) {
            continue
        }

        if ($pattern.Contains('*') -or $pattern.Contains('?') -or $pattern.Contains('[')) {
            if ([System.Management.Automation.WildcardPattern]::Get($pattern, 'IgnoreCase').IsMatch($ChangedPath)) {
                return $true
            }
            continue
        }

        $normalized = $pattern.TrimEnd('/')
        if ($ChangedPath.Equals($normalized, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }

        if ($ChangedPath.StartsWith("$normalized/", [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    return $false
}

$taskDir = Join-Path (Get-AbsolutePath $DelegationRoot) $TaskId
$briefPath = Join-Path $taskDir "brief.md"
$reviewPath = Join-Path $taskDir "review-notes.md"
$reviewResultPath = Join-Path $taskDir "review-result.json"
$resultPath = Join-Path $taskDir "result.json"
$preflightPath = Join-Path $taskDir "preflight.json"
$statusPath = Join-Path $taskDir "git.status.txt"
$responsePath = Join-Path $taskDir "claude.response.md"

if (-not (Test-Path $briefPath)) { throw "Brief file not found: $briefPath" }
if (-not (Test-Path $resultPath)) { throw "Result file not found: $resultPath" }
if (-not (Test-Path $preflightPath)) { throw "Preflight file not found: $preflightPath" }

$briefText = Get-Content $briefPath -Raw
$result = Get-Content $resultPath -Raw | ConvertFrom-Json
$preflight = Get-Content $preflightPath -Raw | ConvertFrom-Json
$changedFiles = @(Get-ChangedFiles -StatusPath $statusPath)
$relevantFiles = @(Get-BriefSectionLines -Text $briefText -Header "Relevant files")
$doneWhenItems = @(Get-BriefSectionLines -Text $briefText -Header "Done when")
$issues = New-Object System.Collections.Generic.List[string]
$recommendedCorrections = New-Object System.Collections.Generic.List[string]
$scopeNotes = New-Object System.Collections.Generic.List[string]
$validationNotes = New-Object System.Collections.Generic.List[string]
$riskNotes = New-Object System.Collections.Generic.List[string]

if ($preflight.status -ne "PASS") {
    $issues.Add("Remote preflight is not green (`$($preflight.status)`).")
    $recommendedCorrections.Add("Fix the remote environment issue recorded in preflight.json before re-running the task.")
}

if ($result.worker_status -ne "SUCCESS") {
    $issues.Add("Worker status is `$($result.worker_status)` instead of `SUCCESS`.")
    $recommendedCorrections.Add("Address the worker failure and return a complete `SUCCESS` contract.")
}

if (-not $result.contract_complete) {
    $issues.Add("Worker output is missing required contract sections.")
    $recommendedCorrections.Add("Return the full result contract with RESULT, SUMMARY, CHANGED_FILES, TESTS_RUN, RISKS, BLOCKERS, and NEXT_ACTIONS.")
}

if (-not $result.diff_present) {
    $issues.Add("No real diff was detected for this run.")
    $recommendedCorrections.Add("Make the required code changes inside the assigned worktree and ensure git status/diff show the edits.")
}

if (-not $result.validation_reported) {
    $issues.Add("Validation was not reported with a meaningful TESTS_RUN value.")
    $recommendedCorrections.Add("Run the declared validation or state a concrete blocker in TESTS_RUN.")
}

if ($null -ne $result.timed_out -and $result.timed_out) {
    $issues.Add("The Claude run hit the overall timeout.")
    $recommendedCorrections.Add("Reduce the task scope or break it into smaller passes so the worker can finish within the timeout.")
}

if ($null -ne $result.idle_timed_out -and $result.idle_timed_out) {
    $issues.Add("The Claude run was terminated for idle output.")
    $recommendedCorrections.Add("Give Claude a smaller, clearer patch to avoid idle spinning and require concrete progress.")
}

if ($doneWhenItems.Count -eq 0) {
    $issues.Add("The brief is missing concrete Done when items.")
    $recommendedCorrections.Add("Fill in the Done when section with explicit completion checks before rerunning.")
}

if ($relevantFiles.Count -eq 0 -and $changedFiles.Count -gt 0) {
    $issues.Add("Changed files cannot be scope-checked because Relevant files is empty.")
    $recommendedCorrections.Add("Declare the allowed file surface in Relevant files so scope drift can be checked.")
}

$outOfScopeFiles = @()
if ($changedFiles.Count -gt 0 -and $relevantFiles.Count -gt 0) {
    $outOfScopeFiles = @($changedFiles | Where-Object { -not (Test-PathInScope -ChangedPath $_ -ScopePatterns $relevantFiles) })
    foreach ($pathValue in $outOfScopeFiles) {
        $issues.Add("Changed file is outside Relevant files: $pathValue")
    }
    if ($outOfScopeFiles.Count -gt 0) {
        $recommendedCorrections.Add("Revert or avoid out-of-scope files and keep edits inside the declared Relevant files list.")
    }
}

if ($changedFiles.Count -eq 0) {
    $scopeNotes.Add("No changed files were recorded.")
} else {
    $scopeNotes.Add("Changed files: $($changedFiles -join ', ')")
}

if ($outOfScopeFiles.Count -eq 0 -and $changedFiles.Count -gt 0) {
    $scopeNotes.Add("All detected changed files stayed within the declared Relevant files list.")
}

if ($result.validation_reported) {
    $validationNotes.Add("Worker reported TESTS_RUN with a meaningful value.")
} else {
    $validationNotes.Add("Worker did not report a meaningful TESTS_RUN value.")
}

if (Test-Path $responsePath) {
    $responseText = Get-Content $responsePath -Raw
    if ($responseText -match 'BLOCKERS:\s*(.+)') {
        $riskNotes.Add("BLOCKERS: $($Matches[1].Trim())")
    }
    if ($responseText -match 'RISKS:\s*(.+)') {
        $riskNotes.Add("RISKS: $($Matches[1].Trim())")
    }
}

if ($riskNotes.Count -eq 0) {
    $riskNotes.Add("No extra risk notes were extracted from the worker response.")
}

$reviewResult = if ($issues.Count -eq 0) { "PASS" } else { "NEEDS_FIX" }
$correctionLines = if ($recommendedCorrections.Count -gt 0) { $recommendedCorrections | Select-Object -Unique } else { @("No additional fixes required.") }

$reviewContent = @(
    "# Review Notes",
    "",
    "REVIEW_RESULT: $reviewResult",
    "",
    "## Scope check",
    ""
) + ($scopeNotes | ForEach-Object { "- $_" }) + @(
    "",
    "## Validation check",
    ""
) + ($validationNotes | ForEach-Object { "- $_" }) + @(
    "",
    "## Risk check",
    ""
) + ($riskNotes | ForEach-Object { "- $_" }) + @(
    "",
    "## Recommended corrections",
    ""
) + ($correctionLines | ForEach-Object { "- $_" })

Set-Content -Path $reviewPath -Value ($reviewContent -join "`r`n") -Encoding utf8

[ordered]@{
    task_id = $TaskId
    attempt = $Attempt
    review_result = $reviewResult
    changed_files = $changedFiles
    relevant_files = $relevantFiles
    done_when = $doneWhenItems
    issues = $issues
    recommended_corrections = $correctionLines
    minimal_fix_list = $correctionLines
    checked_at = (Get-Date).ToString("o")
    review_file = $reviewPath
} | ConvertTo-Json -Depth 5 | Set-Content -Path $reviewResultPath -Encoding utf8

Write-Host "Review result for ${TaskId}: $reviewResult"
if ($reviewResult -ne "PASS") {
    exit 2
}
