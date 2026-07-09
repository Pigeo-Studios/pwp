using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Management;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Win32;

namespace PWPLuncher.Services
{
    public class HWIDInfo
    {
        public string HWID { get; set; }
        public string ComponentsJson { get; set; }
        public string PCName { get; set; }
        public int Flags { get; set; }

        public bool IsVM => (Flags & 1) != 0;
        public bool HasDebugger => (Flags & 2) != 0;
        public bool SpooferDetected => (Flags & 4) != 0;
    }

    public static class HWIDService
    {
        public static HWIDInfo Collect()
        {
            var components = new Dictionary<string, string>();
            int flags = 0;

            if (DetectVM()) flags |= 1;
            if (DetectDebugger()) flags |= 2;
            if (DetectSpoofers()) flags |= 4;

            components["motherboard"] = GetWmiProperty("Win32_BaseBoard", "SerialNumber");
            components["disk"] = GetWmiProperty("Win32_DiskDrive", "SerialNumber", 0);
            components["processor"] = GetWmiProperty("Win32_Processor", "ProcessorId", 0);
            components["bios"] = GetWmiProperty("Win32_BIOS", "SerialNumber");
            components["mac"] = GetMacAddress();
            components["volume"] = GetVolumeSerial();
            components["system_uuid"] = GetWmiProperty("Win32_ComputerSystemProduct", "UUID");
            components["gpu"] = GetWmiProperty("Win32_VideoController", "Name", 0);

            string combined = string.Join("|", components
                .Where(kv => !string.IsNullOrEmpty(kv.Value))
                .OrderBy(kv => kv.Key)
                .Select(kv => $"{kv.Key}={kv.Value}"));

            string hwid;
            using (var sha = SHA256.Create())
            {
                byte[] hash = sha.ComputeHash(Encoding.UTF8.GetBytes(combined));
                hwid = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
            }

            return new HWIDInfo
            {
                HWID = hwid,
                ComponentsJson = System.Text.Json.JsonSerializer.Serialize(components),
                PCName = Environment.MachineName,
                Flags = flags
            };
        }

        public static bool IsHWIDBanned(string hwid, string apiUrl)
        {
            try
            {
                using var wc = new System.Net.WebClient();
                wc.Headers[System.Net.HttpRequestHeader.ContentType] = "application/json";
                var data = $"{{\"hwid\":\"{hwid}\"}}";
                var resp = wc.UploadString($"{apiUrl}/api/v1/launcher/check-hwid-ban", data);
                var doc = System.Text.Json.JsonDocument.Parse(resp);
                return doc.RootElement.GetProperty("data").GetProperty("banned").GetBoolean();
            }
            catch { return false; }
        }

        private static bool DetectVM()
        {
            try
            {
                // Check for VM artifacts via WMI
                var vmChecks = new[] {
                    ("Win32_ComputerSystem", "Model", new[] {"VirtualBox", "VMware", "Virtual Machine", "QEMU", "KVM"}),
                    ("Win32_ComputerSystem", "Manufacturer", new[] {"VMware", "VirtualBox", "Xen", "QEMU", "Microsoft Corporation"}),
                };

                foreach (var (cls, prop, keywords) in vmChecks)
                {
                    var val = GetWmiProperty(cls, prop);
                    if (!string.IsNullOrEmpty(val) && keywords.Any(k => val.IndexOf(k, StringComparison.OrdinalIgnoreCase) >= 0))
                        return true;
                }

                // Registry check
                using var key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\Disk\Enum");
                if (key?.GetValue("0") is string disk && disk.IndexOf("VBOX", StringComparison.OrdinalIgnoreCase) >= 0)
                    return true;
            }
            catch { }
            return false;
        }

        private static bool DetectDebugger()
        {
            if (Debugger.IsAttached) return true;

            try
            {
                using var proc = Process.GetCurrentProcess();
                var ntdll = proc.Modules.Cast<ProcessModule>()
                    .FirstOrDefault(m => m.ModuleName.Equals("ntdll.dll", StringComparison.OrdinalIgnoreCase));
                if (ntdll == null) return false;

                // Check NtQueryInformationProcess via P/Invoke is complex.
                // Simple heuristic: check if any debugger-related environment variable exists
                if (!string.IsNullOrEmpty(Environment.GetEnvironmentVariable("COR_ENABLE_PROFILING"))) return true;
                if (!string.IsNullOrEmpty(Environment.GetEnvironmentVariable("COR_PROFILER"))) return true;
            }
            catch { }
            return false;
        }

        private static bool DetectSpoofers()
        {
            var suspicious = new[] {
                "cheatengine", "cheat engine", "artmoney", "hacktool",
                "memoryhacker", "wpe pro", "wpepro", "fiddler",
                "httpdebugger", "processhacker", "process hacker",
                "ida64", "ida", "ollydbg", "x64dbg", "x32dbg",
                "dnspy", "httpanalyzer", "charles", "proxifier",
                "sockscap", "hwid", "spoofer", "macchanger",
                "vbox", "vmware", "sandboxie"
            };

            try
            {
                var processes = Process.GetProcesses();
                foreach (var p in processes)
                {
                    try
                    {
                        string name = p.ProcessName.ToLowerInvariant();
                        if (suspicious.Any(s => name.Contains(s)))
                            return true;
                    }
                    catch { continue; }
                }
            }
            catch { }

            // Check for VM network adapters
            try
            {
                var adapters = System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces();
                foreach (var a in adapters)
                {
                    string desc = a.Description.ToLowerInvariant();
                    if (desc.Contains("virtualbox") || desc.Contains("vmware") || desc.Contains("vpn"))
                        continue;
                    // If only VM adapters exist, likely VM
                }
            }
            catch { }

            return false;
        }

        private static string GetWmiProperty(string className, string property, int index = -1)
        {
            try
            {
                using var searcher = new ManagementObjectSearcher($"SELECT {property} FROM {className}");
                var results = searcher.Get().Cast<ManagementObject>().ToList();
                if (index >= 0 && index < results.Count)
                    return results[index][property]?.ToString()?.Trim() ?? "";
                if (results.Count > 0)
                    return results[0][property]?.ToString()?.Trim() ?? "";
            }
            catch { }
            return "";
        }

        private static string GetMacAddress()
        {
            try
            {
                var adapters = System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces()
                    .Where(a => a.OperationalStatus == System.Net.NetworkInformation.OperationalStatus.Up
                             && a.NetworkInterfaceType != System.Net.NetworkInformation.NetworkInterfaceType.Loopback
                             && !a.Description.ToLowerInvariant().Contains("virtual")
                             && !a.Description.ToLowerInvariant().Contains("vmware")
                             && !a.Description.ToLowerInvariant().Contains("vbox"));
                var first = adapters.FirstOrDefault();
                if (first != null)
                    return string.Join(":", first.GetPhysicalAddress().GetAddressBytes().Select(b => b.ToString("X2")));
            }
            catch { }
            return "";
        }

        private static string GetVolumeSerial()
        {
            try
            {
                // Get actual volume serial via WMI
                using var searcher = new ManagementObjectSearcher("SELECT VolumeSerialNumber FROM Win32_LogicalDisk WHERE DeviceID = 'C:'");
                foreach (var obj in searcher.Get())
                {
                    var val = obj["VolumeSerialNumber"]?.ToString()?.Trim();
                    if (!string.IsNullOrEmpty(val)) return val;
                }
            }
            catch { }
            return "";
        }
    }
}
