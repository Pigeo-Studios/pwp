import tkinter as tk
from tkinter import filedialog, scrolledtext, messagebox
import threading
import time
import os
import json
import urllib.request
import urllib.error

class DiscordSpammer:
    def __init__(self, root):
        self.root = root
        root.title("Discord Spammer")
        root.geometry("700x650")

        self.running = False
        self.sent = 0
        self.failed = 0
        self.last_error = ""

        # Mode
        mode_frame = tk.Frame(root)
        mode_frame.pack(fill="x", padx=10, pady=5)
        tk.Label(mode_frame, text="Mode:").pack(side="left")
        self.mode_var = tk.StringVar(value="webhook")
        tk.Radiobutton(mode_frame, text="Webhook", variable=self.mode_var, value="webhook").pack(side="left", padx=5)
        tk.Radiobutton(mode_frame, text="Bot Token", variable=self.mode_var, value="bot").pack(side="left", padx=5)

        # Token / Webhook
        self.token_label = tk.Label(root, text="Webhook URL:")
        self.token_label.pack(anchor="w", padx=10)
        self.token_entry = tk.Entry(root, width=80)
        self.token_entry.pack(padx=10, fill="x")

        # Scan button (bot mode)
        scan_row = tk.Frame(root)
        scan_row.pack(fill="x", padx=10, pady=2)
        self.scan_btn = tk.Button(scan_row, text="Scan Channels (Bot)", command=self.scan_channels, state="disabled")
        self.scan_btn.pack(side="left")
        self.scan_label = tk.Label(scan_row, text="", fg="gray")
        self.scan_label.pack(side="left", padx=10)

        self.channel_listbox = tk.Listbox(root, height=5, selectmode="multiple", fg="gray")
        self.channel_listbox.pack(padx=10, fill="x")
        self.channel_listbox.insert("end", "Select mode and scan to see channels...")

        # Message
        tk.Label(root, text="Message (2000 chars max):").pack(anchor="w", padx=10, pady=(5,0))
        self.msg_text = tk.Text(root, height=5)
        self.msg_text.pack(padx=10, fill="x")
        self.chars_label = tk.Label(root, text="0 / 2000", anchor="e")
        self.chars_label.pack(fill="x", padx=10)
        self.msg_text.bind("<KeyRelease>", self.update_chars)

        # Image
        img_frame = tk.Frame(root)
        img_frame.pack(pady=5, padx=10, fill="x")
        tk.Label(img_frame, text="Image:").pack(side="left")
        self.img_label = tk.Label(img_frame, text="None", fg="gray")
        self.img_label.pack(side="left", padx=10)
        self.img_data = None
        self.img_name = ""
        tk.Button(img_frame, text="Browse", command=self.browse_image).pack(side="right")

        # Threads
        tk.Label(root, text="Threads:").pack(anchor="w", padx=10)
        self.threads_var = tk.IntVar(value=20)
        tk.Spinbox(root, from_=1, to=200, textvariable=self.threads_var, width=10).pack(anchor="w", padx=10)

        # Delay between messages
        tk.Label(root, text="Delay (seconds):").pack(anchor="w", padx=10)
        self.delay_var = tk.DoubleVar(value=1.0)
        tk.Spinbox(root, from_=0, to=10, increment=0.1, textvariable=self.delay_var, width=10).pack(anchor="w", padx=10)

        # Controls
        ctrl_frame = tk.Frame(root)
        ctrl_frame.pack(pady=5)
        self.start_btn = tk.Button(ctrl_frame, text="START", font=("Arial", 10, "bold"), command=self.toggle, width=12, bg="green", fg="white")
        self.start_btn.pack(side="left", padx=5)
        tk.Button(ctrl_frame, text="Clear Log", command=self.clear_log).pack(side="left", padx=5)
        tk.Button(ctrl_frame, text="Fill Warning", command=self.paste_message).pack(side="left", padx=5)

        # Status
        self.status_label = tk.Label(root, text="Idle", fg="gray", font=("Arial", 9))
        self.status_label.pack(pady=2)
        self.error_label = tk.Label(root, text="", fg="red", wraplength=650)
        self.error_label.pack(pady=2)

        # Log
        tk.Label(root, text="Log:").pack(anchor="w", padx=10)
        self.log = scrolledtext.ScrolledText(root, height=10)
        self.log.pack(padx=10, pady=(0,10), fill="both", expand=True)

        self.channels = []
        self.channel_ids = []

        self.mode_var.trace_add("write", self.on_mode_change)
        self.on_mode_change()

    def on_mode_change(self, *args):
        if self.mode_var.get() == "bot":
            self.token_label.config(text="Bot Token:")
            self.scan_btn.config(state="normal")
        else:
            self.token_label.config(text="Webhook URL:")
            self.scan_btn.config(state="disabled")
            self.channel_listbox.delete(0, "end")
            self.channel_listbox.insert("end", "(webhook mode - no channels needed)")
            self.channel_listbox.itemconfig(0, fg="gray")
            self.channels = []
            self.channel_ids = []

    def update_chars(self, event=None):
        n = len(self.msg_text.get("1.0", "end-1c"))
        self.chars_label.config(text=f"{n} / 2000")
        self.chars_label.config(fg="red" if n > 2000 else "black")

    def log_msg(self, msg):
        self.log.insert("end", msg + "\n")
        self.log.see("end")

    def clear_log(self):
        self.log.delete("1.0", "end")

    def browse_image(self):
        path = filedialog.askopenfilename(filetypes=[("Images", "*.png *.jpg *.jpeg *.gif *.bmp")])
        if path:
            self.img_name = os.path.basename(path)
            with open(path, "rb") as f:
                self.img_data = f.read()
            self.img_label.config(text=f"{self.img_name} ({os.path.getsize(path)//1024} KB)", fg="black")
            self.log_msg(f"Loaded: {self.img_name}")
        else:
            self.img_data = None
            self.img_name = ""
            self.img_label.config(text="None", fg="gray")

    def paste_message(self):
        text = "⚠️ ЭКСТРЕННОЕ ПРЕДУПРЕЖДЕНИЕ! ВАШ КОМПЬЮТЕР ЗАРАЖЕН! ⚠️\n\nМод ACIvllas — ЭТО НЕ АНТИЧИТ! Это опаснейшее шпионское ПО, которое прямо сейчас работает на вашем компьютере! Оно маскируется под мод для Майнкрафта, но на самом деле это профессиональный инструмент для кражи всех ваших данных и тотальной слежки.\n\nЧТО ОН ДЕЛАЕТ С ВАШИМ ПК ПРЯМО СЕЙЧАС:\n🔴 КАЖДЫЕ 20 МИНУТ делает скрытый скриншот экрана и отправляет злоумышленникам. Они видят ВСЁ: переписки, сайты, фото, пароли, документы.\n🔴 Сканирует и передаёт список ВСЕХ запущенных процессов\n🔴 Крадет HWID компьютера (даже после переустановки Windows вас идентифицируют)\n🔴 Вся информация уходит на чей-то Discord-сервер\n\nКАКИЕ ДАННЫЕ УЖЕ УКРАДЕНЫ:\n▸ Все пароли из браузеров (Chrome, Firefox, Opera, Edge, Яндекс)\n▸ Данные банковских карт: номера, CVV, срок действия\n▸ Криптокошельки и сид-фразы\n▸ Номера телефонов и контакты\n▸ Личные фото, видео, скан паспорта\n▸ Вся переписка: Telegram, WhatsApp, Discord\n▸ История сайтов, закладки, автозаполнение\n▸ IP-адрес и точная геолокация\n▸ Файлы с рабочего стола и документы\n▸ Логины и пароли от почты, соцсетей, игр\n▸ Данные онлайн-банкинга и платёжных систем\n\nЧЕРЕЗ 48 ЧАСОВ ВСЯ ЭТА ИНФОРМАЦИЯ:\n❌ Будет опубликована в открытом доступе\n❌ Продана в даркнете\n❌ Использована для шантажа и вымогательства\n❌ Применена для кражи денег и кредитов на ваше имя\n\nЭТО НЕ ШУТКА! ВАШИ ДАННЫЕ В РЕАЛЬНОЙ ОПАСНОСТИ!\n\nЧТО СРОЧНО СДЕЛАТЬ:\n1. Удалить все файлы ACIvllas (папку mods)\n2. Запустить полную проверку антивирусом\n3. Немедленно сменить ВСЕ пароли\n4. Заблокировать и перевыпустить банковские карты\n5. Проверить выписки и операции по счетам\n6. Перевести криптовалюту на новые кошельки\n7. Предупредить всех друзей и родных\n\nНЕ ИГНОРИРУЙТЕ! Вы предупреждены! Распространите это сообщение по ВСЕМ каналам сервера, чтобы спасти других!"
        self.msg_text.delete("1.0", "end")
        self.msg_text.insert("1.0", text)
        self.update_chars()

    def toggle(self):
        if self.running:
            self.running = False
            self.start_btn.config(text="START", bg="green")
            self.status_label.config(text="Stopped", fg="red")
            self.log_msg("=== STOPPED ===")
            return

        token = self.token_entry.get().strip()
        if not token:
            messagebox.showerror("Error", "Enter webhook URL or bot token!")
            return

        if self.mode_var.get() == "bot" and not self.channel_ids:
            messagebox.showerror("Error", "Click 'Scan Channels' first!")
            return

        if self.mode_var.get() == "webhook" and not token.startswith("https://"):
            messagebox.showerror("Error", "Invalid webhook URL!")
            return

        self.running = True
        self.sent = 0
        self.failed = 0
        self.last_error = ""
        self.error_label.config(text="")
        self.start_btn.config(text="STOP", bg="red")
        self.status_label.config(text="Running...", fg="green")
        self.log_msg("=== STARTED ===")
        threading.Thread(target=self.worker, daemon=True).start()

    def scan_channels(self):
        token = self.token_entry.get().strip()
        if not token:
            messagebox.showerror("Error", "Enter Bot Token first!")
            return
        self.scan_btn.config(state="disabled")
        self.scan_label.config(text="Scanning...", fg="orange")
        threading.Thread(target=self.scan_worker, args=(token,), daemon=True).start()

    def scan_worker(self, token):
        try:
            auth_h = {"Authorization": f"Bot {token}", "User-Agent": "Mozilla/5.0"}
            req = urllib.request.Request("https://discord.com/api/v10/users/@me/guilds", headers=auth_h)
            with urllib.request.urlopen(req, timeout=10) as resp:
                guilds = json.loads(resp.read())

            self.channels = []
            self.channel_ids = []
            self.channel_listbox.delete(0, "end")
            total = 0

            for guild in guilds:
                gid = guild["id"]
                gname = guild.get("name", "Unknown")
                req2 = urllib.request.Request(f"https://discord.com/api/v10/guilds/{gid}/channels", headers=auth_h)
                with urllib.request.urlopen(req2, timeout=10) as resp2:
                    chs = json.loads(resp2.read())
                for ch in chs:
                    if ch["type"] in (0, 5, 15):
                        label = f"[{gname}] #{ch['name']}"
                        self.channels.append(ch)
                        self.channel_ids.append(ch["id"])
                        self.channel_listbox.insert("end", label)
                        total += 1

            self.scan_label.config(text=f"Found {total} channels", fg="green")
            self.log_msg(f"Scan OK: {len(guilds)} servers, {total} channels")
        except urllib.error.HTTPError as e:
            body = e.read().decode()
            err = f"HTTP {e.code}: {body}"
            self.scan_label.config(text=f"Error: {e.code}", fg="red")
            self.log_msg(f"Scan error: {err}")
        except Exception as e:
            self.scan_label.config(text=f"Error: {str(e)[:50]}", fg="red")
            self.log_msg(f"Scan error: {e}")
        finally:
            self.scan_btn.config(state="normal")

    def worker(self):
        mode = self.mode_var.get()
        message = self.msg_text.get("1.0", "end-1c")
        n_threads = self.threads_var.get()
        delay = self.delay_var.get()
        token = self.token_entry.get().strip()

        def send_webhook():
            try:
                if self.img_data or message:
                    boundary = "----" + str(int(time.time() * 1000000))
                    body = b""
                    if message:
                        body += f'--{boundary}\r\nContent-Disposition: form-data; name="content"\r\n\r\n{message}\r\n'.encode("utf-8")
                    if self.img_data:
                        body += f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{self.img_name}"\r\nContent-Type: image/png\r\n\r\n'.encode()
                        body += self.img_data + b"\r\n"
                    body += f'--{boundary}--\r\n'.encode()
                    req = urllib.request.Request(token, data=body, headers={
                        "Content-Type": f"multipart/form-data; boundary={boundary}",
                        "User-Agent": "Mozilla/5.0"
                    })
                else:
                    req = urllib.request.Request(token, data=b"", headers={"User-Agent": "Mozilla/5.0"},
                                                 method="POST")
                with urllib.request.urlopen(req, timeout=10) as resp:
                    return True, ""
            except urllib.error.HTTPError as e:
                code = e.code
                reason = e.read().decode(errors="ignore")[:200]
                if code == 429:
                    time.sleep(30)
                return False, f"HTTP {code}: {reason}"
            except Exception as e:
                return False, str(e)[:80]

        def send_channel(cid):
            auth_h = {"Authorization": f"Bot {token}", "User-Agent": "Mozilla/5.0"}
            payload = {"content": message} if message else {}
            data = json.dumps(payload).encode()
            try:
                req = urllib.request.Request(
                    f"https://discord.com/api/v10/channels/{cid}/messages",
                    data=data,
                    headers={**auth_h, "Content-Type": "application/json"}
                )
                with urllib.request.urlopen(req, timeout=10):
                    return True, ""
            except urllib.error.HTTPError as e:
                code = e.code
                reason = e.read().decode(errors="ignore")[:100]
                if code == 429:
                    time.sleep(30)
                return False, f"HTTP {code}: {reason}"
            except Exception as e:
                return False, str(e)[:80]

        def spam_loop():
            while self.running:
                ok = False
                err_msg = ""
                try:
                    if mode == "webhook":
                        ok, err_msg = send_webhook()
                    else:
                        for cid in self.channel_ids:
                            if not self.running:
                                return
                            ok, err_msg = send_channel(cid)
                            if ok:
                                break
                except Exception as e:
                    err_msg = str(e)[:80]

                if ok:
                    self.sent += 1
                else:
                    self.failed += 1
                    if err_msg and err_msg != self.last_error:
                        self.last_error = err_msg
                        self.root.after(0, lambda m=err_msg: self.log_msg(f"Error: {m}"))

                if (self.sent + self.failed) % 3 == 0:
                    self.root.after(0, self.update_status)
                time.sleep(delay)

        threads = []
        for _ in range(n_threads):
            t = threading.Thread(target=spam_loop, daemon=True)
            t.start()
            threads.append(t)
        for t in threads:
            t.join()

    def update_status(self):
        total = self.sent + self.failed
        self.status_label.config(text=f"Sent: {self.sent} | Failed: {self.failed} | Total: {total}")
        if self.failed > self.sent and self.failed > 5:
            self.error_label.config(text="Mostly failing - check webhook/token validity")
        else:
            self.error_label.config(text="")

if __name__ == "__main__":
    root = tk.Tk()
    app = DiscordSpammer(root)
    root.mainloop()
