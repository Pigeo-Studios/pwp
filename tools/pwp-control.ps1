<#
.SYNOPSIS
  PWP Control — центральная панель локального сервера: база, core-service, лобби и каждый матч отдельно.

.DESCRIPTION
  Таблица компонентов обновляется раз в 2 с (порт слушает? стадия матча из match_status.json).
  Для выбранной строки: Старт / Стоп / Убить процесс / Папка / Лог. Ниже — консоль лобби (RCON) с готовыми
  кнопками и общие действия: запустить всё, остановить всё, собрать и разложить, проверить контент.
  Каждый матч управляется и сам по себе: в игре — F6 (админ-панель этого сервера), здесь — своей строкой.

  Запуск: tools\pwp-control.cmd  (или powershell -STA -ExecutionPolicy Bypass -File tools\pwp-control.ps1)
#>
param(
    [string]$Lobby = "$env:USERPROFILE\Desktop\PWP-TestLobby\lobby",
    [string]$Local = "$env:USERPROFILE\pwp-local",
    [string]$JavaHome = "C:\Program Files\Java\jdk-17",
    [switch]$Status   # без окна: напечатать таблицу компонентов и выйти
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$repo = Split-Path -Parent $PSScriptRoot
$java = Join-Path $JavaHome "bin\java.exe"
$coreJar = Join-Path $repo "core-service\build\libs\core-service-1.0.0.jar"
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"
Remove-Item Env:NoDefaultCurrentDirectoryInExePath -ErrorAction SilentlyContinue

# ------------------------------------------------------------------ помощники
function Test-Port([int]$port) {
    $c = New-Object Net.Sockets.TcpClient
    try { $ok = $c.ConnectAsync("127.0.0.1", $port).Wait(250); return ($ok -and $c.Connected) }
    catch { return $false } finally { $c.Dispose() }
}
function Get-PortPid([int]$port) {
    $c = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return $c.OwningProcess } else { return $null }
}
function Read-Props([string]$file) {
    $cfg = @{}
    if (Test-Path $file) {
        foreach ($line in Get-Content $file) { if ($line -match '^([^#=]+)=(.*)$') { $cfg[$Matches[1].Trim()] = $Matches[2] } }
    }
    return $cfg
}
function Read-Exact($stream, [int]$n) {
    $buf = New-Object byte[] $n; $o = 0
    while ($o -lt $n) { $r = $stream.Read($buf, $o, $n - $o); if ($r -le 0) { throw "соединение закрыто" }; $o += $r }
    return , $buf
}
function Send-RconPacket($stream, [int]$id, [int]$type, [string]$body) {
    $p = [Text.Encoding]::UTF8.GetBytes($body)
    $buf = New-Object byte[] (14 + $p.Length)
    [BitConverter]::GetBytes([int](10 + $p.Length)).CopyTo($buf, 0)
    [BitConverter]::GetBytes($id).CopyTo($buf, 4)
    [BitConverter]::GetBytes($type).CopyTo($buf, 8)
    $p.CopyTo($buf, 12)
    $stream.Write($buf, 0, $buf.Length)
}
function Read-RconPacket($stream) {
    $len = [BitConverter]::ToInt32((Read-Exact $stream 4), 0)
    $data = Read-Exact $stream $len
    return @{ Id = [BitConverter]::ToInt32($data, 0); Body = [Text.Encoding]::UTF8.GetString($data, 8, [Math]::Max(0, $len - 10)) }
}
function Invoke-Rcon([string]$command) {
    $cfg = Read-Props (Join-Path $Lobby "server.properties")
    if ($cfg['enable-rcon'] -ne 'true') { return "RCON выключен в lobby\server.properties" }
    $client = New-Object Net.Sockets.TcpClient
    try {
        $client.ReceiveTimeout = 10000
        $client.Connect("127.0.0.1", [int]$cfg['rcon.port'])
        $s = $client.GetStream()
        Send-RconPacket $s 1 3 $cfg['rcon.password']
        if ((Read-RconPacket $s).Id -eq -1) { return "RCON: неверный пароль" }
        Send-RconPacket $s 2 2 $command
        $body = (Read-RconPacket $s).Body -replace ([string][char]0xA7 + '.'), ''
        if (-not $body) { $body = "(ок, без ответа)" }
        return $body
    } catch { return "RCON недоступен: $($_.Exception.Message)" } finally { $client.Dispose() }
}
function Start-Window([string]$title, [string]$dir, [string]$exe, [string[]]$arguments) {
    $argList = @("/c", "start", "`"$title`"", "/D", "`"$dir`"", "`"$exe`"") + $arguments
    Start-Process -FilePath "cmd.exe" -WorkingDirectory $dir -ArgumentList $argList
}

# ------------------------------------------------------------------ компоненты
function Get-MatchRows {
    $list = @()
    foreach ($d in (Get-ChildItem -Path $Lobby -Directory -Filter "match_*" -ErrorAction SilentlyContinue)) {
        $props = Read-Props (Join-Path $d.FullName "server.properties")
        $port = 0
        if ($props['server-port']) { $port = [int]$props['server-port'] }
        $stage = ""
        $st = Join-Path $d.FullName "match_status.json"
        if (Test-Path $st) {
            try {
                $j = Get-Content $st -Raw | ConvertFrom-Json
                $age = [int](([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - [long]$j.writtenAt) / 1000)
                $stage = "$($j.stage)"
                if ($j.error) { $stage += " · ошибка $($j.error)" }
                if ($age -gt 5) { $stage += " · обновлено $age с назад" }
            } catch { $stage = "match_status.json не читается" }
        }
        $list += [pscustomobject]@{ Id = "match:$($d.Name)"; Name = "Матч $($d.Name)"; Port = $port; Dir = $d.FullName; Stage = $stage }
    }
    return $list
}
function Get-Rows {
    $rows = @(
        [pscustomobject]@{ Id = "db"; Name = "База MariaDB"; Port = 3306; Dir = $Local; Stage = "" },
        [pscustomobject]@{ Id = "core"; Name = "core-service (API)"; Port = 8080; Dir = $Local; Stage = "" },
        [pscustomobject]@{ Id = "lobby"; Name = "Лобби"; Port = 25565; Dir = $Lobby; Stage = "" }
    )
    return $rows + @(Get-MatchRows)
}
function Start-Component($row) {
    if ($row.Port -gt 0 -and (Test-Port $row.Port)) { return "$($row.Name): уже работает" }
    switch -Wildcard ($row.Id) {
        "db" {
            Start-Window "PWP MariaDB :3306" $Local "$Local\mariadb\bin\mariadbd.exe" @("--defaults-file=`"$Local\data\my.ini`"", "--console")
        }
        "core" {
            if (-not (Test-Path $coreJar)) { return "Нет $coreJar — нажмите «Собрать и разложить»" }
            if (-not (Test-Port 3306)) { return "Сначала база: core без неё не стартует" }
            Start-Window "PWP core-service :8080" $Local $java @("-Xmx512M", "-jar", "`"$coreJar`"", "`"$Local\core-config.json`"")
        }
        "lobby" { Start-Window "PWP Lobby :25565" $Lobby "cmd.exe" @("/c", "run.bat", "nogui") }
        "match:*" { return "Матч поднимает лобби: «Начать голосование» или в игре F6 → «Лобби»" }
    }
    return "$($row.Name): запуск..."
}
function Stop-Component($row, [switch]$Force) {
    if (-not $Force) {
        switch -Wildcard ($row.Id) {
            "db" {
                $env:MYSQL_PWD = (Get-Content "$Local\db_password.txt" -TotalCount 1).Trim()
                try { & "$Local\mariadb\bin\mariadb-admin.exe" -h127.0.0.1 -P3306 -uroot shutdown 2>&1 | Out-Null }
                finally { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
                return "База: остановка"
            }
            "lobby" { return "Лобби: " + (Invoke-Rcon "stop") }
            "match:*" { return "Матч (через лобби): " + (Invoke-Rcon "pwp server stop") }
        }
    }
    $procId = Get-PortPid $row.Port
    if (-not $procId) { return "$($row.Name): не запущен" }
    Stop-Process -Id $procId -Force
    return "$($row.Name): процесс $procId завершён"
}

if ($Status) {
    foreach ($r in Get-Rows) { "{0,-22} {1,6}  {2,-10} {3}" -f $r.Name, $r.Port, $(if ($r.Port -gt 0 -and (Test-Port $r.Port)) { "работает" } else { "выключен" }), $r.Stage }
    exit 0
}

# ------------------------------------------------------------------ окно
$form = New-Object Windows.Forms.Form
$form.Text = "PWP Control — локальный сервер"
$form.ClientSize = New-Object Drawing.Size(964, 680)
$form.MinimumSize = New-Object Drawing.Size(980, 600)
$form.StartPosition = "CenterScreen"
$form.Font = New-Object Drawing.Font("Segoe UI", 9.5)
$form.BackColor = [Drawing.Color]::FromArgb(24, 26, 30)
$form.ForeColor = [Drawing.Color]::Gainsboro

function New-Button([string]$text, [int]$x, [int]$y, [int]$w, [scriptblock]$onClick) {
    $b = New-Object Windows.Forms.Button
    $b.Text = $text; $b.Location = New-Object Drawing.Point($x, $y); $b.Size = New-Object Drawing.Size($w, 32)
    $b.FlatStyle = "Flat"; $b.BackColor = [Drawing.Color]::FromArgb(44, 48, 56); $b.ForeColor = [Drawing.Color]::White
    $b.FlatAppearance.BorderColor = [Drawing.Color]::FromArgb(80, 88, 100)
    $b.Add_Click($onClick)
    $form.Controls.Add($b)
}

$grid = New-Object Windows.Forms.ListView
$grid.View = "Details"; $grid.FullRowSelect = $true; $grid.MultiSelect = $false; $grid.HideSelection = $false
$grid.Location = New-Object Drawing.Point(12, 12); $grid.Size = New-Object Drawing.Size(940, 220)
$grid.Anchor = "Top, Left, Right"
$grid.BackColor = [Drawing.Color]::FromArgb(32, 35, 40); $grid.ForeColor = [Drawing.Color]::Gainsboro
foreach ($c in @(@("Компонент", 200), @("Порт", 65), @("Состояние", 115), @("Стадия", 300), @("Папка", 255))) { [void]$grid.Columns.Add($c[0], $c[1]) }
$form.Controls.Add($grid)

$log = New-Object Windows.Forms.TextBox
$log.Multiline = $true; $log.ScrollBars = "Vertical"; $log.ReadOnly = $true
$log.Location = New-Object Drawing.Point(12, 376); $log.Size = New-Object Drawing.Size(940, 292)
$log.Anchor = "Top, Bottom, Left, Right"
$log.BackColor = [Drawing.Color]::FromArgb(16, 17, 20); $log.ForeColor = [Drawing.Color]::FromArgb(190, 220, 190)
$log.Font = New-Object Drawing.Font("Consolas", 9.5)
$form.Controls.Add($log)
function Write-Log([string]$text) { $log.AppendText("[$(Get-Date -Format HH:mm:ss)] $text`r`n") }

function Get-Selected {
    if ($grid.SelectedItems.Count -eq 0) { Write-Log "Выберите строку в таблице"; return $null }
    return $grid.SelectedItems[0].Tag
}
function Update-Grid {
    $sel = $null
    if ($grid.SelectedItems.Count) { $sel = $grid.SelectedItems[0].Tag.Id }
    $grid.BeginUpdate(); $grid.Items.Clear()
    foreach ($r in Get-Rows) {
        $up = $r.Port -gt 0 -and (Test-Port $r.Port)
        $it = New-Object Windows.Forms.ListViewItem($r.Name)
        [void]$it.SubItems.Add("$($r.Port)")
        if ($up) { [void]$it.SubItems.Add("● работает") } else { [void]$it.SubItems.Add("○ выключен") }
        [void]$it.SubItems.Add($r.Stage)
        [void]$it.SubItems.Add($r.Dir)
        if ($up) { $it.ForeColor = [Drawing.Color]::FromArgb(120, 220, 140) } else { $it.ForeColor = [Drawing.Color]::FromArgb(150, 150, 150) }
        $it.Tag = $r
        [void]$grid.Items.Add($it)
        if ($r.Id -eq $sel) { $it.Selected = $true }
    }
    $grid.EndUpdate()
}
function Start-Script([string[]]$scriptArgs) {
    $a = @("-NoExit", "-ExecutionPolicy", "Bypass", "-File", "`"$repo\tools\pwp-local.ps1`"", "-Lobby", "`"$Lobby`"", "-Local", "`"$Local`"") + $scriptArgs
    Start-Process powershell.exe -ArgumentList $a
}

# ряд 1: действия над выбранной строкой
$y = 242
New-Button "▶ Старт" 12 $y 110 { $r = Get-Selected; if ($r) { Write-Log (Start-Component $r) } }
New-Button "■ Стоп" 128 $y 110 { $r = Get-Selected; if ($r) { Write-Log (Stop-Component $r) } }
New-Button "Убить процесс" 244 $y 130 {
    $r = Get-Selected
    if ($r -and [Windows.Forms.MessageBox]::Show("Принудительно завершить «$($r.Name)»? Несохранённое пропадёт.", "PWP", "YesNo", "Warning") -eq "Yes") {
        Write-Log (Stop-Component $r -Force)
    }
}
New-Button "Папка" 380 $y 90 { $r = Get-Selected; if ($r) { Invoke-Item $r.Dir } }
New-Button "Лог" 476 $y 90 {
    $r = Get-Selected
    if (-not $r) { return }
    $f = $null
    if ($r.Id -eq "core") { $f = Get-ChildItem "$Local\logs" -Filter *.log -ErrorAction SilentlyContinue | Sort-Object LastWriteTime | Select-Object -Last 1 | ForEach-Object FullName }
    elseif ($r.Id -ne "db") { $f = Join-Path $r.Dir "logs\latest.log" }
    if ($f -and (Test-Path $f)) { Start-Process notepad.exe $f } else { Write-Log "$($r.Name): лога нет" }
}

# ряд 2: всё сразу
$y = 284
New-Button "Запустить всё" 12 $y 150 {
    Write-Log "Запуск: база → core → лобби (ход — в отдельном окне)"
    Start-Script @("-Start")
}
New-Button "Остановить всё" 168 $y 150 {
    if ([Windows.Forms.MessageBox]::Show("Остановить матчи, лобби, core и базу?", "PWP", "YesNo", "Question") -ne "Yes") { return }
    $rows = Get-Rows
    foreach ($m in @($rows | Where-Object { $_.Id -like "match:*" -and (Test-Port $_.Port) })) { Write-Log (Stop-Component $m -Force) }
    foreach ($id in @("lobby", "core", "db")) {
        $r = $rows | Where-Object Id -eq $id
        if (Test-Port $r.Port) { Write-Log (Stop-Component $r) }
    }
}
New-Button "Собрать и разложить" 324 $y 170 {
    Write-Log "Сборка модов и core + раскладка jar — в отдельном окне (~10 мин). Серверы перед этим лучше остановить."
    Start-Script @("-Build")
}
New-Button "Проверить контент" 500 $y 170 {
    Write-Log "Проверка китов, техники, дивизий, карт и переводов..."
    $out = & py -3 "$repo\tools\pwp_doctor.py" 2>&1 | Out-String
    Write-Log ("`r`n" + ($out -replace "`r?`n", "`r`n"))
}
New-Button "README" 676 $y 110 { Start-Process notepad.exe "$repo\README.md" }

# ряд 3: консоль лобби
$y = 328
$lbl = New-Object Windows.Forms.Label
$lbl.Text = "Лобби:"; $lbl.Location = New-Object Drawing.Point(12, ($y + 7)); $lbl.AutoSize = $true
$form.Controls.Add($lbl)
New-Button "Статус матчей" 66 $y 120 { Write-Log "> pwp server status"; Write-Log (Invoke-Rcon "pwp server status") }
New-Button "Начать голосование" 192 $y 150 { Write-Log "> pwp server start"; Write-Log (Invoke-Rcon "pwp server start") }
New-Button "Остановить матч" 348 $y 130 { Write-Log "> pwp server stop"; Write-Log (Invoke-Rcon "pwp server stop") }
New-Button "Игроки" 484 $y 80 { Write-Log "> list"; Write-Log (Invoke-Rcon "list") }
$box = New-Object Windows.Forms.TextBox
$box.Location = New-Object Drawing.Point(572, ($y + 4)); $box.Size = New-Object Drawing.Size(290, 26)
$box.BackColor = [Drawing.Color]::FromArgb(32, 35, 40); $box.ForeColor = [Drawing.Color]::White
$form.Controls.Add($box)
$runBox = {
    $c = $box.Text.Trim().TrimStart('/')
    if ($c) { Write-Log "> $c"; Write-Log (Invoke-Rcon $c); $box.Clear() }
}
New-Button "Отправить" 868 $y 84 $runBox
$box.Add_KeyDown({ if ($_.KeyCode -eq "Enter") { & $runBox; $_.SuppressKeyPress = $true } })

$timer = New-Object Windows.Forms.Timer
$timer.Interval = 2000
$timer.Add_Tick({ Update-Grid })
$form.Add_Shown({ Update-Grid; $timer.Start(); Write-Log "Готово. Выберите строку и действие. Матчи создаёт лобби — они появятся в таблице сами." })
$form.Add_FormClosed({ $timer.Stop() })
[void]$form.ShowDialog()
