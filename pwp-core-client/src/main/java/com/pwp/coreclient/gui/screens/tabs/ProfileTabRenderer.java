package com.pwp.coreclient.gui.screens.tabs;

import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.PlayerData.CachedProfile;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.PacketDataRequest;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ProfileTabRenderer {

    public enum State { LOADING, LOADED }
    private State state = State.LOADING;
    private JsonObject profileData;
    private String currentUuid;

    public void requestProfile(String uuid) {
        currentUuid = uuid;
        state = State.LOADING;
        PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("profile", ""));
    }

    public void tick() {
        if (state != State.LOADING || currentUuid == null) return;
        CachedProfile cp = PlayerData.get(java.util.UUID.fromString(currentUuid));
        if (cp == null || cp.data == null) return;
        profileData = cp.data;
        state = State.LOADED;
    }

    public void render(GuiGraphics g, int sw, int sh, int mx, int my) {
        var f = PWPTheme.Fonts.display();

        if (state == State.LOADING) {
            g.drawString(f, Component.literal("ПРОФИЛЬ"), sw/2-f.width("ПРОФИЛЬ")/2, 80, PWPTheme.Colors.TEXT_ACCENT, false);
            g.fill(sw/2-40, 94, sw/2+40, 95, PWPTheme.Colors.ACCENT);
            String msg = "Загрузка...";
            g.drawString(f, Component.literal(msg), sw/2-f.width(msg)/2, sh/2, PWPTheme.Colors.TEXT_DIM, false);
            return;
        }

        if (profileData == null) return;

        String name = jsonStr(jsonObj(profileData, "player"), "nickname", "—");
        int lvl = jsonInt(profileData, "level", 1);
        JsonObject st = profileData.has("stats") ? profileData.getAsJsonObject("stats") : profileData;
        int kills = jsonInt(st, "kills", 0);
        int deaths = jsonInt(st, "deaths", 0);
        int revives = jsonInt(st, "revives", 0);
        int vk = jsonInt(st, "vehicleKills", 0);
        int cap = jsonInt(st, "captures", 0);
        int bst = jsonInt(st, "bestKillStreak", 0);
        int tk = jsonInt(st, "teamKills", 0);
        int vd = jsonInt(st, "vehiclesDestroyed", 0);
        int ad = jsonInt(st, "airVehiclesDestroyed", 0);
        int dmg = jsonInt(st, "damageDealt", 0);
        int heal = jsonInt(st, "healingDone", 0);
        int sup = jsonInt(st, "suppliesDelivered", 0);
        double longest = jsonDouble(st, "longestKill", 0);
        int matches = jsonInt(st, "matchesPlayed", 0);
        int wins = jsonInt(st, "wins", 0);
        int losses = jsonInt(st, "losses", 0);
        int pt = jsonInt(st, "playtimeSeconds", 0);
        int bws = jsonInt(st, "bestWinStreak", 0);
        int cws = jsonInt(st, "currentWinStreak", 0);

        g.drawString(f, Component.literal("ПРОФИЛЬ"), sw/2-f.width("ПРОФИЛЬ")/2, 80, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2-40, 94, sw/2+40, 95, PWPTheme.Colors.ACCENT);
        g.drawString(f, Component.literal(name + "  |  Ур. " + lvl), sw/2-f.width(name + "  |  Ур. " + lvl)/2, 108, PWPTheme.Colors.TEXT_PRIMARY, false);

        int x1 = 40, x2 = sw/2+6, cw = sw/2-46;
        drawPanel(g, x1, 130, cw, "БОЙ", new String[][]{
            {"Убийства",""+kills}, {"Смерти",""+deaths},
            {"K/D",String.format("%.2f", deaths>0 ? kills/(double)deaths : (double)kills)}, {"Спасения",""+revives},
            {"Убито техникой",""+vk}, {"Захваты",""+cap},
            {"Лучшая серия",""+bst}, {"Тимкиллы",""+tk},
        });
        drawPanel(g, x2, 130, cw, "МАТЧИ", new String[][]{
            {"Сыграно",""+matches}, {"Побед",""+wins},
            {"Поражений",""+losses}, {"WinRate",matches>0 ? String.format("%.0f%%", wins*100f/matches) : "0%"},
            {"Наиграно",fmtTime(pt)}, {"Убийств/матч",matches>0 ? String.format("%.1f", kills/(double)matches) : "0"},
            {"Макс. побед",""+bws}, {"Тек. побед",""+cws},
        });
        drawPanel(g, x1, 130+getPH(8), cw, "ТЕХНИКА", new String[][]{
            {"Уничтожено",""+vd}, {"Сбито авиации",""+ad},
        });
        drawPanel(g, x1, 130+getPH(8)+getPH(2)+8, cw, "ПОДДЕРЖКА", new String[][]{
            {"Урон",String.format("%,.0f", (double)dmg)}, {"Лечение",String.format("%,.0f", (double)heal)},
            {"Припасы",""+sup}, {"Макс. дист.",String.format("%.0fм", longest)},
        });
    }

    private int getPH(int rows) { return 25 + rows*12 + 6; }

    private void drawPanel(GuiGraphics g, int x, int y, int w, String title, String[][] rows) {
        int rh = 12, h = 25 + rows.length*rh + 6, halfW = w/2 - 8;
        PWPPanel.renderWithTitle(g, x, y, w, h, title, PWPTheme.Colors.TEXT_ACCENT);
        int ly = y + 25;
        for (int r = 0; r < rows.length; r += 2) {
            if ((r/2) % 2 == 1) g.fill(x+8, ly, w-16, rh, 0x08000000);
            row(g, rows[r][0], rows[r][1], x+10, ly, halfW);
            if (r+1 < rows.length) row(g, rows[r+1][0], rows[r+1][1], x+10+halfW+8, ly, halfW);
            ly += rh;
        }
    }

    private void row(GuiGraphics g, String label, String val, int x, int y, int maxRw) {
        var f = PWPTheme.Fonts.display();
        g.drawString(f, Component.literal(label+":"), x, y, PWPTheme.Colors.TEXT_DIM, false);
        int valX = x+maxRw-4-f.width(val);
        if (valX < x+f.width(label)+12) valX = x+f.width(label)+12;
        g.drawString(f, Component.literal(val), valX, y, PWPTheme.Colors.TEXT_PRIMARY, false);
    }

    private static String fmtTime(int s) {
        int h = s/3600;
        if (h >= 24) return (h/24)+"д "+(h%24)+"ч";
        return h+"ч "+(s%3600/60)+"м";
    }

    private static int jsonInt(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }
    private static double jsonDouble(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
    private static String jsonStr(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
    private static JsonObject jsonObj(JsonObject o, String k) { return o.has(k) ? o.getAsJsonObject(k) : new JsonObject(); }
}
