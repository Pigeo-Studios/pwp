package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.server.VehicleActionService;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Радиальное меню техники (этап 17a, как в Squad): клавиша «Действия с техникой» (по умолчанию J) в машине или
 * глядя на неё. Пункты присылает сервер ({@link VehicleActionService#DATA_TYPE}): закрепить за отрядом, «только мой
 * отряд», открепить, запросить вождение, разрешить/отказать просившим. Клик шлёт только id машины и действие.
 */
@OnlyIn(Dist.CLIENT)
public class VehicleRadialScreen extends WarfareRadialScreen {
    private static final Logger LOG = LoggerFactory.getLogger(VehicleRadialScreen.class);
    private static final int MAX_REQUESTS = 3;

    private final int entityId;
    private JsonObject state;

    public VehicleRadialScreen(int entityId) {
        super(Component.translatable("gui.pwpwarfare.vehicle_menu.title"));
        this.entityId = entityId;
    }

    /** Клавиша и ответы сервера — один раз при старте клиента. */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(VehicleRadialScreen::onKey);
        ClientDataResponses.listen((type, json) -> {
            if (!VehicleActionService.DATA_TYPE.equals(type)) return;
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof VehicleRadialScreen s
                            && o.has("entityId")
                            && o.get("entityId").getAsInt() == s.entityId) {
                        s.state = o;
                    }
                });
            } catch (RuntimeException e) {
                LOG.warn("Меню техники: битый ответ", e);
            }
        });
    }

    private static void onKey(InputEvent.Key e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getAction() != 1 || mc.screen != null || mc.player == null) return;
        if (!ModKeyBindings.VEHICLE_MENU_KEY.matches(e.getKey(), e.getScanCode())) return;
        Entity target = mc.player.getVehicle();
        if (target == null && mc.hitResult instanceof EntityHitResult hit) target = hit.getEntity();
        if (target == null) return;
        mc.setScreen(new VehicleRadialScreen(target.getRootVehicle().getId()));
    }

    @Override
    protected void init() {
        super.init();
        JsonObject q = new JsonObject();
        q.addProperty("entityId", entityId);
        com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(
                new DataRequest(VehicleActionService.DATA_TYPE, q.toString()));
    }

    private Runnable act(String action, String target) {
        return () -> {
            PacketHandler.INSTANCE.sendToServer(new VehicleActionService.Act(entityId, action, target));
            onClose();
        };
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("gui.pwpwarfare.vehicle_menu." + key, args);
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        if (state == null) {
            out.add(new Entry(t("loading"), WarfareIcon.CLOCK, null, WTheme.HUD_VALUE, false, null));
            return out;
        }
        JsonArray actions = state.getAsJsonArray("actions");
        JsonArray requests = state.getAsJsonArray("requests");
        for (var el : actions) {
            switch (el.getAsString()) {
                case "claim" -> out.add(Entry.of(t("claim"), WarfareIcon.SQUAD, act("claim", "")));
                case "lock" -> out.add(
                        Entry.of(t("lock"), WarfareIcon.LOCK, act("lock", "")).detail(t("lock.detail")));
                case "unlock" -> out.add(Entry.of(t("unlock"), WarfareIcon.LOCK, act("unlock", "")));
                case "release" -> out.add(Entry.of(t("release"), WarfareIcon.MINUS, act("release", "")));
                case "request_drive" -> out.add(Entry.of(t("request"), WarfareIcon.VEHICLE, act("request_drive", "")));
                default -> {}
            }
        }
        boolean answers = false;
        for (var el : actions) answers |= el.getAsString().equals("approve");
        if (answers) {
            int n = 0;
            for (var r : requests) {
                if (n++ >= MAX_REQUESTS) break;
                String uuid = r.getAsJsonObject().get("uuid").getAsString();
                String name = r.getAsJsonObject().get("name").getAsString();
                out.add(Entry.of(t("approve", name), WarfareIcon.PLUS, act("approve", uuid)));
                out.add(Entry.of(t("deny", name), WarfareIcon.TRASH, act("deny", uuid))
                        .accent(WTheme.HUD_DANGER));
            }
        }
        if (out.isEmpty()) out.add(new Entry(t("none"), WarfareIcon.VEHICLE, null, WTheme.HUD_VALUE, false, null));
        return out;
    }
}
