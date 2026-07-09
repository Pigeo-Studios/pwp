using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using PWPLuncher.Models;

namespace PWPLuncher.Services
{
    public static class DownloadService
    {
        public static string ApiUrl { get; set; } = "http://pigeo.asuscomm.com:8080";
        private static readonly string GitHubRaw = "https://raw.githubusercontent.com/Pigeo-Studios/Pwpfiles/main";
        private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(30) };

        public static async Task<List<ManifestEntry>> FetchManifest(string category = null)
        {
            try
            {
                string url = $"{ApiUrl}/api/v1/launcher/manifest";
                if (!string.IsNullOrEmpty(category))
                    url += "?category=" + Uri.EscapeDataString(category);

                var req = new HttpRequestMessage(HttpMethod.Get, url);
                req.Headers.Add("User-Agent", "PWP Launcher/1.0");
                var uri = new Uri(url);
                req.Headers.Add("X-PWP-Sign", LauncherSigner.Sign(uri.AbsolutePath));
                var resp = await Client.SendAsync(req);
                resp.EnsureSuccessStatusCode();
                var json = await resp.Content.ReadAsStringAsync();
                var wrapper = JsonSerializer.Deserialize<ManifestWrapper>(json,
                    new System.Text.Json.JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                return wrapper?.Success == true && wrapper.Data?.Files != null ? wrapper.Data.Files : new List<ManifestEntry>();
            }
            catch (Exception ex)
            {
                LogService.Write("MANIFEST", $"Failed: {ex.Message}");
                return new List<ManifestEntry>();
            }
        }

        public static async Task<bool> DownloadFiles(List<ManifestEntry> files, string basePath,
            IProgress<(int current, int total, string file, double speedKbps)> progress = null)
        {
            int total = files.Count, current = 0;
            bool allSuccess = true;
            long overallBytes = 0;
            var stopwatch = new System.Diagnostics.Stopwatch();
            stopwatch.Start();

            foreach (var entry in files)
            {
                current++;
                progress?.Report((current, total, entry.Path, 0));

                try
                {
                    string fullPath = Path.Combine(basePath, entry.Path);
                    string dir = Path.GetDirectoryName(fullPath);
                    Directory.CreateDirectory(dir);
                    string githubUrl = $"{GitHubRaw}/{entry.Path.Replace('\\', '/')}";

                    var fileTimer = System.Diagnostics.Stopwatch.StartNew();
                    using var wc = new System.Net.WebClient();
                    wc.Headers["User-Agent"] = "PWP Launcher";
                    await wc.DownloadFileTaskAsync(githubUrl, fullPath + ".tmp");
                    fileTimer.Stop();

                    long fileSize = new FileInfo(fullPath + ".tmp").Length;
                    overallBytes += fileSize;
                    double speedKbps = fileSize / (fileTimer.Elapsed.TotalSeconds * 1024);
                    if (fileTimer.Elapsed.TotalSeconds < 0.1) speedKbps = 0;

                    progress?.Report((current, total, entry.Path, speedKbps));

                    string hash = VerificationService.ComputeSha256(fullPath + ".tmp");
                    if (!string.Equals(hash, entry.Sha256, StringComparison.OrdinalIgnoreCase))
                    {
                        LogService.Write("DOWNLOAD", $"Hash mismatch: {entry.Path}");
                        File.Delete(fullPath + ".tmp");
                        allSuccess = false;
                        continue;
                    }

                    if (File.Exists(fullPath)) File.Delete(fullPath);
                    File.Move(fullPath + ".tmp", fullPath);
                }
                catch (Exception ex)
                {
                    LogService.Write("DOWNLOAD", $"Failed {entry.Path}: {ex.Message}");
                    allSuccess = false;
                }
            }
            return allSuccess;
        }
    }
}
