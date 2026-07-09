using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public static class ProcessWatcherService
    {
        private static readonly HashSet<string> BlacklistedProcesses = new(StringComparer.OrdinalIgnoreCase)
        {
            "cheatengine", "cheatengine-x86_64", "ce", "artmoney", "artmoney7",
            "artmoney8", "artmoney9", "wpe", "wepro", "wpeng",
            "ollydbg", "x64dbg", "x32dbg", "ida", "ida64", "idaq",
            "dnspy", "processhacker", "procmon", "procexp",
            "httpanalyzer", "fiddler", "charles", "burpsuite",
            "httpdebugger", "tcpview", "wireshark", "dumpcap",
            "injector", "extremeinjector", "processinjector",
            "reclass", "reclass.net", "ghidra", "radare2",
            "pixelbot", "minecraftbot", "baritone",
            "autoclicker", "mousekey", "ghostmouse",
        };

        private static CancellationTokenSource _cts;
        private static Process _minecraftProcess;
        private static Task _monitorTask;
        private static int _checkCounter;

        public static event Action<string> CheatDetected;
        public static event Action GameExited;

        public static bool IsRunning => _monitorTask != null && !_monitorTask.IsCompleted;

        public static bool PreLaunchCheck()
        {
            try
            {
                var processes = Process.GetProcesses();
                foreach (var p in processes)
                {
                    try
                    {
                        string name = p.ProcessName.ToLowerInvariant();
                        if (BlacklistedProcesses.Contains(name))
                        {
                            LogService.Write("ANTI-CHEAT", $"Blocked: {p.ProcessName} running");
                            CheatDetected?.Invoke($"Обнаружен запрещенный процесс: {p.ProcessName}\nПожалуйста, закройте его и попробуйте снова.");
                            return false;
                        }
                    }
                    catch { continue; }
                }
            }
            catch (Exception ex)
            {
                LogService.Write("ANTI-CHEAT", $"Pre-check error: {ex.Message}");
            }
            return true;
        }

        public static void StartMonitoring(Process minecraftProcess)
        {
            StopMonitoring();
            _minecraftProcess = minecraftProcess;
            _cts = new CancellationTokenSource();

            _monitorTask = Task.Run(async () =>
            {
                var token = _cts.Token;

                // Wait for game to exit
                try
                {
                    while (!token.IsCancellationRequested)
                    {
                        if (_minecraftProcess == null || _minecraftProcess.HasExited)
                        {
                            await Task.Delay(1000, token);
                            break;
                        }

                        // Periodic check for cheats (every ~30s = every 6th iteration)
                        _checkCounter++;
                        if (_checkCounter % 6 == 0)
                        {
                            var processes = Process.GetProcesses();
                            foreach (var p in processes)
                            {
                                if (token.IsCancellationRequested) return;
                                try
                                {
                                    string name = p.ProcessName.ToLowerInvariant();
                                    if (BlacklistedProcesses.Contains(name))
                                    {
                                        LogService.Write("ANTI-CHEAT", $"Cheat detected during game: {p.ProcessName}");
                                        try { _minecraftProcess.Kill(); } catch { }
                                        CheatDetected?.Invoke($"Обнаружен чит: {p.ProcessName}\nИгра остановлена.");
                                        return;
                                    }
                                }
                                catch { }
                            }
                        }

                        await Task.Delay(5000, token);
                    }
                }
                catch (TaskCanceledException) { }
                finally
                {
                    GameExited?.Invoke();
                }
            }, _cts.Token);
        }

        public static void StopMonitoring()
        {
            _cts?.Cancel();
            _minecraftProcess = null;
        }
    }
}
