using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using PWPLuncher.Models;

namespace PWPLuncher.Services
{
    public static class ModService
    {
        private static List<ManifestEntry> _allMods;
        private static List<ManifestEntry> _optionalMods;
        private static HashSet<string> _selectedMods = new();

        public static IReadOnlyList<ManifestEntry> OptionalMods =>
            _optionalMods ?? new List<ManifestEntry>();

        public static event Action ModListChanged;

        public static async Task LoadMods()
        {
            _allMods = await DownloadService.FetchManifest("mod");
            _optionalMods = _allMods?.FindAll(m => m.ModOptional) ?? new List<ManifestEntry>();
            ModListChanged?.Invoke();
        }

        public static bool ToggleMod(string modPath)
        {
            if (_selectedMods.Contains(modPath))
            {
                _selectedMods.Remove(modPath);
                return false;
            }
            else
            {
                _selectedMods.Add(modPath);
                return true;
            }
        }

        public static bool IsSelected(string modPath) => _selectedMods.Contains(modPath);

        public static void SelectAll()
        {
            if (_optionalMods == null) return;
            foreach (var m in _optionalMods)
                _selectedMods.Add(m.Path);
        }

        public static void DeselectAll()
        {
            _selectedMods.Clear();
        }

        public static async Task<bool> DownloadOptionalMods(string basePath,
            IProgress<(int current, int total, string file, double speedKbps)> progress = null)
        {
            var selected = _allMods?.FindAll(m => _selectedMods.Contains(m.Path));
            if (selected == null || selected.Count == 0) return true;

            string optionalDir = Path.Combine(basePath, "optional-mods");
            return await DownloadService.DownloadFiles(selected, optionalDir, progress);
        }

        public static async Task<bool> SyncRequiredMods(string basePath,
            IProgress<(int current, int total, string file, double speedKbps)> progress = null)
        {
            var required = _allMods?.FindAll(m => !m.ModOptional);
            if (required == null || required.Count == 0) return true;

            string modsDir = Path.Combine(basePath, "mods");
            return await DownloadService.DownloadFiles(required, modsDir, progress);
        }
    }
}
