using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public static class ForgeInstallService
    {
        private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(60) };
        private static readonly HttpClient AssetClient = new() { Timeout = TimeSpan.FromSeconds(30) };
        private const string ForgeBuild = "47.4.20";
        private const string ForgeVersion = "1.20.1-forge-" + ForgeBuild;
        private const string InstallerUrl = "https://raw.githubusercontent.com/Pigeo-Studios/Pwpfiles/main/forge-installer.jar";

        public static async Task<bool> EnsureForgeInstalled(string gameDir)
        {
            string mcDir = Path.Combine(gameDir, ".minecraft");
            string libDir = Path.Combine(mcDir, "libraries");
            string versionDir = Path.Combine(mcDir, "versions", ForgeVersion);
            string versionJson = Path.Combine(versionDir, ForgeVersion + ".json");
            string mcVersionDir = Path.Combine(mcDir, "versions", "1.20.1");
            string clientJar = Path.Combine(mcVersionDir, "1.20.1.jar");
            string forgeJson = Path.Combine(mcVersionDir, "1.20.1.json");

            // Check if already fully installed
            if (File.Exists(versionJson) && File.Exists(clientJar) && File.Exists(forgeJson))
            {
                LogService.Write("INSTALL", "Minecraft 1.20.1 + Forge already installed");
                return true;
            }

            LogService.Write("INSTALL", "Installing Minecraft + Forge...");
            Directory.CreateDirectory(mcDir);

            // ── 1. Run Forge installer ──
            if (!File.Exists(versionJson))
            {
                LogService.Write("INSTALL", "Step 1/4: Running Forge installer...");

                // Create launcher_profiles.json (Forge installer requires it)
                string launcherProfiles = Path.Combine(mcDir, "launcher_profiles.json");
                if (!File.Exists(launcherProfiles))
                    File.WriteAllText(launcherProfiles, "{\"profiles\":{},\"selectedProfile\":\"(Default)\"}");

                // Download installer
                string installerPath = Path.Combine(Path.GetTempPath(), "forge-installer.jar");
                using (var wc = new System.Net.WebClient())
                {
                    wc.Headers["User-Agent"] = "PWP Launcher";
                    await wc.DownloadFileTaskAsync(InstallerUrl, installerPath);
                }

                // Run installer
                string javaPath = JavaService.JavaPath;
                if (string.IsNullOrEmpty(javaPath)) return false;

                var psi = new ProcessStartInfo(javaPath,
                    $"-jar \"{installerPath}\" --installClient \"{mcDir}\"")
                {
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };
                psi.EnvironmentVariables.Remove("JAVA_TOOL_OPTIONS");

                var proc = Process.Start(psi);
                string output = await proc.StandardOutput.ReadToEndAsync();
                string error = await proc.StandardError.ReadToEndAsync();
                proc.WaitForExit(120000);

                try { File.Delete(installerPath); } catch { }

                if (proc.ExitCode != 0)
                {
                    LogService.Write("INSTALL", $"Forge installer failed: {error}");
                    return false;
                }
                LogService.Write("INSTALL", "Forge installer done");
            }

            // ── 2. Download Mojang version manifest ──
            if (!File.Exists(forgeJson))
            {
                LogService.Write("INSTALL", "Step 2/4: Fetching Minecraft version manifest...");
                try
                {
                    Directory.CreateDirectory(mcVersionDir);
                    string manifestJson = await Client.GetStringAsync("https://launchermeta.mojang.com/mc/game/version_manifest.json");
                    var manifest = JsonDocument.Parse(manifestJson);
                    string versionUrl = null;
                    foreach (var v in manifest.RootElement.GetProperty("versions").EnumerateArray())
                    {
                        if (v.GetProperty("id").GetString() == "1.20.1")
                        { versionUrl = v.GetProperty("url").GetString(); break; }
                    }
                    if (versionUrl != null)
                    {
                        var vJson = await Client.GetStringAsync(versionUrl);
                        File.WriteAllText(forgeJson, vJson);
                    }
                }
                catch (Exception ex)
                {
                    LogService.Write("INSTALL", $"Manifest failed: {ex.Message}");
                }
            }

            // ── 3. Download ALL libraries from Mojang version.json ──
            if (File.Exists(forgeJson))
            {
                LogService.Write("INSTALL", "Step 3/4: Downloading Minecraft libraries from Mojang...");
                try
                {
                    var doc = JsonDocument.Parse(File.ReadAllText(forgeJson));
                    var root = doc.RootElement;
                    int total = 0, downloaded = 0, errors = 0;

                    // Download libraries
                    foreach (var lib in root.GetProperty("libraries").EnumerateArray())
                    {
                        total++;
                        try
                        {
                            if (lib.TryGetProperty("rules", out var rules))
                            {
                                bool allow = false;
                                foreach (var rule in rules.EnumerateArray())
                                {
                                    string action = rule.GetProperty("action").GetString();
                                    if (rule.TryGetProperty("os", out var os))
                                    {
                                        if (os.GetProperty("name").GetString() == "windows")
                                            allow = action == "allow";
                                        else if (action == "disallow")
                                            allow = false;
                                    }
                                    else { allow = action == "allow"; }
                                }
                                if (!allow) continue;
                            }

                            if (lib.TryGetProperty("downloads", out var dl))
                            {
                                var artifact = dl.GetProperty("artifact");
                                string path = artifact.GetProperty("path").GetString();
                                string url = artifact.GetProperty("url").GetString();
                                string localPath = Path.Combine(libDir, path);

                                if (!File.Exists(localPath))
                                {
                                    Directory.CreateDirectory(Path.GetDirectoryName(localPath));
                                    try
                                    {
                                        var bytes = await Client.GetByteArrayAsync(url);
                                        File.WriteAllBytes(localPath, bytes);
                                        downloaded++;
                                    }
                                    catch { errors++; }
                                }

                                // Natives (classifiers)
                                if (dl.TryGetProperty("classifiers", out var classifiers))
                                {
                                    foreach (var c in classifiers.EnumerateObject())
                                    {
                                        if (c.Value.TryGetProperty("path", out var np) &&
                                            c.Value.TryGetProperty("url", out var nu))
                                        {
                                            string nPath = np.GetString();
                                            string nLocal = Path.Combine(libDir, nPath);
                                            if (!File.Exists(nLocal))
                                            {
                                                try
                                                {
                                                    Directory.CreateDirectory(Path.GetDirectoryName(nLocal));
                                                    var nBytes = await Client.GetByteArrayAsync(nu.GetString());
                                                    File.WriteAllBytes(nLocal, nBytes);
                                                }
                                                catch { }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        catch { errors++; }
                    }

                    // Download client jar
                    try
                    {
                        var dlInfo = root.GetProperty("downloads").GetProperty("client");
                        string clientUrl = dlInfo.GetProperty("url").GetString();
                        if (!File.Exists(clientJar))
                        {
                            var clientBytes = await Client.GetByteArrayAsync(clientUrl);
                            File.WriteAllBytes(clientJar, clientBytes);
                        }
                    }
                    catch (Exception ex)
                    {
                        LogService.Write("INSTALL", $"Client jar failed: {ex.Message}");
                    }

                    // Download asset index + all asset objects
                    try
                    {
                        if (root.TryGetProperty("assetIndex", out var assetIdx))
                        {
                            string idxUrl = assetIdx.GetProperty("url").GetString();
                            string idxId = assetIdx.GetProperty("id").GetString();
                            string idxDir = Path.Combine(mcDir, "assets", "indexes");
                            string idxPath = Path.Combine(idxDir, idxId + ".json");
                            string objDir = Path.Combine(mcDir, "assets", "objects");
                            Directory.CreateDirectory(idxDir);
                            Directory.CreateDirectory(objDir);

                            if (!File.Exists(idxPath))
                            {
                                var idxBytes = await Client.GetByteArrayAsync(idxUrl);
                                File.WriteAllBytes(idxPath, idxBytes);
                                LogService.Write("INSTALL", $"Asset index {idxId} downloaded");
                            }

                            // Download all asset objects
                            var idxDoc = JsonDocument.Parse(File.ReadAllBytes(idxPath));
                            var objects = idxDoc.RootElement.GetProperty("objects");
                            int totalObjects = 0;
                            int downloadedObjs = 0;
                            int errorsObjs = 0;

                            foreach (var obj in objects.EnumerateObject())
                            {
                                totalObjects++;
                                string hash = obj.Value.GetProperty("hash").GetString();
                                string subDir = hash.Substring(0, 2);
                                string destPath = Path.Combine(objDir, subDir, hash);
                                if (File.Exists(destPath)) continue;

                                try
                                {
                                    string url = $"https://resources.download.minecraft.net/{subDir}/{hash}";
                                    Directory.CreateDirectory(Path.GetDirectoryName(destPath));
                                    var data = await AssetClient.GetByteArrayAsync(url);
                                    File.WriteAllBytes(destPath, data);
                                    downloadedObjs++;
                                    if (downloadedObjs % 100 == 0)
                                        LogService.Write("INSTALL", $"Assets: {downloadedObjs}/{totalObjects}...");
                                }
                                catch { errorsObjs++; }
                            }

                            LogService.Write("INSTALL", $"Assets: {downloadedObjs} downloaded, {errorsObjs} errors, {totalObjects} total");
                        }
                    }
                    catch (Exception ex)
                    {
                        LogService.Write("INSTALL", $"Asset download failed: {ex.Message}");
                    }

                    LogService.Write("INSTALL", $"Libraries: {downloaded} downloaded, {errors} errors, {total} total");
                }
                catch (Exception ex)
                {
                    LogService.Write("INSTALL", $"Library download failed: {ex.Message}");
                }
            }

            // ── 4. Verify ──
            bool hasVersionJson = File.Exists(versionJson);
            bool hasClientJar = File.Exists(clientJar);
            bool hasForgeJson = File.Exists(forgeJson);

            LogService.Write("INSTALL", $"Step 4/4: Forge={hasVersionJson}, Client={hasClientJar}, Mojang={hasForgeJson}");

            if (hasVersionJson && hasClientJar)
            {
                int jarCount = 0;
                if (Directory.Exists(libDir))
                    jarCount = Directory.GetFiles(libDir, "*.jar", SearchOption.AllDirectories).Length;
                LogService.Write("INSTALL", $"Total libraries: {jarCount} jars");
                return true;
            }

            LogService.Write("INSTALL", "Install incomplete - missing files");
            return false;
        }
    }
}
