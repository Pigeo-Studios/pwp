using System;
using System.Security.Cryptography;
using System.Text;

namespace PWPLuncher.Services
{
    public static class LauncherSigner
    {
        // Должен совпадать с launcher.secret в config.json Core Service
        private const string Secret = "pwp_launcher_secret_2024";

        public static string Sign(string path)
        {
            long timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            string data = $"{timestamp}:{path}";

            using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(Secret));
            byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(data));
            string hex = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();

            return $"{timestamp}:{hex}";
        }
    }
}
