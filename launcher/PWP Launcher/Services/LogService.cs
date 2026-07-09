using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public static class LogService
    {
        private static readonly ConcurrentQueue<string> LogBuffer = new();
        private static readonly string LogPath;
        private static readonly object FileLock = new();

        public static event Action<string> NewLogEntry;

        public static List<string> AllLogs { get; } = new();

        static LogService()
        {
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string dir = Path.Combine(appData, ".pwplauncher", "logs");
            Directory.CreateDirectory(dir);
            LogPath = Path.Combine(dir, $"launcher_{DateTime.Now:yyyy-MM-dd}.log");
        }

        public static void Write(string tag, string message)
        {
            string entry = $"[{DateTime.Now:HH:mm:ss}] [{tag}] {message}";
            LogBuffer.Enqueue(entry);

            lock (AllLogs)
            {
                AllLogs.Add(entry);
            }

            NewLogEntry?.Invoke(entry);
            System.Diagnostics.Debug.WriteLine(entry);
        }

        public static void FlushToDisk()
        {
            var lines = new List<string>();
            while (LogBuffer.TryDequeue(out string entry))
                lines.Add(entry);

            if (lines.Count == 0) return;

            lock (FileLock)
            {
                try
                {
                    File.AppendAllLines(LogPath, lines, Encoding.UTF8);
                }
                catch { }
            }
        }

        public static async Task UploadLogs()
        {
            // Just flush to disk, no remote upload
            FlushToDisk();
        }

        public static string GetRecentLogs(int count = 50)
        {
            lock (AllLogs)
            {
                int take = Math.Min(count, AllLogs.Count);
                var recent = new List<string>();
                for (int i = AllLogs.Count - take; i < AllLogs.Count; i++)
                    recent.Add(AllLogs[i]);
                return string.Join(Environment.NewLine, recent);
            }
        }
    }
}
