using System;
using System.IO;
using System.Windows;

namespace PWPLuncher
{
    public partial class App : Application
    {
        public App()
        {
            try
            {
                File.WriteAllText("app_debug.log", "[DEBUG] App() constructor started\n");

                DispatcherUnhandledException += (s, e) =>
                {
                    string log = $"[FATAL] {e.Exception.Message}\n{e.Exception.StackTrace}";
                    File.WriteAllText("crash.log", log);
                    e.Handled = true;
                };

                AppDomain.CurrentDomain.UnhandledException += (s, e) =>
                {
                    var ex = e.ExceptionObject as Exception;
                    File.WriteAllText("crash.log", $"[FATAL] {ex?.Message}\n{ex?.StackTrace}");
                };

                File.WriteAllText("app_debug.log", "[DEBUG] App() constructor done\n");
            }
            catch (Exception ex)
            {
                File.WriteAllText("crash.log", $"[FATAL] App ctor: {ex.Message}\n{ex.StackTrace}");
            }
        }
    }
}
