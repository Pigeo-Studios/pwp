package com.pigeostudios.pwp.warfare.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Отправка статуса игрока в лаунчер (http://127.0.0.1:42157/status) для Discord RPC.
// Лаунчер слушает этот порт и переводит RPC в состояния lobby/match/playing/idle.
// Все ошибки молча игнорируются: без лаунчера мод работает как раньше.
public class LauncherStatusReporter {
    private static final String ENDPOINT = "http://127.0.0.1:42157/status";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pwp-launcher-status");
        t.setDaemon(true);
        return t;
    });

    private static volatile String lastState = "";
    private static volatile String lastFaction = "";

    private LauncherStatusReporter() {}

    // Постит статус только при изменении состояния/фракции (синк идёт раз в секунду)
    public static void report(String state, String nickname, String faction) {
        String f = faction == null ? "" : faction;
        if (state.equals(lastState) && f.equals(lastFaction)) return;
        lastState = state;
        lastFaction = f;
        String payload = "{\"state\":\"" + jsonEscape(state)
                + "\",\"nickname\":\"" + jsonEscape(nickname == null ? "Player" : nickname)
                + "\",\"faction\":\"" + jsonEscape(f) + "\"}";
        EXECUTOR.execute(() -> post(payload));
    }

    // Вызывается из handleSyncGameData каждую секунду на любом PWP-сервере
    public static void onSyncGameData(boolean isGameStarted, String blueFaction, String redFaction,
                                      String myTeam, String nickname) {
        if (isGameStarted && myTeam != null
                && (myTeam.equalsIgnoreCase("Blue") || myTeam.equalsIgnoreCase("Red"))) {
            boolean blue = myTeam.equalsIgnoreCase("Blue");
            String my = blue ? blueFaction : redFaction;
            String enemy = blue ? redFaction : blueFaction;
            String myName = factionDisplay(my);
            String enemyName = factionDisplay(enemy);
            String factionText;
            if (myName != null && enemyName != null && !myName.equals(enemyName)) {
                factionText = myName + " против " + enemyName;
            } else if (myName != null) {
                factionText = myName;
            } else {
                factionText = blue ? "Синие" : "Красные";
            }
            report("match", nickname, factionText);
        } else {
            report("lobby", nickname, "");
        }
    }

    // Клиент отключился от сервера — возвращаем базовый статус лаунчера
    public static void onDisconnect(String nickname) {
        report("playing", nickname, "");
    }

    private static String factionDisplay(String f) {
        if (f == null || f.isEmpty() || "none".equalsIgnoreCase(f)) return null;
        return switch (f.toLowerCase()) {
            case "ukraine", "ukr" -> "Украина";
            case "russia", "russian" -> "Россия";
            case "usa", "us" -> "США";
            case "nato" -> "НАТО";
            case "insurgency", "insurgent", "insurgents" -> "Повстанцы";
            case "pmc" -> "ЧВК";
            case "bluefor" -> "Синие";
            case "redfor" -> "Красные";
            default -> f.toUpperCase();
        };
    }

    private static void post(String payload) {
        try {
            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            conn.getResponseCode(); // дождаться ответа, чтобы соединение завершилось
            conn.disconnect();
        } catch (Exception ignored) {
            // Лаунчер не запущен или порт занят — не критично
        }
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
