param(
    [int]$MinimumMajorVersion = 21
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

function Get-JavaMajorVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaHome
    )

    $javaExe = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path $javaExe)) {
        return $null
    }

    $version = $null
    $releaseFile = Join-Path $JavaHome 'release'
    if (Test-Path $releaseFile) {
        $versionEntry = Get-Content $releaseFile |
            Where-Object { $_ -like 'JAVA_VERSION=*' } |
            Select-Object -First 1
        if ($versionEntry) {
            $version = (($versionEntry -split '=', 2)[1]).Trim().Trim('"')
        }
    }

    if (-not $version) {
        $versionLine = cmd /c "`"$javaExe`" -version 2>&1" | Select-Object -First 1
        if ($versionLine) {
            $version = [regex]::Match($versionLine, '"([^"]+)"').Groups[1].Value
        }
    }

    if (-not $version) {
        return $null
    }

    $majorToken = if ($version.StartsWith('1.')) {
        ($version -split '\.')[1]
    } else {
        ($version -split '\.')[0]
    }

    if ($majorToken -notmatch '^\d+$') {
        return $null
    }

    return [int]$majorToken
}

$candidates = [System.Collections.Generic.List[string]]::new()

if ($env:JAVA_HOME) {
    $candidates.Add($env:JAVA_HOME)
}

$jetBrainsCandidates = @(
    'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3\jbr',
    'C:\Program Files\JetBrains\IntelliJ IDEA Ultimate 2024.3\jbr',
    'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition\jbr',
    'C:\Program Files\JetBrains\IntelliJ IDEA Ultimate\jbr'
)

if ($env:LOCALAPPDATA) {
    $jetBrainsCandidates += @(
        (Join-Path $env:LOCALAPPDATA 'Programs\IntelliJ IDEA Community\jbr'),
        (Join-Path $env:LOCALAPPDATA 'Programs\IntelliJ IDEA Ultimate\jbr')
    )
}

$jetBrainsCandidates | Where-Object { $_ } | ForEach-Object { $candidates.Add($_) }

$jdkRoots = @(
    (Join-Path $env:USERPROFILE '.jdks'),
    'C:\Program Files\Java',
    'C:\Program Files\Eclipse Adoptium',
    'C:\Program Files\Microsoft'
)

foreach ($root in $jdkRoots | Where-Object { $_ -and (Test-Path $_) }) {
    Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { $candidates.Add($_.FullName) }
}

foreach ($candidate in $candidates | Where-Object { $_ } | Select-Object -Unique) {
    $major = Get-JavaMajorVersion -JavaHome $candidate
    if ($null -ne $major -and $major -ge $MinimumMajorVersion) {
        Write-Output $candidate
        exit 0
    }
}

Write-Error "Unable to locate Java $MinimumMajorVersion or newer."
exit 1
