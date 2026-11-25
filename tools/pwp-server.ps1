<#
.SYNOPSIS
  PWP: собрать моды и разложить их по серверам одной командой (+ запустить лобби).

.DESCRIPTION
  1. -Build: gradlew build (моды + тесты; reobf-гарантия внутри сборки).
  2. Берёт 3 jar: pwp-core, pwp-game, sbwchunkload (по одному свежему из build/libs).
  3. В каждой папке -Server: старые jar PWP (в т.ч. 11 бывших модов до слияния 25.11.2026) ПЕРЕНОСИТ
     в mods_backup_<дата> (не удаляет), кладёт новые 3 jar.
  4. -Start: запускает run.bat первой папки (обычно лобби) в отдельном окне.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\pwp-server.ps1 -Build -Start `
    -Server "C:\srv\lobby","C:\srv\PWP-Server\match_template"
#>
param(
    [Parameter(Mandatory = $true)][string[]]$Server,
    [switch]$Build,
    [switch]$Start,
    [string]$JavaHome = "C:\Program Files\Java\jdk-17"
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot

if ($Build) {
    Write-Host "== Сборка (gradlew build) ..."
    Push-Location $repo
    try {
        & "$repo\gradlew.bat" build "-Dorg.gradle.java.home=$JavaHome" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Сборка упала (код $LASTEXITCODE)" }
    } finally { Pop-Location }
}

function Get-OneJar([string]$module, [string]$prefix) {
    $jars = @(Get-ChildItem -Path (Join-Path $repo "$module\build\libs") -Filter "$prefix-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*-sources.jar" })
    if ($jars.Count -ne 1) { throw "Нужен ровно один $prefix-*.jar в $module\build\libs (найдено $($jars.Count)). Запустите с -Build." }
    return $jars[0]
}
$new = @(
    (Get-OneJar "pwp-core" "pwp-core"),
    (Get-OneJar "pwp-game" "pwp-game"),
    (Get-OneJar "sbwchunkload" "sbwchunkload")
)

$py = Get-Command py -ErrorAction SilentlyContinue
if ($py) {
    Push-Location $repo
    try {
        & py -3 tools\check_reobf.py
        if ($LASTEXITCODE -ne 0) { throw "check_reobf: jar не reobf — в прод нельзя" }
    } finally { Pop-Location }
} else {
    Write-Warning "py не найден — пропускаю tools\check_reobf.py (reobf всё равно проверяется в сборке)"
}

# Все имена jar PWP: текущие 3 + бывшие моды до слияния (их нужно убрать, иначе дубли modid → краш)
$legacy = @("pwp-api-", "pwp-core-client-", "pwp-core-server-", "pwp-lobby-", "pwp-cosmetics-", "pwp-warfare-",
    "pwp-medicine-", "pwp-movement-", "pwp-limits-", "pwp-blast-protection-", "pwp-drone-",
    "pwp-core-", "pwp-game-", "sbwchunkload-")
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"

foreach ($dir in $Server) {
    $mods = Join-Path $dir "mods"
    if (-not (Test-Path $mods)) { throw "Нет папки mods: $mods" }
    $old = @(Get-ChildItem -Path $mods -Filter "*.jar" | Where-Object {
        $n = $_.Name; ($legacy | Where-Object { $n.StartsWith($_) }).Count -gt 0 })
    if ($old.Count -gt 0) {
        $backup = Join-Path $dir "mods_backup_$stamp"
        New-Item -ItemType Directory -Force -Path $backup | Out-Null
        foreach ($f in $old) { Move-Item -LiteralPath $f.FullName -Destination $backup }
        Write-Host "  $dir : $($old.Count) старых jar -> $backup"
    }
    foreach ($j in $new) { Copy-Item -LiteralPath $j.FullName -Destination $mods }
    Write-Host "  $dir : положены $($new.Name -join ', ')"
}

if ($Start) {
    $first = $Server[0]
    $run = Join-Path $first "run.bat"
    if (-not (Test-Path $run)) { throw "Нет $run" }
    Write-Host "== Запуск $run"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "run.bat" -WorkingDirectory $first
}
Write-Host "Готово."
