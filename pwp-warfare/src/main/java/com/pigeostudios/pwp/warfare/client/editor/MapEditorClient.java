package com.pigeostudios.pwp.warfare.client.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.admin.MapEditorPackets;
import com.pwp.coreclient.admin.AdminBackend;
import com.pwp.coreclient.admin.AdminPanel;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Клиентская обвязка редактора карты: вход из паузы/админки, разбор ответов сервера. */
@OnlyIn(Dist.CLIENT)
public final class MapEditorClient {
    private static final Logger LOG = LoggerFactory.getLogger(MapEditorClient.class);
    private static boolean forceNext;

    private MapEditorClient() {}

    /** ClientModEvents.onClientSetup. */
    public static void init() {
        AdminPanel.setMapEditor(parent -> new MapEditorScreen(parent, AdminBackend.NETWORK));
        ClientDataResponses.listen(MapEditorClient::onData);
    }

    /** «Сбросить правки»: следующий ответ {@code mapEditor} заменит черновик даже несохранённый. */
    static void forceNextLoad() {
        forceNext = true;
    }

    private static void onData(String type, String json) {
        if (MapEditorPackets.DATA_EDITOR.equals(type)) {
            MapEditorSession.onEditorData(json, forceNext);
            forceNext = false;
            refreshScreen();
        } else if (MapEditorPackets.DATA_VERSIONS.equals(type)) {
            MapEditorSession.onVersions(json);
        } else if (DataRequests.ACTION_RESULT.equals(type)) {
            onResult(json);
        }
    }

    private static void onResult(String json) {
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            LOG.warn("Map editor: bad actionResult", e);
            return;
        }
        String action = o.has("action") ? o.get("action").getAsString() : "";
        if (!action.startsWith("mapEditor.")) return;
        boolean ok = o.has("success") && o.get("success").getAsBoolean();
        String err =
                o.has("error") && !o.get("error").isJsonNull() ? o.get("error").getAsString() : "";
        if (ok) {
            MapEditorSession.markSent();
            MapEditorScreen.status(
                    Component.translatable("pwpwarfare.editor.ok." + action.substring("mapEditor.".length())),
                    PWPTheme.Text.ACCENT);
        } else {
            Component text = I18n.exists(err) ? Component.translatable(err) : Component.literal(err);
            MapEditorScreen.status(Component.translatable("pwpwarfare.editor.failed", text), PWPTheme.Text.DANGER);
        }
        // свежие флаги (правка матча/версия) и список версий
        AdminBackend.NETWORK.request(MapEditorPackets.DATA_VERSIONS, "");
    }

    private static void refreshScreen() {
        if (Minecraft.getInstance().screen instanceof MapEditorScreen s) s.onServerData();
    }
}
