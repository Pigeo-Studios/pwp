using System;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public class UpdateInfo
    {
        public bool UpdateAvailable { get; set; }
        public string Version { get; set; }
        public string DownloadUrl { get; set; }
        public string Sha256 { get; set; }
        public bool Mandatory { get; set; }
        public string Changelog { get; set; }
    }

    public static class UpdateService
    {
        public static string CurrentVersion { get; set; } = "1.0.0";
        private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(10) };

        public static async Task<UpdateInfo> CheckForUpdate()
        {
            try
            {
                string url = $"{AuthService.ApiUrl}/api/v1/launcher/version?current={CurrentVersion}&platform=windows";
                var uri = new Uri(url);
                var req = new HttpRequestMessage(HttpMethod.Get, uri);
                req.Headers.Add("User-Agent", "PWP Launcher/1.0");
                req.Headers.Add("X-PWP-Sign", LauncherSigner.Sign(uri.AbsolutePath));

                var resp = await Client.SendAsync(req);
                resp.EnsureSuccessStatusCode();
                var json = await resp.Content.ReadAsStringAsync();
                var doc = System.Text.Json.JsonDocument.Parse(json);

                if (!doc.RootElement.GetProperty("success").GetBoolean())
                    return new UpdateInfo { UpdateAvailable = false };

                var data = doc.RootElement.GetProperty("data");
                bool updateAvailable = data.GetProperty("update_available").GetBoolean();

                if (!updateAvailable)
                    return new UpdateInfo { UpdateAvailable = false };

                return new UpdateInfo
                {
                    UpdateAvailable = true,
                    Version = data.GetProperty("latest_version").GetString(),
                    DownloadUrl = data.GetProperty("download_url").GetString(),
                    Sha256 = data.GetProperty("sha256").GetString(),
                    Mandatory = data.GetProperty("mandatory").GetBoolean()
                };
            }
            catch (Exception ex)
            {
                LogService.Write("UPDATE", $"Check failed: {ex.Message}");
                return new UpdateInfo { UpdateAvailable = false };
            }
        }

        public static async Task<bool> ApplyUpdate(UpdateInfo info)
        {
            try
            {
                string tempDir = Path.Combine(Path.GetTempPath(), "pwp-update");
                Directory.CreateDirectory(tempDir);

                string tempFile = Path.Combine(tempDir, "PWP Launcher.exe");
                string updaterScript = Path.Combine(tempDir, "update.cmd");

                LogService.Write("UPDATE", $"Downloading v{info.Version}...");

                // Download with HMAC signing
                var uri = new Uri(info.DownloadUrl);
                var dlReq = new HttpRequestMessage(HttpMethod.Get, uri);
                dlReq.Headers.Add("User-Agent", "PWP Launcher/1.0");
                dlReq.Headers.Add("X-PWP-Sign", LauncherSigner.Sign(uri.AbsolutePath));

                var resp = await Client.SendAsync(dlReq);
                resp.EnsureSuccessStatusCode();
                using (var fs = new FileStream(tempFile, FileMode.Create, FileAccess.Write, FileShare.None))
                {
                    await resp.Content.CopyToAsync(fs);
                }

                // Verify checksum
                if (!string.IsNullOrEmpty(info.Sha256))
                {
                    string hash = VerificationService.ComputeSha256(tempFile);
                    if (!string.Equals(hash, info.Sha256, StringComparison.OrdinalIgnoreCase))
                    {
                        LogService.Write("UPDATE", "Checksum mismatch!");
                        return false;
                    }
                }

                string currentExe = Process.GetCurrentProcess().MainModule.FileName;

                string script =
                    $"@echo off\n" +
                    $"title PWP Launcher Updater\n" +
                    $"echo Updating PWP Launcher...\n" +
                    $"timeout /t 1 /nobreak >nul\n" +
                    $"del /f /q \"{currentExe}\"\n" +
                    $"copy /y \"{tempFile}\" \"{currentExe}\"\n" +
                    $"start \"\" \"{currentExe}\"\n" +
                    $"del /f /q \"%~f0\"\n";

                File.WriteAllText(updaterScript, script);

                LogService.Write("UPDATE", "Starting updater...");
                Process.Start(new ProcessStartInfo("cmd.exe", $"/c \"{updaterScript}\"")
                {
                    UseShellExecute = false,
                    CreateNoWindow = true
                });

                return true;
            }
            catch (Exception ex)
            {
                LogService.Write("UPDATE", $"Apply failed: {ex.Message}");
                return false;
            }
        }
    }
}
