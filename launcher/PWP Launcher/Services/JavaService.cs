using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public static class JavaService
    {
        public static string JavaPath { get; private set; }

        public static async Task<bool> EnsureJava(string launcherPath)
        {
            string runtimeDir = Path.Combine(launcherPath, "runtime", "jdk-17");
            string javaExe = FindJava(runtimeDir);

            if (javaExe != null)
            {
                JavaPath = javaExe;
                LogService.Write("JAVA", $"Found Java at {javaExe}");
                return true;
            }

            // Need to download
            LogService.Write("JAVA", "Downloading Java 17...");
            try
            {
                Directory.CreateDirectory(runtimeDir);

                // Use Adoptium API to get download URL
                string apiUrl = "https://api.adoptium.net/v3/assets/latest/17/hotspot?os=windows&arch=x64&image_type=jdk";
                using var wc = new WebClient();
                string json = await wc.DownloadStringTaskAsync(apiUrl);
                var doc = System.Text.Json.JsonDocument.Parse(json);

                var binary = doc.RootElement[0].GetProperty("binary");
                var installer = binary.GetProperty("installer");
                string downloadUrl = installer.GetProperty("link").GetString();
                string checksum = installer.GetProperty("checksum").GetProperty("sha256").GetString();

                string tempZip = Path.Combine(Path.GetTempPath(), "pwp-java.zip");
                await wc.DownloadFileTaskAsync(new Uri(downloadUrl), tempZip);

                // Extract
                System.IO.Compression.ZipFile.ExtractToDirectory(tempZip, runtimeDir);

                // Move contents from extracted folder to runtimeDir
                var dirs = Directory.GetDirectories(runtimeDir);
                if (dirs.Length > 0)
                {
                    string extracted = dirs[0];
                    foreach (var dir in Directory.GetDirectories(extracted))
                    {
                        string target = Path.Combine(runtimeDir, Path.GetFileName(dir));
                        if (Directory.Exists(target)) Directory.Delete(target, true);
                        Directory.Move(dir, target);
                    }
                    foreach (var file in Directory.GetFiles(extracted))
                    {
                        string target = Path.Combine(runtimeDir, Path.GetFileName(file));
                        if (File.Exists(target)) File.Delete(target);
                        File.Move(file, target);
                    }
                    Directory.Delete(extracted, true);
                }

                File.Delete(tempZip);

                javaExe = FindJava(runtimeDir);
                if (javaExe != null)
                {
                    JavaPath = javaExe;
                    LogService.Write("JAVA", "Java 17 installed successfully");
                    return true;
                }
            }
            catch (Exception ex)
            {
                LogService.Write("JAVA", $"Failed to install Java: {ex.Message}");
            }

            return false;
        }

        private static string FindJava(string runtimeDir)
        {
            // Check various possible locations
            string[] candidates = {
                Path.Combine(runtimeDir, "bin", "java.exe"),
                Path.Combine(runtimeDir, "jdk-17", "bin", "java.exe"),
                Path.Combine(runtimeDir, "jdk-17.0.1", "bin", "java.exe"),
            };

            foreach (var path in candidates)
            {
                if (File.Exists(path))
                {
                    // Verify it works
                    try
                    {
                        var psi = new ProcessStartInfo(path, "-version")
                        {
                            RedirectStandardError = true,
                            UseShellExecute = false,
                            CreateNoWindow = true
                        };
                        using var proc = Process.Start(psi);
                        string ver = proc.StandardError.ReadToEnd();
                        proc.WaitForExit(3000);
                        if (ver.Contains("openjdk") || ver.Contains("17."))
                            return path;
                    }
                    catch { }
                }
            }

            // Check system PATH
            try
            {
                var psi = new ProcessStartInfo("java", "-version")
                {
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using var proc = Process.Start(psi);
                string ver = proc.StandardError.ReadToEnd();
                proc.WaitForExit(3000);
                if (ver.Contains("17.") || ver.Contains("openjdk"))
                {
                    // Find full path
                    var whichPsi = new ProcessStartInfo("where", "java")
                    {
                        RedirectStandardOutput = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };
                    using var whichProc = Process.Start(whichPsi);
                    string path = whichProc.StandardOutput.ReadLine();
                    if (!string.IsNullOrEmpty(path) && File.Exists(path))
                        return path;
                }
            }
            catch { }

            return null;
        }

        public static async Task<string> GetJavaVersion()
        {
            if (string.IsNullOrEmpty(JavaPath)) return "Not found";
            try
            {
                var psi = new ProcessStartInfo(JavaPath, "-version")
                {
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using var proc = Process.Start(psi);
                string ver = await proc.StandardError.ReadToEndAsync();
                proc.WaitForExit(3000);
                return ver.Trim().Replace("\n", " | ");
            }
            catch { return "Error"; }
        }
    }
}
