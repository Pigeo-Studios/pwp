package com.pigeostudios.pwp.warfare.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.tutorial.Tutorial;
import com.pigeostudios.pwp.warfare.tutorial.Tutorial.Step;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обучение новичка на клиенте (этап 5 ROADMAP): шаги — чистый {@link Tutorial}, показ — подсказки
 * {@code tutorial.*} в {@link WarfareHints} (плашка справа сверху, та же система «что дальше»).
 * Прогресс — на аккаунт: {@code config/pwp_tutorial.json} ({@code {"<uuid>": "ROLE", …}}, JsonObject вручную).
 * Выключатель «Обучение новичка» ({@link WarfareUiSettings#TUTORIAL}) — пропустить. Ничего не шлёт на сервер.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TutorialClient {
    private static final Logger LOG = LoggerFactory.getLogger(TutorialClient.class);
    private static final String FILE = "pwp_tutorial.json";

    private static Tutorial tutorial;
    private static UUID owner;
    private static boolean justFinished;
    private static int tickCounter;

    private TutorialClient() {}

    /** Сейчас показывается шаг {@code s} (обучение включено и не пройдено). */
    public static boolean showing(Step s) {
        return active() && tutorial.step() == s;
    }

    /** Обучение идёт — обычные подсказки тех же шагов (команда/отряд/роль) молчат, чтобы не дублировать. */
    public static boolean active() {
        return WarfareUiSettings.TUTORIAL.get() && tutorial != null && !tutorial.done();
    }

    /** Обучение только что пройдено (для разовой поздравительной подсказки). */
    public static boolean justFinished() {
        return justFinished;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++tickCounter % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (!p.getUUID().equals(owner)) {
            owner = p.getUUID();
            tutorial = new Tutorial(loadStep(owner));
            justFinished = false;
        }
        if (tutorial.done() || !WarfareUiSettings.TUTORIAL.get()) return;
        Step before = tutorial.step();
        Step now = tutorial.update(snapshot(p));
        if (now != before) {
            saveStep(owner, now);
            if (now == Step.DONE) justFinished = true;
        }
    }

    private static Tutorial.Snapshot snapshot(LocalPlayer p) {
        boolean alive = p.isAlive() && !p.isSpectator();
        boolean battle = ClientData.isGameStarted;
        boolean onPoint = false;
        if (battle && alive) {
            for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
                if (cp.area != null && cp.area.contains(p.position())) {
                    onPoint = true;
                    break;
                }
            }
        }
        return new Tutorial.Snapshot(
                ClientStaging.hasTeam(),
                ClientStaging.mySquad() != null,
                ClientStaging.hasKit(),
                battle,
                alive && !ClientSafetyState.inMainZone,
                onPoint);
    }

    // ───────────────────────────── файл прогресса ─────────────────────────────

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static JsonObject readAll() {
        Path f = file();
        if (!Files.isRegularFile(f)) return new JsonObject();
        try {
            JsonElement root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
            if (root.isJsonObject()) return root.getAsJsonObject();
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Обучение: не прочитан {} — начинаем с первого шага", f, ex);
        }
        return new JsonObject();
    }

    private static Step loadStep(UUID id) {
        JsonElement e = readAll().get(id.toString());
        if (e == null || !e.isJsonPrimitive()) return Step.TEAM;
        try {
            return Step.valueOf(e.getAsString());
        } catch (IllegalArgumentException ex) {
            return Step.TEAM;
        }
    }

    private static void saveStep(UUID id, Step step) {
        JsonObject all = readAll();
        all.addProperty(id.toString(), step.name());
        try {
            Files.writeString(file(), all.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warn("Обучение: не записан {}", file(), ex);
        }
    }
}
