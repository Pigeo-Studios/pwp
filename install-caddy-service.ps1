$caddy = "C:\Users\maska\OneDrive\Desktop\PWP\caddy.exe"
$config = "C:\Users\maska\OneDrive\Desktop\PWP\Caddyfile"
$binPath = "`"$caddy`" run --config `"$config`""

# Remove existing service if present
sc.exe delete PWPCaddy 2>$null

# Create service
New-Service -Name "PWPCaddy" `
    -BinaryPathName $binPath `
    -DisplayName "PWP Caddy HTTPS Proxy" `
    -Description "Reverse proxy for PWP core-service with Let's Encrypt" `
    -StartupType Automatic

# Start service
Start-Service -Name PWPCaddy

Write-Host "Caddy service installed and started!"
