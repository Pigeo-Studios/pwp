@echo off
REM PWP Control: central GUI panel of the local server (DB, core, lobby, matches)
start "" powershell -NoProfile -STA -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0pwp-control.ps1" %*
