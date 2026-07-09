using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Threading.Tasks;
using PWPLuncher.Models;

namespace PWPLuncher.Services
{
    public class VerificationResult
    {
        public bool Valid { get; set; }
        public List<string> MissingFiles { get; set; } = new();
        public List<string> CorruptedFiles { get; set; } = new();
        public long TotalBytesNeeded { get; set; }
        public int TotalFilesNeeded { get; set; }
    }

    public static class VerificationService
    {
        public static async Task<VerificationResult> VerifyFiles(
            List<ManifestEntry> manifest,
            string basePath,
            IProgress<(int current, int total, string file)> progress = null)
        {
            var result = new VerificationResult();
            int total = manifest.Count;
            int current = 0;

            foreach (var entry in manifest)
            {
                current++;
                progress?.Report((current, total, entry.Path));

                string fullPath = Path.Combine(basePath, entry.Path);

                if (!File.Exists(fullPath))
                {
                    result.MissingFiles.Add(entry.Path);
                    result.TotalBytesNeeded += entry.Size;
                    result.TotalFilesNeeded++;
                    continue;
                }

                var fileInfo = new FileInfo(fullPath);
                if (fileInfo.Length != entry.Size)
                {
                    result.CorruptedFiles.Add(entry.Path);
                    result.TotalBytesNeeded += entry.Size;
                    result.TotalFilesNeeded++;
                    continue;
                }

                string hash = await Task.Run(() => ComputeSha256(fullPath));
                if (!string.Equals(hash, entry.Sha256, StringComparison.OrdinalIgnoreCase))
                {
                    result.CorruptedFiles.Add(entry.Path);
                    result.TotalBytesNeeded += entry.Size;
                    result.TotalFilesNeeded++;
                }
            }

            result.Valid = result.MissingFiles.Count == 0 && result.CorruptedFiles.Count == 0;
            return result;
        }

        public static string ComputeSha256(string filePath)
        {
            using var sha = SHA256.Create();
            using var stream = File.OpenRead(filePath);
            byte[] hash = sha.ComputeHash(stream);
            return BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
        }

        public static bool VerifySingleFile(string filePath, string expectedSha256)
        {
            try
            {
                string actual = ComputeSha256(filePath);
                return string.Equals(actual, expectedSha256, StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }
    }
}
