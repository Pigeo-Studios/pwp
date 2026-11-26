<#
.SYNOPSIS
  PWP: полный локальный сервер одной командой — MariaDB → core-service → лобби → матч-серверы.

.DESCRIPTION
  1. -Build: моды (gradlew build) + core-service (shadowJar).
  2. Кладёт 3 jar (pwp-core, pwp-game, sbwchunkload) в лобби, шаблон матча и (если задан -Client) в клиент;
     старые jar PWP переносятся в mods_backup_<дата> (tools\pwp-server.ps1).
  3. MariaDB: запуск, если порт 3306 свободен; БД pwp_core создаётся и наполняется из репо ОДИН раз
     (схема, скины, техника, киты). Ничего не удаляется.
  4. core-service: запуск, если :8080 не отвечает (таблицы и миграции создаёт сам).
     Пустая таблица faction_divisions — импорт стартовых дивизий maps\vehicles\divisions.sql.
  5. Лобби в своём окне. Дальше всё само: голосование → матч из шаблона (порты с 25566) → итоги → лобби.

  Без -Start выполняются только сборка и раскладка jar.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\pwp-local.ps1 -Build -Start
#>
param(
    [switch]$Build,
    [switch]$Start,
    [string]$Lobby = "$env:USERPROFILE\Desktop\PWP-TestLobby\lobby",
    [string]$Template = "",
    [string]$Local = "$env:USERPROFILE\pwp-local",
    [string]$Client = "$env:APPDATA\PrismLauncher\instances\1.20.1\minecraft",
    [string]$JavaHome = "C:\Program Files\Java\jdk-17"
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
if (-not $Template) { $Template = Join-Path (Split-Path -Parent $Lobby) "PWP-Server\match_template" }
$java = Join-Path $JavaHome "bin\java.exe"
$coreJar = Join-Path $repo "core-service\build\libs\core-service-1.0.0.jar"

function Test-Port([int]$port) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}
function Test-Core {
    try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 "http://127.0.0.1:8080/api/v1/network/status" | Out-Null; return $true }
    catch { return [bool]$_.Exception.Response }
}
function Wait-Until([scriptblock]$cond, [int]$seconds, [string]$what) {
    for ($i = 0; $i -lt $seconds; $i++) { if (& $cond) { return }; Start-Sleep -Seconds 1 }
    throw "$what не поднялся за $seconds с"
}

# ------------------------------------------------------------------ 1. сборка
if ($Build) {
    Push-Location $repo
    try {
        Write-Host "== [1/5] Сборка модов ..."
        & "$repo\gradlew.bat" build "-Dorg.gradle.java.home=$JavaHome" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Сборка модов упала" }
        Write-Host "== [1/5] Сборка core-service ..."
        Push-Location core-service
        try {
            & .\gradlew.bat shadowJar "-Dorg.gradle.java.home=$JavaHome" --console=plain
            if ($LASTEXITCODE -ne 0) { throw "Сборка core-service упала" }
        } finally { Pop-Location }
    } finally { Pop-Location }
}

# ------------------------------------------------------------------ 2. jar
$targets = @($Lobby, $Template)
if ($Client -and (Test-Path (Join-Path $Client "mods"))) { $targets += $Client }
Write-Host "== [2/5] Раскладка jar: $($targets -join ' | ')"
& (Join-Path $PSScriptRoot "pwp-server.ps1") -Server $targets

if (-not $Start) { Write-Host "Готово (без запуска: добавьте -Start)."; exit 0 }

foreach ($f in @("$Local\mariadb\bin\mariadbd.exe", "$Local\data\my.ini", "$Local\core-config.json", "$Local\db_password.txt", $coreJar, $java, "$Lobby\run.bat")) {
    if (-not (Test-Path $f)) { throw "Не найден файл: $f" }
}
# Дочерние процессы (лобби → матчи) берут java отсюда; cmd должен искать run.bat в текущей папке
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue

# ------------------------------------------------------------------ 3. MariaDB
$mariadb = "$Local\mariadb\bin\mariadb.exe"
$env:MYSQL_PWD = (Get-Content "$Local\db_password.txt" -TotalCount 1).Trim()
function Invoke-Sql([string]$sql) {
    $out = & $mariadb -h127.0.0.1 -P3306 -uroot --default-character-set=utf8mb4 -N -e $sql 2>$null
    return @{ ok = ($LASTEXITCODE -eq 0); out = $out }
}
function Import-SqlFile([string]$file) {
    # через cmd и «<»: пайп PowerShell перекодирует stdin и бьёт кириллицу (AGENTS.md)
    cmd /c "`"$mariadb`" -h127.0.0.1 -P3306 -uroot --default-character-set=utf8mb4 --force pwp_core < `"$file`""
}
try {
    if (Test-Port 3306) { Write-Host "== [3/5] MariaDB уже слушает :3306" }
    else {
        Write-Host "== [3/5] Запуск MariaDB ..."
        Start-Process -FilePath "$Local\mariadb\bin\mariadbd.exe" -ArgumentList "--defaults-file=`"$Local\data\my.ini`"", "--console" -WorkingDirectory $Local
        Wait-Until { Test-Port 3306 } 60 "MariaDB"
    }
    if (-not (Invoke-Sql "USE pwp_core").ok) {
        Write-Host "      БД pwp_core нет — создаю и импортирую из репо ..."
        Invoke-Sql "CREATE DATABASE pwp_core CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" | Out-Null
        foreach ($s in @("core-service\sql\schema.sql", "core-service\sql\migration_add_skins_cases.sql",
                "core-service\sql\migration_add_faction_vehicles.sql", "core-service\sql\migration_add_vehicle_category.sql",
                "core-service\sql\seed_skins.sql", "maps\kits\import.sql", "maps\vehicles\import.sql")) {
            Write-Host "        $s"; Import-SqlFile (Join-Path $repo $s)
        }
    }

    # ------------------------------------------------------------------ 4. core-service
    if (Test-Core) { Write-Host "== [4/5] core-service уже отвечает на :8080 (если он старый — перезапустите, чтобы взять новый jar)" }
    else {
        Write-Host "== [4/5] Запуск core-service ..."
        Start-Process -FilePath $java -ArgumentList "-Xmx512M", "-jar", "`"$coreJar`"", "`"$Local\core-config.json`"" -WorkingDirectory $Local
        Wait-Until { Test-Core } 90 "core-service (лог: $Local\logs)"
    }
    $div = Invoke-Sql "SELECT COUNT(*) FROM pwp_core.faction_divisions"
    if ($div.ok -and "$($div.out)".Trim() -eq "0") {
        Write-Host "      Дивизий нет — импортирую стартовые (maps\vehicles\divisions.sql)"
        Import-SqlFile (Join-Path $repo "maps\vehicles\divisions.sql")
    }
} finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

# ------------------------------------------------------------------ 5. лобби
if (Test-Port 25565) { Write-Host "== [5/5] Порт 25565 занят (лобби уже запущено или работает другой сервер) — лобби не запускаю" }
else {
    Write-Host "== [5/5] Запуск лобби на :25565 (Forge ~1-2 мин) ..."
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "run.bat", "nogui" -WorkingDirectory $Lobby
}
Write-Host ""
Write-Host "Готово. Клиент: 127.0.0.1:25565. Матчи лобби поднимает само (порты 25566+), нужен хотя бы 1 игрок."
