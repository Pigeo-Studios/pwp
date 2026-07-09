using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using PWPLuncher.Models;
using PWPLuncher.Services;

namespace PWPLuncher
{
    public partial class MainWindow : Window
    {
        private string _launcherPath;
        private bool _isLaunching;

        public MainWindow()
        {
            try
            {
                InitializeComponent();
                Loaded += OnLoaded;
                LogService.NewLogEntry += OnNewLog;
                ProcessWatcherService.CheatDetected += OnCheatDetected;
                ProcessWatcherService.GameExited += OnGameExited;
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show($"Init error: {ex.Message}\n{ex.StackTrace}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private async void OnLoaded(object sender, RoutedEventArgs e)
        {
            LogService.Write("LAUNCHER", "PWP Launcher starting...");

            // Set up paths
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            _launcherPath = Path.Combine(appData, ".pwplauncher");
            PathBox.Text = _launcherPath;
            Directory.CreateDirectory(_launcherPath);
            Directory.CreateDirectory(Path.Combine(_launcherPath, "minecraft"));
            Directory.CreateDirectory(Path.Combine(_launcherPath, "minecraft", ".minecraft"));

            // Load saved settings
            LoadSettings();

            // Periodic disk flush
            _ = Task.Run(async () =>
            {
                while (true)
                {
                    await Task.Delay(5000);
                    try { LogService.FlushToDisk(); } catch { }
                }
            });

            // Check API connection
            bool apiOnline = await AuthService.CheckConnection();
            if (!apiOnline)
            {
                LogService.Write("LAUNCHER", "API not reachable!");
                StatusText.Text = "Сервер недоступен";
                StatusDot.Fill = (SolidColorBrush)FindResource("BrushRed");
                PlaySubtext.Text = "Не удалось подключиться к серверу";
                BtnPlay.IsEnabled = false;
                ShowLoginPanel();
                LoginStatus.Text = "Сервер не отвечает. Попробуйте позже.";
                LoginStatus.Foreground = (SolidColorBrush)FindResource("BrushRed");

                // Auto-retry every 10 seconds
                _ = Task.Run(async () =>
                {
                    while (!apiOnline)
                    {
                        await Task.Delay(10000);
                        apiOnline = await AuthService.CheckConnection();
                        if (apiOnline)
                        {
                            await Dispatcher.InvokeAsync(() =>
                            {
                                StatusText.Text = "Готов";
                                StatusDot.Fill = (SolidColorBrush)FindResource("BrushGreen");
                                PlaySubtext.Text = "Нажмите чтобы начать";
                                BtnPlay.IsEnabled = true;
                                LoginStatus.Text = "";
                                ShowLoginPanel();
                            });
                        }
                    }
                });
                return;
            }

            // Check for launcher update
            await CheckUpdate();

            // Try restore session
            if (AuthService.TryRestoreSession())
            {
                ShowMainPanel();
                LogService.Write("AUTH", $"Session restored: {AuthService.CurrentSession.Login}");
                UserGreeting.Text = $"Добро пожаловать, {AuthService.CurrentSession.Login}";

                // Preload mods in background
                _ = ModService.LoadMods();
            }
            else
            {
                ShowLoginPanel();
            }
        }

        // ── Panel Navigation ──────────────────────────────

        private void ShowLoginPanel()
        {
            PanelLogin.Visibility = Visibility.Visible;
            Panel2FA.Visibility = Visibility.Collapsed;
            PanelMain.Visibility = Visibility.Collapsed;
            PanelMods.Visibility = Visibility.Collapsed;
            PanelConsole.Visibility = Visibility.Collapsed;
            TitleText.Text = "PWP LAUNCHER — ВХОД";
        }

        private void Show2faPanel()
        {
            PanelLogin.Visibility = Visibility.Collapsed;
            Panel2FA.Visibility = Visibility.Visible;
            PanelMain.Visibility = Visibility.Collapsed;
            PanelMods.Visibility = Visibility.Collapsed;
            PanelConsole.Visibility = Visibility.Collapsed;
            TitleText.Text = "PWP LAUNCHER — 2FA";
            TfaCodeBox.Focus();
        }

        private void ShowMainPanel()
        {
            PanelLogin.Visibility = Visibility.Collapsed;
            Panel2FA.Visibility = Visibility.Collapsed;
            PanelMain.Visibility = Visibility.Visible;
            PanelMods.Visibility = Visibility.Collapsed;
            PanelConsole.Visibility = Visibility.Collapsed;
            PanelSettings.Visibility = Visibility.Collapsed;
            TitleText.Text = $"PWP LAUNCHER — {AuthService.CurrentSession?.Login ?? "ГЛАВНАЯ"}";
        }

        // ── Login Logic ───────────────────────────────────

        private async void BtnLogin_Click(object sender, RoutedEventArgs e)
        {
            string login = LoginBox.Text.Trim();
            string password = PasswordBox.Password;

            if (string.IsNullOrEmpty(login) || string.IsNullOrEmpty(password))
            {
                LoginError.Text = "Введите логин и пароль";
                return;
            }

            BtnLogin.IsEnabled = false;
            LoginError.Text = "";
            LoginStatus.Text = "Вход...";

            // Быстрая проверка связи (2 сек)
            if (!await FastPing())
            {
                LoginError.Text = "Сервер не отвечает";
                LoginStatus.Text = "";
                BtnLogin.IsEnabled = true;
                return;
            }

            var (success, error, session) = await AuthService.Login(login, password);

            if (success)
            {
                LogService.Write("AUTH", $"Login successful: {session.Login}");
                UserGreeting.Text = $"Добро пожаловать, {session.Login}";
                PasswordBox.Password = "";
                ShowMainPanel();

                // Preload mods in background
                _ = ModService.LoadMods();
            }
            else if (error == "2FA")
            {
                LogService.Write("AUTH", "2FA required");
                Show2faPanel();
            }
            else
            {
                LoginError.Text = error ?? "Ошибка входа";
                LogService.Write("AUTH", $"Login failed: {error}");
            }

            BtnLogin.IsEnabled = true;
            LoginStatus.Text = "";
        }

        private async void BtnVerify2FA_Click(object sender, RoutedEventArgs e)
        {
            string code = TfaCodeBox.Text.Trim();
            if (code.Length < 4)
            {
                TfaError.Text = "Введите код из Telegram";
                return;
            }

            BtnVerify2FA.IsEnabled = false;
            TfaError.Text = "";

            var (success, error, session) = await AuthService.Verify2FA(
                AuthService.CurrentSession?.UUID ?? "", code);

            if (success)
            {
                LogService.Write("AUTH", "2FA verified");
                UserGreeting.Text = $"Добро пожаловать, {session.Login}";
                TfaCodeBox.Text = "";
                ShowMainPanel();
                _ = ModService.LoadMods();
            }
            else
            {
                TfaError.Text = error ?? "Неверный код";
                LogService.Write("AUTH", $"2FA failed: {error}");
            }

            BtnVerify2FA.IsEnabled = true;
        }

        private static readonly string SettingsPath = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            ".pwplauncher", "settings.json");

        private void LoadSettings()
        {
            try
            {
                if (File.Exists(SettingsPath))
                {
                    var json = File.ReadAllText(SettingsPath);
                    var doc = System.Text.Json.JsonDocument.Parse(json);
                    if (doc.RootElement.TryGetProperty("ram", out var ram))
                        RamSlider.Value = Math.Clamp(ram.GetInt32(), 2, 16);
                }
            }
            catch { }
        }

        private void SaveSettings()
        {
            try
            {
                var dir = System.IO.Path.GetDirectoryName(SettingsPath);
                Directory.CreateDirectory(dir);
                var data = new { ram = (int)RamSlider.Value };
                var json = System.Text.Json.JsonSerializer.Serialize(data);
                File.WriteAllText(SettingsPath, json);
            }
            catch { }
        }

        private async Task<bool> FastPing()
        {
            try
            {
                using var fastClient = new System.Net.Http.HttpClient { Timeout = TimeSpan.FromSeconds(2) };
                var resp = await fastClient.GetAsync($"{AuthService.ApiUrl}/api/v1/health");
                return resp.IsSuccessStatusCode;
            }
            catch { return false; }
        }

        private async void BtnPlay_Click(object sender, RoutedEventArgs e)
        {
            if (_isLaunching) return;
            _isLaunching = true;
            BtnPlay.IsEnabled = false;
            PlaySubtext.Text = "Подготовка...";
            SaveSettings();

            if (!await FastPing())
            {
                PlaySubtext.Text = "Сервер недоступен";
                StatusText.Text = "Ошибка подключения";
                StatusDot.Fill = (SolidColorBrush)FindResource("BrushRed");
                _isLaunching = false;
                BtnPlay.IsEnabled = true;
                return;
            }

            try
            {
                // Phase 0: HWID check
                LogService.Write("LAUNCH", "Phase 0: HWID check...");
                StatusText.Text = "Проверка оборудования...";
                var hwid = HWIDService.Collect();

                if (hwid.IsVM)
                {
                    LogService.Write("ANTI-CHEAT", "VM detected");
                }
                if (hwid.HasDebugger)
                {
                    await ShowError("Обнаружен отладчик. Пожалуйста, закройте его.");
                    return;
                }
                if (hwid.SpooferDetected)
                {
                    await ShowError("Обнаружены подозрительные программы.");
                    return;
                }

                // Check HWID ban
                if (HWIDService.IsHWIDBanned(hwid.HWID, AuthService.ApiUrl))
                {
                    await ShowError("Ваше оборудование заблокировано.\nОбратитесь в поддержку.");
                    return;
                }

                // Phase 1: Pre-launch cheat check
                LogService.Write("LAUNCH", "Phase 1: Anti-cheat check...");
                StatusText.Text = "Проверка системы...";
                if (!ProcessWatcherService.PreLaunchCheck())
                {
                    await ShowError("Обнаружены запрещенные программы.\nЗакройте их и попробуйте снова.");
                    return;
                }

                // Phase 2: Fetch manifest
                LogService.Write("LAUNCH", "Phase 2: Fetching manifest...");
                StatusText.Text = "Загрузка манифеста...";
                UpdateProgress(0, "Загрузка списка файлов...");
                var manifest = await DownloadService.FetchManifest();

                if (manifest.Count == 0)
                {
                    LogService.Write("LAUNCH", "Empty manifest, skipping file check");
                }

                // Phase 3: Verify files
                LogService.Write("LAUNCH", "Phase 3: Verifying files...");
                StatusText.Text = "Проверка файлов...";
                string mcPath = Path.Combine(_launcherPath, "minecraft", ".minecraft");
                var verificationProgress = new Progress<(int, int, string)>(v =>
                {
                    UpdateProgress(v.Item1 * 50 / v.Item2, $"Проверка: {Path.GetFileName(v.Item3)}");
                });
                var verifyResult = await VerificationService.VerifyFiles(manifest, mcPath, verificationProgress);

                if (!verifyResult.Valid)
                {
                    LogService.Write("LAUNCH", $"Need {verifyResult.TotalFilesNeeded} files ({verifyResult.TotalBytesNeeded} bytes)");
                    StatusText.Text = "Загрузка файлов...";

                    // Download missing/corrupted files
                    var filesToGet = manifest.Where(m =>
                        verifyResult.MissingFiles.Contains(m.Path) ||
                        verifyResult.CorruptedFiles.Contains(m.Path)).ToList();

                    var downloadProgress = new Progress<(int, int, string, double)>(v =>
                    {
                        int pct = 50 + (v.Item1 * 50 / v.Item2);
                        string speed = v.Item4 > 0 ? $" [{v.Item4:F0} KB/s]" : "";
                        UpdateProgress(pct, $"Загрузка: {Path.GetFileName(v.Item3)}{speed}");
                    });

                    bool dlOk = await DownloadService.DownloadFiles(filesToGet, mcPath, downloadProgress);
                    if (!dlOk)
                    {
                        await ShowError("Ошибка загрузки файлов. Проверьте подключение.");
                        return;
                    }
                }

                // Phase 4: Java
                LogService.Write("LAUNCH", "Phase 4: Java check...");
                StatusText.Text = "Проверка Java...";
                UpdateProgress(90, "Проверка Java...");
                bool javaOk = await JavaService.EnsureJava(_launcherPath);
                if (!javaOk)
                {
                    await ShowError("Не удалось установить Java 17.");
                    return;
                }

                // Phase 5: Minecraft + Forge install
                LogService.Write("LAUNCH", "Phase 5: Installing Minecraft + Forge...");
                StatusText.Text = "Установка...";
                UpdateProgress(90, "Minecraft + Forge...");
                string gameDir = Path.Combine(_launcherPath, "minecraft");
                bool installOk = await ForgeInstallService.EnsureForgeInstalled(gameDir);
                if (!installOk)
                {
                    await ShowError("Не удалось установить Minecraft/Forge.");
                    return;
                }

                // Phase 6: Mods + gun packs sync
                LogService.Write("LAUNCH", "Phase 6: Syncing mods...");
                StatusText.Text = "Синхронизация модов...";
                UpdateProgress(93, "Моды...");
                await ModService.LoadMods();
                await ModService.SyncRequiredMods(mcPath);

                // Phase 6b: TACZ gun packs (from GitHub)
                LogService.Write("LAUNCH", "Phase 6b: Syncing gun packs...");
                StatusText.Text = "Синхронизация обвесов...";
                try
                {
                    string taczDir = Path.Combine(mcPath, "tacz");
                    string gh = "https://raw.githubusercontent.com/Pigeo-Studios/Pwpfiles/main/tacz";
                    var gunPacks = new (string zip, string folder)[]
                    {
                        ("endlessammo.zip", "endlessammo"),
                        ("gucci_vuitton_attachment.zip", "gucci_vuitton_attachment"),
                        ("maxstuff.zip", "maxstuff"),
                        ("tacz_default_gun.zip", "tacz_default_gun"),
                        ("cibr_guns_pack.zip", null), // null = download as-is, don't extract
                    };

                    foreach (var (zip, folder) in gunPacks)
                    {
                        if (folder == null)
                        {
                            // Copy zip directly to tacz dir (gun pack in .zip format)
                            string destZip = Path.Combine(taczDir, zip);
                            if (File.Exists(destZip)) continue;
                            Directory.CreateDirectory(taczDir);
                            LogService.Write("INSTALL", $"Downloading {zip}...");
                            using (var wc = new WebClient())
                            {
                                wc.Headers["User-Agent"] = "PWP Launcher";
                                await wc.DownloadFileTaskAsync($"{gh}/{zip}", destZip);
                            }
                        }
                        else
                        {
                            string packDir = Path.Combine(taczDir, folder);
                            if (Directory.Exists(packDir)) continue;

                            string localZip = Path.Combine(Path.GetTempPath(), zip);
                            LogService.Write("INSTALL", $"Downloading {zip}...");
                            using (var wc = new WebClient())
                            {
                                wc.Headers["User-Agent"] = "PWP Launcher";
                                await wc.DownloadFileTaskAsync($"{gh}/{zip}", localZip);
                            }

                            Directory.CreateDirectory(packDir);
                            ZipFile.ExtractToDirectory(localZip, packDir);
                            try { File.Delete(localZip); } catch { }
                        }
                    }
                    LogService.Write("INSTALL", "Gun packs installed");
                }
                catch (Exception ex)
                {
                    LogService.Write("LAUNCH", $"TACZ sync skipped: {ex.Message}");
                }

                // Phase 6c: Verify mod integrity
                try
                {
                    string modsDir = Path.Combine(mcPath, "mods");
                    if (Directory.Exists(modsDir))
                    {
                        var modManifest = await DownloadService.FetchManifest("mod");
                        var needRedownload = new List<string>();

                        foreach (var entry in modManifest)
                        {
                            string filePath = Path.Combine(modsDir, Path.GetFileName(entry.Path));
                            if (!File.Exists(filePath)) { needRedownload.Add(entry.Path); continue; }

                            using var stream = File.OpenRead(filePath);
                            string hash = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(stream)).ToLower();
                            if (hash != entry.Sha256) needRedownload.Add(entry.Path);
                        }

                        if (needRedownload.Count > 0)
                        {
                            LogService.Write("LAUNCH", $"Mod integrity: {needRedownload.Count} files changed, redownloading...");
                            StatusText.Text = "Перезагрузка изменённых модов...";
                            var required = modManifest.Where(m => needRedownload.Contains(m.Path)).ToList();
                            await DownloadService.DownloadFiles(required, modsDir);
                        }
                        else
                        {
                            LogService.Write("LAUNCH", $"Mod integrity: OK ({modManifest.Count} files)");
                        }
                    }
                }
                catch (Exception ex)
                {
                    LogService.Write("LAUNCH", $"Mod integrity check skipped: {ex.Message}");
                }

                // Phase 7: Get server token
                LogService.Write("LAUNCH", "Phase 7: Getting server token...");
                StatusText.Text = "Авторизация на сервере...";
                UpdateProgress(96, "Получение токена...");
                var tokenResult = await AuthService.GetServerToken(hwid);
                if (!tokenResult.Success)
                {
                    if (tokenResult.Error != null && tokenResult.Error.Contains("session"))
                    {
                        LogService.Write("AUTH", "Session expired, redirecting to login");
                        AuthService.Logout();
                        ShowLoginPanel();
                    }
                    await ShowError(tokenResult.Error ?? "Ошибка авторизации");
                    return;
                }

                // Phase 8: Launch
                LogService.Write("LAUNCH", "Phase 8: Launching Minecraft...");
                StatusText.Text = "Запуск...";
                UpdateProgress(100, "Запуск Minecraft...");
                await Task.Delay(500);

                var proc = await LaunchService.LaunchMinecraft(
                    (int)RamSlider.Value,
                    mcPath,
                    tokenResult.ServerToken);

                if (proc != null && !proc.HasExited)
                {
                    PlaySubtext.Text = "Minecraft запущен";
                    StatusText.Text = "В игре";
                    StatusDot.Fill = (SolidColorBrush)FindResource("BrushAccent");
                    BtnPlay.Content = "&#x25B6;  В ИГРЕ";

                    // Upload logs after launch
                    _ = LogService.UploadLogs();

                    // Wait for exit
                    await Task.Run(() => proc.WaitForExit());

                    LogService.Write("LAUNCH", "Minecraft closed");
                    StatusText.Text = "Готов";
                    StatusDot.Fill = (SolidColorBrush)FindResource("BrushGreen");
                    PlaySubtext.Text = "Нажмите чтобы начать";
                    BtnPlay.Content = "&#x25B6;  PLAY";
                }
            }
            catch (Exception ex)
            {
                LogService.Write("LAUNCH", $"Error: {ex.Message}");
                await ShowError($"Ошибка запуска: {ex.Message}");
            }
            finally
            {
                _isLaunching = false;
                BtnPlay.IsEnabled = true;
                BtnPlay.Content = "&#x25B6;  PLAY";
                ProgressBar.Value = 0;
                ProgressText.Text = "";
            }
        }

        // ── UI Helpers ────────────────────────────────────

        private void UpdateProgress(int pct, string text)
        {
            Dispatcher.Invoke(() =>
            {
                ProgressBar.Value = pct;
                ProgressText.Text = text;
            });
        }

        private Task ShowError(string message)
        {
            LogService.Write("ERROR", message);
            return Dispatcher.InvokeAsync(() =>
            {
                PlaySubtext.Text = message;
                StatusText.Text = "Ошибка";
                StatusDot.Fill = (SolidColorBrush)FindResource("BrushRed");
                BtnPlay.IsEnabled = true;
                _isLaunching = false;
                ProgressBar.Value = 0;
                ProgressText.Text = "";
            }).Task;
        }

        private async Task CheckUpdate()
        {
            try
            {
                var update = await UpdateService.CheckForUpdate();
                if (update.UpdateAvailable)
                {
                    LogService.Write("UPDATE", $"v{update.Version} available");
                    var result = System.Windows.MessageBox.Show(
                        $"Доступно обновление лаунчера v{update.Version}.\n" +
                        (update.Mandatory ? "Это обязательное обновление.\n" : "Хотите обновить?") +
                        "Нажмите OK для обновления.",
                        "Обновление лаунчера",
                        MessageBoxButton.OKCancel,
                        MessageBoxImage.Information);

                    if (result == MessageBoxResult.OK)
                    {
                        bool applied = await UpdateService.ApplyUpdate(update);
                        if (applied)
                        {
                            System.Windows.Application.Current.Shutdown();
                            return;
                        }
                        else
                        {
                            LogService.Write("UPDATE", "Update apply failed");
                        }
                    }
                }
            }
            catch { }
        }

        // ── Cheat Detection Callback ──────────────────────

        private void OnCheatDetected(string message)
        {
            Dispatcher.Invoke(async () =>
            {
                await ShowError(message);
                BtnPlay.Content = "&#x25B6;  PLAY";
                StatusText.Text = "Чит обнаружен";
                StatusDot.Fill = (SolidColorBrush)FindResource("BrushRed");
            });
        }

        private void OnGameExited()
        {
            Dispatcher.Invoke(() =>
            {
                if (!_isLaunching)
                {
                    StatusText.Text = "Готов";
                    StatusDot.Fill = (SolidColorBrush)FindResource("BrushGreen");
                    PlaySubtext.Text = "Нажмите чтобы начать";
                    BtnPlay.Content = "&#x25B6;  PLAY";
                }
            });
        }

        // ── Console ───────────────────────────────────────

        private void OnNewLog(string entry)
        {
            Dispatcher.Invoke(() =>
            {
                ConsoleOutput.Text += entry + Environment.NewLine;
                if (ConsoleScroll.ViewportHeight + ConsoleScroll.VerticalOffset >= ConsoleScroll.ExtentHeight - 20)
                    ConsoleScroll.ScrollToEnd();
            });
        }

        // ── Button Handlers ───────────────────────────────

        private void BtnMinimize_Click(object sender, RoutedEventArgs e) =>
            WindowState = WindowState.Minimized;

        private void BtnClose_Click(object sender, RoutedEventArgs e)
        {
            SaveSettings();
            ProcessWatcherService.StopMonitoring();
            System.Windows.Application.Current.Shutdown();
        }

        private void BtnBrowse_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new Microsoft.Win32.OpenFileDialog
            {
                Title = "Выберите папку для установки",
                ValidateNames = false,
                CheckFileExists = false,
                CheckPathExists = false,
                FileName = "Выберите папку"
            };
            if (dlg.ShowDialog() == true)
            {
                _launcherPath = System.IO.Path.GetDirectoryName(dlg.FileName);
                PathBox.Text = _launcherPath;
            }
        }

        private void BtnLogout_Click(object sender, RoutedEventArgs e)
        {
            AuthService.Logout();
            LoginBox.Text = "";
            PasswordBox.Password = "";
            LoginError.Text = "";
            ShowLoginPanel();
        }

        private void BtnMods_Click(object sender, RoutedEventArgs e)
        {
            PanelMain.Visibility = Visibility.Collapsed;
            PanelMods.Visibility = Visibility.Visible;
            TitleText.Text = "PWP LAUNCHER — МОДЫ";
            BuildModsList();
        }

        private void BtnModsBack_Click(object sender, RoutedEventArgs e)
        {
            ShowMainPanel();
        }

        private void BtnConsole_Click(object sender, RoutedEventArgs e)
        {
            PanelMain.Visibility = Visibility.Collapsed;
            PanelConsole.Visibility = Visibility.Visible;
            TitleText.Text = "PWP LAUNCHER — КОНСОЛЬ";
        }

        private void BtnConsoleBack_Click(object sender, RoutedEventArgs e)
        {
            ShowMainPanel();
        }

        private void BtnSendLogs_Click(object sender, RoutedEventArgs e)
        {
            _ = LogService.UploadLogs();
        }

        private void BtnClearLogs_Click(object sender, RoutedEventArgs e)
        {
            ConsoleOutput.Text = "";
        }

        // ── Settings Panel ────────────────────────────────

        private void BtnSettings_Click(object sender, RoutedEventArgs e)
        {
            PanelMain.Visibility = Visibility.Collapsed;
            PanelSettings.Visibility = Visibility.Visible;
            TitleText.Text = "PWP LAUNCHER — НАСТРОЙКИ";

            // Sync RAM slider
            SettingsRamSlider.Value = RamSlider.Value;
            SettingsRamLabel.Text = $"{(int)RamSlider.Value} GB";

            // Check Java
            _ = RefreshJavaStatus();
        }

        private void BtnSettingsBack_Click(object sender, RoutedEventArgs e)
        {
            // Sync RAM back
            RamSlider.Value = SettingsRamSlider.Value;
            ShowMainPanel();
        }

        private void SettingsRamSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (SettingsRamLabel != null)
                SettingsRamLabel.Text = $"{(int)e.NewValue} GB";
        }

        private async Task RefreshJavaStatus()
        {
            string path = JavaService.JavaPath;
            if (!string.IsNullOrEmpty(path))
            {
                string ver = await JavaService.GetJavaVersion();
                JavaStatus.Text = $"✓ {path}";
                JavaStatus.Foreground = (SolidColorBrush)FindResource("BrushGreen");
                BtnJavaDownload.IsEnabled = false;
            }
            else
            {
                JavaStatus.Text = "✗ Java 17 не найдена";
                JavaStatus.Foreground = (SolidColorBrush)FindResource("BrushRed");
            }
        }

        private async void BtnJavaDetect_Click(object sender, RoutedEventArgs e)
        {
            JavaStatus.Text = "Поиск Java...";
            bool ok = await JavaService.EnsureJava(_launcherPath);
            if (ok) await RefreshJavaStatus();
            else JavaStatus.Text = "✗ Java не найдена. Нажмите 'Скачать'";
        }

        private async void BtnJavaDownload_Click(object sender, RoutedEventArgs e)
        {
            JavaProgress.Visibility = Visibility.Visible;
            JavaStatus.Text = "Скачивание Java 17...";
            bool ok = await JavaService.EnsureJava(_launcherPath);
            if (ok)
            {
                await RefreshJavaStatus();
                JavaProgress.Visibility = Visibility.Collapsed;
            }
            else
            {
                JavaStatus.Text = "✗ Ошибка скачивания Java";
                JavaProgress.Visibility = Visibility.Collapsed;
            }
        }

        // ── Mods Panel ────────────────────────────────────

        private async void BuildModsList()
        {
            await ModService.LoadMods();
            ModsList.Children.Clear();

            var mods = ModService.OptionalMods;
            if (mods.Count == 0)
            {
                ModsList.Children.Add(new TextBlock
                {
                    Text = "Нет доступных опциональных модов",
                    Foreground = (SolidColorBrush)FindResource("BrushTextMuted"),
                    Margin = new Thickness(0, 16, 0, 0),
                    HorizontalAlignment = HorizontalAlignment.Center
                });
                return;
            }

            foreach (var mod in mods)
            {
                var border = new Border
                {
                    Background = (SolidColorBrush)FindResource("BrushBgCard"),
                    CornerRadius = new CornerRadius(6),
                    BorderBrush = (SolidColorBrush)FindResource("BrushBorder"),
                    BorderThickness = new Thickness(1),
                    Margin = new Thickness(0, 4, 0, 4)
                };

                var stack = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(12, 8, 12, 8) };

                var check = new CheckBox
                {
                    IsChecked = ModService.IsSelected(mod.Path),
                    VerticalAlignment = VerticalAlignment.Center,
                    Foreground = (SolidColorBrush)FindResource("BrushAccent"),
                    Tag = mod.Path
                };
                check.Checked += (s, e) => ModService.ToggleMod(mod.Path);
                check.Unchecked += (s, e) => ModService.ToggleMod(mod.Path);

                var infoStack = new StackPanel { Margin = new Thickness(12, 0, 0, 0) };
                infoStack.Children.Add(new TextBlock
                {
                    Text = mod.ModName ?? Path.GetFileName(mod.Path),
                    FontWeight = FontWeights.Bold,
                    Foreground = (SolidColorBrush)FindResource("BrushTextPrimary"),
                    FontSize = 13
                });
                if (!string.IsNullOrEmpty(mod.ModDescription))
                {
                    infoStack.Children.Add(new TextBlock
                    {
                        Text = mod.ModDescription,
                        Foreground = (SolidColorBrush)FindResource("BrushTextMuted"),
                        FontSize = 11,
                        TextWrapping = TextWrapping.Wrap,
                        MaxWidth = 400
                    });
                }

                stack.Children.Add(check);
                stack.Children.Add(infoStack);
                border.Child = stack;
                ModsList.Children.Add(border);
            }
        }

        private void BtnSelectAll_Click(object sender, RoutedEventArgs e)
        {
            ModService.SelectAll();
            foreach (var child in ModsList.Children)
            {
                if (child is Border border && border.Child is StackPanel sp)
                {
                    foreach (var el in sp.Children)
                    {
                        if (el is CheckBox cb) cb.IsChecked = true;
                    }
                }
            }
        }

        private void BtnDeselectAll_Click(object sender, RoutedEventArgs e)
        {
            ModService.DeselectAll();
            foreach (var child in ModsList.Children)
            {
                if (child is Border border && border.Child is StackPanel sp)
                {
                    foreach (var el in sp.Children)
                    {
                        if (el is CheckBox cb) cb.IsChecked = false;
                    }
                }
            }
        }

        // ── Window events ─────────────────────────────────

        protected override void OnMouseLeftButtonDown(System.Windows.Input.MouseButtonEventArgs e)
        {
            base.OnMouseLeftButtonDown(e);
            DragMove();
        }

        private void RamSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (RamLabel != null)
                RamLabel.Text = $"{(int)e.NewValue} GB";
        }
    }
}
