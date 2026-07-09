using System;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;

namespace PWPLuncher.Services
{
    public class SessionInfo
    {
        public string Token { get; set; }
        public string UUID { get; set; }
        public string Login { get; set; }
        public string Role { get; set; }
        public bool IsValid => !string.IsNullOrEmpty(Token);
    }

    public class ServerTokenResult
    {
        public bool Success { get; set; }
        public string ServerToken { get; set; }
        public string Username { get; set; }
        public string UUID { get; set; }
        public string Role { get; set; }
        public string Error { get; set; }
    }

    public static class AuthService
    {
        public static string ApiUrl { get; set; } = "http://pigeo.asuscomm.com:8080";
        public static SessionInfo CurrentSession { get; private set; }

        private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(5) };

        private static HttpRequestMessage MakeRequest(HttpMethod method, string url)
        {
            var uri = new Uri(url);
            var req = new HttpRequestMessage(method, uri);
            req.Headers.Add("User-Agent", "PWP Launcher/1.0");
            req.Headers.Add("X-PWP-Sign", LauncherSigner.Sign(uri.AbsolutePath));
            return req;
        }

        private static async Task<JsonDocument> PostJson(string url, object body)
        {
            var json = JsonSerializer.Serialize(body);
            var req = MakeRequest(HttpMethod.Post, url);
            req.Content = new StringContent(json, System.Text.Encoding.UTF8, "application/json");
            var resp = await Client.SendAsync(req);
            resp.EnsureSuccessStatusCode();
            return JsonDocument.Parse(await resp.Content.ReadAsStringAsync());
        }

        private static async Task<JsonDocument> GetJson(string url)
        {
            var req = MakeRequest(HttpMethod.Get, url);
            var resp = await Client.SendAsync(req);
            resp.EnsureSuccessStatusCode();
            return JsonDocument.Parse(await resp.Content.ReadAsStringAsync());
        }

        public static async Task<bool> CheckConnection()
        {
            try
            {
                var doc = await GetJson($"{ApiUrl}/api/v1/health");
                return doc.RootElement.GetProperty("status").GetString() == "ok";
            }
            catch (Exception ex) { LogService.Write("AUTH", $"Health check failed: {ex.Message}"); return false; }
        }

        public static async Task<(bool Success, string Error, SessionInfo Session)> Login(string login, string password)
        {
            try
            {
                var doc = await PostJson($"{ApiUrl}/api/v1/auth/login", new { login, password });
                if (!doc.RootElement.GetProperty("success").GetBoolean())
                {
                    var err = doc.RootElement.GetProperty("error").GetString();
                    return (false, err, null);
                }
                var data = doc.RootElement.GetProperty("data");

                if (data.TryGetProperty("2fa_required", out var tfa) && tfa.GetBoolean())
                {
                    var pending = new SessionInfo { UUID = data.GetProperty("uuid").GetString() };
                    CurrentSession = pending; // save UUID for 2FA verification
                    return (false, "2FA", pending);
                }

                var session = new SessionInfo
                {
                    Token = data.GetProperty("token").GetString(),
                    UUID = data.GetProperty("uuid").GetString(),
                    Login = data.GetProperty("login").GetString(),
                    Role = data.GetProperty("role").GetString()
                };
                CurrentSession = session;
                SaveSession(session);
                _ = SubmitHwidAsync();
                return (true, null, session);
            }
            catch (TaskCanceledException) { return (false, "Сервер не отвечает (таймаут 10с)", null); }
            catch (HttpRequestException ex) { return (false, $"Сервер недоступен: {ex.Message}", null); }
            catch (Exception ex) { return (false, ex.Message, null); }
        }

        public static async Task<(bool Success, string Error, SessionInfo Session)> Verify2FA(string uuid, string code)
        {
            try
            {
                var doc = await PostJson($"{ApiUrl}/api/v1/auth/verify-2fa", new { uuid, code });
                if (!doc.RootElement.GetProperty("success").GetBoolean())
                    return (false, doc.RootElement.GetProperty("error").GetString(), null);

                var data = doc.RootElement.GetProperty("data");
                var session = new SessionInfo
                {
                    Token = data.GetProperty("token").GetString(),
                    UUID = data.GetProperty("uuid").GetString(),
                    Login = data.GetProperty("login").GetString(),
                    Role = data.GetProperty("role").GetString()
                };
                CurrentSession = session;
                SaveSession(session);
                _ = SubmitHwidAsync();
                return (true, null, session);
            }
            catch (TaskCanceledException) { return (false, "Сервер не отвечает", null); }
            catch (Exception ex) { return (false, ex.Message, null); }
        }

        private static async Task SubmitHwidAsync()
        {
            try
            {
                if (CurrentSession == null) return;
                var hwid = HWIDService.Collect();
                var data = new
                {
                    sessionToken = CurrentSession.Token,
                    hwid = hwid.HWID,
                    hwidComponents = hwid.ComponentsJson,
                    pcName = hwid.PCName,
                    flags = hwid.Flags
                };
                await PostJson($"{ApiUrl}/api/v1/launcher/submit-hwid", data);
            }
            catch { }
        }

        public static async Task<ServerTokenResult> GetServerToken(HWIDInfo hwid)
        {
            try
            {
                if (CurrentSession == null)
                    return new ServerTokenResult { Success = false, Error = "Not logged in" };

                var doc = await PostJson($"{ApiUrl}/api/v1/launcher/server-token", new
                {
                    sessionToken = CurrentSession.Token
                });

                if (!doc.RootElement.GetProperty("success").GetBoolean())
                    return new ServerTokenResult { Success = false, Error = doc.RootElement.GetProperty("error").GetString() };

                var data = doc.RootElement.GetProperty("data");
                return new ServerTokenResult
                {
                    Success = true,
                    ServerToken = data.GetProperty("server_token").GetString(),
                    Username = data.GetProperty("username").GetString(),
                    UUID = data.GetProperty("uuid").GetString(),
                    Role = data.GetProperty("role").GetString()
                };
            }
            catch (TaskCanceledException) { return new ServerTokenResult { Success = false, Error = "Сервер не отвечает" }; }
            catch (Exception ex) { return new ServerTokenResult { Success = false, Error = ex.Message }; }
        }

        // ── Session persistence ───────────────────────────
        private static readonly string SessionPath = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".pwplauncher", "session.dat");

        private static void SaveSession(SessionInfo session)
        {
            try
            {
                var dir = System.IO.Path.GetDirectoryName(SessionPath);
                System.IO.Directory.CreateDirectory(dir);
                var encrypted = Convert.ToBase64String(
                    System.Security.Cryptography.ProtectedData.Protect(
                        System.Text.Encoding.UTF8.GetBytes(JsonSerializer.Serialize(session)),
                        null, System.Security.Cryptography.DataProtectionScope.CurrentUser));
                System.IO.File.WriteAllText(SessionPath, encrypted);
            }
            catch { }
        }

        public static bool TryRestoreSession()
        {
            try
            {
                if (!System.IO.File.Exists(SessionPath)) return false;
                var encrypted = System.IO.File.ReadAllText(SessionPath);
                var decrypted = System.Security.Cryptography.ProtectedData.Unprotect(
                    Convert.FromBase64String(encrypted), null,
                    System.Security.Cryptography.DataProtectionScope.CurrentUser);
                var json = System.Text.Encoding.UTF8.GetString(decrypted);
                CurrentSession = JsonSerializer.Deserialize<SessionInfo>(json);
                return CurrentSession?.IsValid == true;
            }
            catch { return false; }
        }

        public static void Logout()
        {
            CurrentSession = null;
            try { if (System.IO.File.Exists(SessionPath)) System.IO.File.Delete(SessionPath); }
            catch { }
        }
    }
}
