package com.pwp.coreclient.gui.devtest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.gui.screens.PWPDisconnectedScreen;
import com.pwp.coreclient.gui.screens.PWPLoadingScreen;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.screens.PWPMainMenuScreen;
import com.pwp.coreclient.gui.screens.PWPSelectionScreen;
import com.pwp.coreclient.gui.screens.tabs.LobbyTab;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.LobbyStatePacket;
import com.pwp.coreclient.ui.UIDraw;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;

/**
 * Фикстуры экранов pwp-core-client для тест-стенда UI. Учитывают переключатели галереи:
 * {@link UiGallery#longStrings()} — максимально длинные ru/uk строки, {@link UiGallery#emptyData()}
 * — пустые списки. Только данные для экранов: никаких пакетов и сетевых вызовов.
 */
public final class CoreUiFixtures {
    private CoreUiFixtures() {}

    private static final String LONG_RU =
            "Северо-западный промышленный район «Заводской узел №3» с затопленными карьерами и железнодорожной развязкой";
    private static final String LONG_UK =
            "Північно-західний промисловий район «Заводський вузол №3» із затопленими кар'єрами та залізничною розв'язкою";

    private static boolean longs() {
        return UiGallery.longStrings();
    }

    private static boolean empty() {
        return UiGallery.emptyData();
    }

    private static String name(String normal, int i) {
        return longs() ? (i % 2 == 0 ? LONG_RU : LONG_UK) : normal;
    }

    public static void registerAll() {
        UiGallery.register("core.main_menu", PWPMainMenuScreen::new);
        UiGallery.register("core.loading.connecting", () -> new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING));
        UiGallery.register("core.loading.world", () -> new PWPLoadingScreen(PWPLoadingScreen.Context.LOADING_WORLD));
        UiGallery.register(
                "core.loading.transferring", () -> new PWPLoadingScreen(PWPLoadingScreen.Context.TRANSFERRING));
        UiGallery.register("core.loading.progress", () -> {
            PWPLoadingScreen s = new PWPLoadingScreen(PWPLoadingScreen.Context.LOADING_MAP);
            s.setProgress(0.62F);
            return s;
        });
        UiGallery.register("core.loading.error", () -> {
            PWPLoadingScreen s = new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING);
            s.setError("preview");
            return s;
        });
        UiGallery.register("vanilla.level_loading", () -> new LevelLoadingScreen(new StoringChunkProgressListener(11)));
        UiGallery.register("core.disconnect.short", () -> new PWPDisconnectedScreen(Component.literal("Timed out")));
        UiGallery.register("core.disconnect.long", () -> new PWPDisconnectedScreen(Component.literal(longReason())));
        UiGallery.register(
                "core.disconnect.colored",
                () -> new PWPDisconnectedScreen(Component.empty()
                        .append(Component.literal("Вы были кикнуты: ").withStyle(ChatFormatting.RED))
                        .append(Component.literal("подозрение на сторонний клиент. ")
                                .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("Обратитесь в Discord-поддержку PWP.")
                                .withStyle(ChatFormatting.GRAY))));
        UiGallery.register("core.disconnect.empty", () -> new PWPDisconnectedScreen(Component.empty()));

        UiGallery.register("core.lobby.vote_map", () -> lobby(voteMap(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.vote_mode", () -> lobby(voteMode(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.vote_faction", () -> lobby(voteFaction(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.vote_finished", () -> lobby(finished(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.idle", () -> lobby(idle(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.match", () -> lobby(match(), LobbyTab.GOLOSOVANIE));
        UiGallery.register("core.lobby.play", () -> lobby(match(), LobbyTab.IGRA));
        UiGallery.register("core.lobby.leaders", () -> lobby(idle(), LobbyTab.LIDERY));
        UiGallery.register("core.lobby.profile", () -> lobby(idle(), LobbyTab.PROFIL));
        UiGallery.register("core.selection", CoreUiFixtures::selection);
    }

    // ================================================================ лобби

    private static Screen lobby(LobbyStatePacket state, LobbyTab tab) {
        Supplier<JsonObject> leaders = () -> empty() ? emptyLeaderboard() : leaderboard();
        return PWPLobbyScreen.preview(state, leaders, uuid -> empty() ? null : profile(), tab);
    }

    private static LobbyStatePacket voteMap() {
        LobbyStateFixture f = new LobbyStateFixture();
        f.phase = LobbyStatePacket.PHASE_MAP_VOTE;
        f.leadingName = name("Такмачка", 0);
        if (!empty()) {
            int n = longs() ? 6 : 4;
            f.mapNames = new String[n];
            f.mapDisplayNames = new String[n];
            f.mapDescriptions = new String[n];
            f.maxPlayers = new int[n];
            f.voteCounts = new int[n];
            f.worldPaths = new String[n];
            f.blueFactions = new String[n];
            f.redFactions = new String[n];
            String[] names = {"Такмачка", "Порт Северный", "Каньон", "Завод"};
            String[][] sides = {{"ukraine", "russia"}, {"usa", "russia"}, {"nato", "insurgency"}, {"pmc", "russia"}};
            for (int i = 0; i < n; i++) {
                f.mapNames[i] = "map" + i;
                f.mapDisplayNames[i] = name(names[i % names.length], i);
                f.mapDescriptions[i] = longs() ? LONG_RU + ". " + LONG_UK : "Городские бои, 5 точек захвата";
                f.maxPlayers[i] = 80;
                f.voteCounts[i] = new int[] {5, 3, 999, 0, 2, 1}[i];
                f.worldPaths[i] = "";
                f.blueFactions[i] = sides[i % 4][0];
                f.redFactions[i] = sides[i % 4][1];
            }
            f.myMapVote = 0;
        }
        return f.build();
    }

    private static LobbyStatePacket voteMode() {
        LobbyStateFixture f = new LobbyStateFixture();
        f.phase = LobbyStatePacket.PHASE_MODE_VOTE;
        f.remainingSeconds = 8; // «срочный» таймер
        if (!empty()) {
            f.modeNames = new String[] {"aas", "invasion"};
            f.modeDisplayNames = new String[] {name("Advance and Secure", 0), name("Invasion", 1)};
            f.modeDescriptions = new String[] {
                longs() ? LONG_RU : "Обе команды захватывают точки по порядку",
                longs() ? LONG_UK : "Атакующие захватывают, защитники держат"
            };
            f.modeVoteCounts = new int[] {7, 4};
            f.myModeVote = 1;
        }
        return f.build();
    }

    private static LobbyStatePacket voteFaction() {
        LobbyStateFixture f = new LobbyStateFixture();
        f.phase = LobbyStatePacket.PHASE_FACTION_VOTE;
        f.mode = "invasion";
        f.team1Factions = empty() ? new String[] {"", "", ""} : new String[] {"usa", "ukraine", "nato"};
        f.team2Factions = empty() ? new String[] {"", "", ""} : new String[] {"russia", "insurgency", "pmc"};
        f.team1Votes = new int[] {6, 3, 0};
        f.team2Votes = new int[] {2, 999, 1};
        f.myFaction1 = empty() ? -1 : 1;
        f.myFaction2 = empty() ? -1 : 0;
        return f.build();
    }

    private static LobbyStatePacket finished() {
        LobbyStateFixture f = new LobbyStateFixture();
        f.resultMap = name("Такмачка", 0);
        f.resultMapVotes = 12;
        f.resultMode = "aas";
        f.resultModeVotes = 9;
        f.resultF1 = "ukraine";
        f.resultF1Votes = 7;
        f.resultF2 = "russia";
        f.resultF2Votes = 5;
        return f.build();
    }

    private static LobbyStatePacket idle() {
        LobbyStateFixture f = new LobbyStateFixture();
        if (longs()) {
            f.canStartNewMatch = false;
            f.policyReason = LONG_RU + ". " + LONG_UK;
        }
        return f.build();
    }

    private static LobbyStatePacket match() {
        LobbyStateFixture f = new LobbyStateFixture();
        f.phase = LobbyStatePacket.PHASE_MATCH_PLAYING;
        f.matchMapDisplay = name("Такмачка", 0);
        f.matchModeDisplay = name("Advance and Secure", 1);
        f.matchBlueFaction = "ukraine";
        f.matchRedFaction = "russia";
        f.matchBlueTickets = 214;
        f.matchRedTickets = 187;
        f.matchServerId = 7;
        f.matchElapsed = 1385;
        f.matchPlayers = 63;
        f.matchMaxPlayers = 80;
        if (!empty()) {
            int n = longs() ? 6 : 3;
            f.mServerIds = new int[n];
            f.mDisplayNames = new String[n];
            f.mStatuses = new String[n];
            f.mBlueTickets = new int[n];
            f.mRedTickets = new int[n];
            f.mPlayers = new int[n];
            f.mMaxPlayers = new int[n];
            f.mElapsed = new int[n];
            f.mBlueFactions = new String[n];
            f.mRedFactions = new String[n];
            f.mWorldPaths = new String[n];
            String[] st = {"PLAYING", "STARTING", "FULL", "VOTING", "FINISHED", "CLOSED"};
            for (int i = 0; i < n; i++) {
                f.mServerIds[i] = i == 3 ? -1 : 10 + i;
                f.mDisplayNames[i] = name("Порт Северный " + (i + 1), i);
                f.mStatuses[i] = st[i % st.length];
                f.mBlueTickets[i] = 300 - i * 40;
                f.mRedTickets[i] = 250 - i * 30;
                f.mPlayers[i] = 40 + i * 7;
                f.mMaxPlayers[i] = 80;
                f.mElapsed[i] = 60 * (i + 3) + 17;
                f.mBlueFactions[i] = "usa";
                f.mRedFactions[i] = "russia";
                f.mWorldPaths[i] = "";
            }
        } else {
            f.phase = LobbyStatePacket.PHASE_IDLE; // без активного матча — пустая вкладка «Игра»
        }
        return f.build();
    }

    private static JsonObject leaderboard() {
        JsonObject root = new JsonObject();
        JsonArray players = new JsonArray();
        String[] nicks = {"Pigeon_Commander", "Sniper_UA", "Tankist", "MedicMain", "xX_Breacher_Xx", "Ghost"};
        for (int i = 0; i < 40; i++) {
            JsonObject p = new JsonObject();
            JsonObject player = new JsonObject();
            player.addProperty("nickname", longs() ? "Очень_Длинный_Ник_Игрока_" + i : nicks[i % nicks.length] + i);
            player.addProperty("uuid", "00000000-0000-0000-0000-0000000000" + String.format("%02d", i));
            JsonObject s = new JsonObject();
            s.addProperty("kills", 900 - i * 17);
            s.addProperty("deaths", 300 + i * 3);
            s.addProperty("wins", 80 - i);
            s.addProperty("losses", 30 + i);
            s.addProperty("captures", 120 - i * 2);
            s.addProperty("revives", 40 + i);
            s.addProperty("vehicleKills", 12 + i % 5);
            s.addProperty("damageDealt", 1_234_567 - i * 20_000);
            s.addProperty("healingDone", 54_000 - i * 900);
            s.addProperty("playtimeSeconds", 360_000 - i * 5000);
            p.add("player", player);
            p.add("stats", s);
            players.add(p);
        }
        root.add("players", players);
        return root;
    }

    private static JsonObject emptyLeaderboard() {
        JsonObject root = new JsonObject();
        root.add("players", new JsonArray());
        return root;
    }

    private static JsonObject profile() {
        JsonObject root = new JsonObject();
        JsonObject player = new JsonObject();
        player.addProperty("nickname", longs() ? "Очень_Длинный_Ник_Игрока_Для_Проверки" : "Pigeon_Commander");
        root.add("player", player);
        root.addProperty("level", 17);
        JsonObject s = new JsonObject();
        String[] keys = {
            "kills",
            "deaths",
            "revives",
            "vehicleKills",
            "captures",
            "bestKillStreak",
            "teamKills",
            "vehiclesDestroyed",
            "airVehiclesDestroyed",
            "damageDealt",
            "healingDone",
            "suppliesDelivered",
            "matchesPlayed",
            "wins",
            "losses",
            "playtimeSeconds",
            "bestWinStreak",
            "currentWinStreak"
        };
        int[] vals = {1532, 611, 214, 37, 188, 23, 4, 51, 9, 2_345_678, 98_765, 1203, 144, 81, 63, 512_340, 11, 3};
        for (int i = 0; i < keys.length; i++) s.addProperty(keys[i], vals[i]);
        s.addProperty("longestKill", 1245.6);
        root.add("stats", s);
        return root;
    }

    private static String longReason() {
        return "Соединение с сервером матча было потеряно: превышено время ожидания ответа от сервера. "
                + "Возможные причины: нестабильное подключение к интернету, перезапуск сервера матча, "
                + "блокировка порта брандмауэром или антивирусом, слишком высокая задержка до узла "
                + "pigeo.asuscomm.com. Попробуйте переподключиться через главное меню. "
                + LONG_RU + ". " + LONG_UK;
    }

    // ================================================================ выбор команды (core-версия)

    private static Screen selection() {
        List<String> items = empty() ? List.of() : List.of("BLUE", "RED", "SPECTATOR");
        return new PWPSelectionScreen(
                longs() ? LONG_RU : "ВЫБОР КОМАНДЫ",
                longs() ? LONG_UK : "Выберите сторону, за которую будете сражаться",
                items,
                (g, si) -> {
                    var font = PWPTheme.Fonts.display();
                    Component label = UIDraw.ellipsize(font, Component.literal(si.label), si.w - 8);
                    UIDraw.centered(g, font, label, si.x + si.w / 2, si.y + si.h / 2, PWPTheme.Text.HEADING);
                },
                i -> UiGallery.openGallery(),
                30,
                160,
                140,
                PWPTheme.Spacing.XL,
                i -> i == 2,
                false);
    }
}
