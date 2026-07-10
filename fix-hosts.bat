@echo off
findstr /v "pigeo.asuscomm.com" %SystemRoot%\System32\drivers\etc\hosts > %temp%\hosts.tmp
move /y %temp%\hosts.tmp %SystemRoot%\System32\drivers\etc\hosts >nul
echo 127.0.0.1 pigeon.asuscomm.com >> %SystemRoot%\System32\drivers\etc\hosts
echo Added 127.0.0.1 pigeon.asuscomm.com
pause
