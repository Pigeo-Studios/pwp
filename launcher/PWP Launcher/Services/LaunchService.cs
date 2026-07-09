using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public static class LaunchService
    {
        public static async Task<Process> LaunchMinecraft(int ramGb, string launcherPath, string serverToken)
        {
            try
            {
                string javaPath = JavaService.JavaPath;
                if (string.IsNullOrEmpty(javaPath) || !File.Exists(javaPath))
                    throw new Exception("Java not found");

                string mcDir = launcherPath;
                string libDir = Path.Combine(mcDir, "libraries");
                string assetsDir = Path.Combine(mcDir, "assets");
                string nativesDir = Path.Combine(mcDir, "versions", "1.20.1-forge-47.4.20", "natives");

                // 1. Load Forge version.json
                string forgeVersionName = "1.20.1-forge-47.4.20";
                string forgeVersionDir = Path.Combine(mcDir, "versions", forgeVersionName);
                string forgeVersionJson = Path.Combine(forgeVersionDir, forgeVersionName + ".json");
                if (!File.Exists(forgeVersionJson))
                    throw new Exception("Forge не установлен. Нажмите PLAY ещё раз.");

                var forgeDoc = JsonDocument.Parse(File.ReadAllText(forgeVersionJson));
                var forgeRoot = forgeDoc.RootElement;

                // 2. Load Vanilla version.json (inheritsFrom)
                string mainClass = forgeRoot.GetProperty("mainClass").GetString();

                string vanillaVersionName = forgeRoot.TryGetProperty("inheritsFrom", out var inherits)
                    ? inherits.GetString() : null;
                JsonDocument vanillaDoc = null;
                JsonElement vanillaRoot = default;

                if (vanillaVersionName != null)
                {
                    string vanillaVersionJson = Path.Combine(mcDir, "versions", vanillaVersionName, vanillaVersionName + ".json");
                    if (File.Exists(vanillaVersionJson))
                    {
                        vanillaDoc = JsonDocument.Parse(File.ReadAllText(vanillaVersionJson));
                        vanillaRoot = vanillaDoc.RootElement;
                    }
                }

                // 3. Build library list from version.jsons only
                var seenLibs = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
                var allLibs = new List<string>();

                // Vanilla libraries first (with rules filtering)
                if (vanillaDoc != null && vanillaRoot.TryGetProperty("libraries", out var vanLibs))
                {
                    foreach (var lib in vanLibs.EnumerateArray())
                    {
                        if (!LibMatchesCurrentOs(lib)) continue;
                        string path = LibPathFromJson(lib, libDir);
                        if (path != null && !seenLibs.Contains(path))
                        {
                            seenLibs.Add(path);
                            allLibs.Add(path);
                        }
                    }
                }

                // Forge libraries (override Vanilla by name dedup)
                if (forgeRoot.TryGetProperty("libraries", out var forgeLibs))
                {
                    foreach (var lib in forgeLibs.EnumerateArray())
                    {
                        if (!LibMatchesCurrentOs(lib)) continue;
                        string path = LibPathFromJson(lib, libDir);
                        if (path != null && !seenLibs.Contains(path))
                        {
                            seenLibs.Add(path);
                            allLibs.Add(path);
                        }
                    }
                }

                // 4. Add Forge client jar to classpath (NOT Vanilla — causes module conflict)
                string forgeClientJar = Path.Combine(forgeVersionDir, forgeVersionName + ".jar");
                if (File.Exists(forgeClientJar) && !seenLibs.Contains(forgeClientJar))
                {
                    seenLibs.Add(forgeClientJar);
                    allLibs.Add(forgeClientJar);
                }

                // 5. Extract natives if needed
                if (!Directory.Exists(nativesDir))
                {
                    Directory.CreateDirectory(nativesDir);
                    foreach (string dllFile in Directory.GetFiles(libDir, "*.dll", SearchOption.AllDirectories))
                    {
                        try { File.Copy(dllFile, Path.Combine(nativesDir, Path.GetFileName(dllFile)), true); } catch { }
                    }
                }

                // 6. Build classpath string
                string classpathStr = string.Join(";", allLibs).Replace("\\", "/");

                // 7. Get JVM arguments from Forge version.json
                var jvmArgs = new List<string>();
                jvmArgs.Add($"-Xmx{ramGb}G");
                jvmArgs.Add($"-Xms{Math.Max(ramGb / 2, 1)}G");

                // Add natives dir JVM args (always needed regardless of version.json)
                string nativesDirSlash = nativesDir.Replace("\\", "/");
                jvmArgs.Add($"-Djava.library.path={nativesDirSlash}");
                jvmArgs.Add($"-Djna.tmpdir={nativesDirSlash}");
                jvmArgs.Add($"-Dorg.lwjgl.system.SharedLibraryExtractPath={nativesDirSlash}");
                jvmArgs.Add($"-Dio.netty.native.workdir={nativesDirSlash}");

                if (forgeRoot.TryGetProperty("arguments", out var forgeArgs) &&
                    forgeArgs.TryGetProperty("jvm", out var jvmArr))
                {
                    foreach (var arg in jvmArr.EnumerateArray())
                    {
                        string str = arg.GetString();
                        if (str == "-cp" || str == "${classpath}") continue;
                        str = str.Replace("${library_directory}", libDir.Replace("\\", "/"))
                                 .Replace("${classpath_separator}", ";")
                                 .Replace("${version_name}", forgeVersionName)
                                 .Replace("${natives_directory}", nativesDirSlash)
                                 .Replace("${launcher_name}", "PWP Launcher")
                                 .Replace("${launcher_version}", "1.0");
                        jvmArgs.Add(str);
                    }
                }

                // 8. Build command: jvm args + -cp + mainClass + game args + auth args
                var cmdArgs = new List<string>();
                cmdArgs.AddRange(jvmArgs);
                cmdArgs.Add("-cp");
                cmdArgs.Add(classpathStr);
                cmdArgs.Add(mainClass);

                // 9. Game arguments from Forge version.json
                string username = AuthService.CurrentSession?.Login ?? "Player";
                string uuid = AuthService.CurrentSession?.UUID ?? "00000000-0000-0000-0000-000000000000";

                if (forgeRoot.TryGetProperty("arguments", out var fArgs2) &&
                    fArgs2.TryGetProperty("game", out var gameArr))
                {
                    foreach (var arg in gameArr.EnumerateArray())
                    {
                        string str = arg.GetString();
                        if (str == null) continue;
                        str = str.Replace("${auth_player_name}", username)
                                 .Replace("${auth_uuid}", uuid)
                                 .Replace("${auth_access_token}", serverToken)
                                 .Replace("${auth_session}", serverToken)
                                 .Replace("${user_type}", "mojang")
                                 .Replace("${version_name}", forgeVersionName)
                                 .Replace("${game_directory}", mcDir.Replace("\\", "/"))
                                 .Replace("${assets_root}", assetsDir.Replace("\\", "/"))
                                 .Replace("${assets_index_name}", "5")
                                 .Replace("${game_assets}", assetsDir.Replace("\\", "/"))
                                 .Replace("${natives_directory}", nativesDirSlash)
                                 .Replace("${launcher_name}", "PWP Launcher")
                                 .Replace("${launcher_version}", "1.0");
                        cmdArgs.Add(str);
                    }
                }

                // 10. Append standard auth args (Mojang launcher always adds these)
                cmdArgs.Add("--username");
                cmdArgs.Add(username);
                cmdArgs.Add("--version");
                cmdArgs.Add(forgeVersionName);
                cmdArgs.Add("--gameDir");
                cmdArgs.Add(mcDir.Replace("\\", "/"));
                cmdArgs.Add("--assetsDir");
                cmdArgs.Add(assetsDir.Replace("\\", "/"));
                cmdArgs.Add("--assetIndex");
                cmdArgs.Add("5");
                cmdArgs.Add("--uuid");
                cmdArgs.Add(uuid);
                cmdArgs.Add("--accessToken");
                cmdArgs.Add(serverToken);
                cmdArgs.Add("--userType");
                cmdArgs.Add("mojang");
                cmdArgs.Add("--versionType");
                cmdArgs.Add("release");

                // 11. Write argfile (each arg on its own line, forward slashes in paths)
                string argsFile = Path.Combine(Path.GetTempPath(), "pwp-launch-" + Guid.NewGuid().ToString("N").Substring(0, 8) + ".txt");
                string argsContent = string.Join("\n", cmdArgs.Select(a => a.Replace("\\", "/")));
                File.WriteAllText(argsFile, argsContent);

                LogService.Write("LAUNCH", $"Starting Minecraft...");
                LogService.Write("LAUNCH", $"CPU: {Environment.ProcessorCount} cores, RAM: {ramGb}G/{Math.Max(ramGb / 2, 1)}G");

                var psi = new ProcessStartInfo(javaPath, $"@{argsFile}")
                {
                    UseShellExecute = false,
                    WorkingDirectory = mcDir,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = false
                };
                psi.EnvironmentVariables.Remove("JAVA_TOOL_OPTIONS");

                var proc = Process.Start(psi);
                if (proc == null) throw new Exception("Failed to start process");

                LogService.Write("LAUNCH", $"PID: {proc.Id}");

                _ = Task.Run(() =>
                {
                    try
                    {
                        string line;
                        while ((line = proc.StandardOutput.ReadLine()) != null)
                        {
                            if (line.Contains("Exception") || line.Contains("ERROR") || line.Contains("Error"))
                                LogService.Write("MC", line);
                        }
                    }
                    catch { }
                });
                _ = Task.Run(() =>
                {
                    try
                    {
                        string line;
                        while ((line = proc.StandardError.ReadLine()) != null)
                            LogService.Write("MC-ERR", line);
                    }
                    catch { }
                });

                return proc;
            }
            catch (Exception ex)
            {
                LogService.Write("LAUNCH", $"Failed: {ex.Message}");
                throw;
            }
        }

        private static bool LibMatchesCurrentOs(JsonElement lib)
        {
            if (!lib.TryGetProperty("rules", out var rules))
                return true; // no rules → always included

            // Rules processing: last matching rule wins (action: allow/disallow)
            bool allowed = true;
            foreach (var rule in rules.EnumerateArray())
            {
                string action = rule.GetProperty("action").GetString();
                if (rule.TryGetProperty("os", out var os))
                {
                    bool osMatches = false;
                    if (os.TryGetProperty("name", out var osName))
                    {
                        string name = osName.GetString();
                        if (name == "windows" || name == "win" || name == "win32")
                            osMatches = true;
                        else if (name == "osx" && !Environment.Is64BitOperatingSystem)
                            osMatches = true;
                        else if (name == "osx-arm64" && Environment.Is64BitOperatingSystem)
                            osMatches = Environment.OSVersion.Platform == PlatformID.MacOSX ? false : false;
                    }
                    else
                    {
                        osMatches = true; // rule targets all OS
                    }

                    if (rule.TryGetProperty("arch", out var arch))
                    {
                        if (arch.GetString() == "x86" && !Environment.Is64BitOperatingSystem)
                            osMatches = true;
                        else
                            osMatches = false;
                    }

                    if (osMatches)
                        allowed = action == "allow";
                }
                else if (rule.TryGetProperty("features", out _))
                {
                    // Skip feature rules (not applicable)
                }
                else
                {
                    allowed = action == "allow";
                }
            }
            return allowed;
        }

        private static string LibPathFromJson(JsonElement lib, string libDir)
        {
            try
            {
                string name = lib.GetProperty("name").GetString();
                var parts = name.Split(':');
                if (parts.Length < 3) return null;

                // Check if this library has a natives classifier for Windows
                string classifier = "";
                if (parts.Length > 3)
                {
                    classifier = "-" + parts[3];
                }
                else if (lib.TryGetProperty("natives", out var natives))
                {
                    // Natives libs use classifiers instead of artifact
                    if (natives.TryGetProperty("windows", out var winNatives))
                    {
                        classifier = "-" + winNatives.GetString();
                    }
                }

                string groupId = parts[0].Replace('.', '/');
                string artifactId = parts[1];
                string version = parts[2];
                string jarPath = $"{groupId}/{artifactId}/{version}/{artifactId}-{version}{classifier}.jar";

                // Try artifact first, then classifier
                string fullPath = Path.Combine(libDir, jarPath.Replace("/", "\\"));
                if (File.Exists(fullPath)) return fullPath;

                // If library has downloads.artifact.path, use that
                if (lib.TryGetProperty("downloads", out var dl))
                {
                    if (string.IsNullOrEmpty(classifier) && dl.TryGetProperty("artifact", out var art))
                    {
                        string artPath = art.GetProperty("path").GetString();
                        fullPath = Path.Combine(libDir, artPath);
                        if (File.Exists(fullPath)) return fullPath;
                    }
                    else if (!string.IsNullOrEmpty(classifier) && dl.TryGetProperty("classifiers", out var classifiers))
                    {
                        string classifierKey = classifier.TrimStart('-');
                        if (classifiers.TryGetProperty(classifierKey, out var classDl))
                        {
                            string cPath = classDl.GetProperty("path").GetString();
                            fullPath = Path.Combine(libDir, cPath);
                            if (File.Exists(fullPath)) return fullPath;
                        }
                    }
                }

                return null;
            }
            catch { return null; }
        }
    }
}
