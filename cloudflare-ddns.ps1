# Cloudflare DDNS — обновляет A-запись при смене IP
# Запускать каждые 5 минут через планировщик Windows

$apiToken = "ТВОЙ_API_TOKEN"   # Создать в Cloudflare: My Profile → API Tokens → Create Token (Zone:DNS:Edit)
$zoneName = "твой-домен.com"  # Например pwp-warfare.com
$recordName = "api.твой-домен.com"  # api.pwp-warfare.com

# ==== Дальше ничего не трогать ====

$currentIp = (Invoke-WebRequest -Uri "https://api.ipify.org" -UseBasicParsing).Content.Trim()
$headers = @{ "Authorization" = "Bearer $apiToken"; "Content-Type" = "application/json" }

# Получаем Zone ID
$zones = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones?name=$zoneName" -Headers $headers
$zoneId = $zones.result[0].id

# Получаем DNS запись
$records = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records?name=$recordName&type=A" -Headers $headers
$recordId = $records.result[0].id
$oldIp = $records.result[0].content

if ($currentIp -ne $oldIp) {
    $body = @{ type = "A"; name = $recordName; content = $currentIp; ttl = 120; proxied = $true } | ConvertTo-Json
    Invoke-RestMethod -Method PUT -Uri "https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$recordId" -Headers $headers -Body $body
    Write-Host "IP updated: $oldIp -> $currentIp"
} else {
    Write-Host "IP unchanged: $currentIp"
}
