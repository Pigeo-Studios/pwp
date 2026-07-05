using System.Windows;
using System.Windows.Controls;

namespace PWPLuncher
{
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
        }

        private void BtnMinimize_Click(object sender, RoutedEventArgs e)
        {
            WindowState = WindowState.Minimized;
        }

        private void BtnClose_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void RamSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (RamLabel != null)
                RamLabel.Text = $"{(int)e.NewValue} GB";
        }

        private void BtnBrowse_Click(object sender, RoutedEventArgs e)
        {
            // Will be implemented with folder picker in full version
            PlaySubtext.Text = "Выбор папки...";
        }

        private async void BtnPlay_Click(object sender, RoutedEventArgs e)
        {
            BtnPlay.IsEnabled = false;
            PlaySubtext.Text = "Запуск...";
            StatusText.Text = "Загрузка...";
            StatusDot.Fill = (System.Windows.Media.SolidColorBrush)FindResource("BrushAccent");

            for (int i = 0; i <= 100; i += 5)
            {
                ProgressBar.Value = i;
                ProgressText.Text = $"Подготовка... {i}%";
                await Task.Delay(30);
            }

            StatusText.Text = "Готов к запуску";
            StatusDot.Fill = (System.Windows.Media.SolidColorBrush)FindResource("BrushGreen");
            PlaySubtext.Text = "Нажмите чтобы начать";
            BtnPlay.IsEnabled = true;
        }

        protected override void OnMouseLeftButtonDown(System.Windows.Input.MouseButtonEventArgs e)
        {
            base.OnMouseLeftButtonDown(e);
            DragMove();
        }
    }
}
