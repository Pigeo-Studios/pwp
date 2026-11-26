package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.match.TacticalRose;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.server.CommanderAbilities;
import com.pigeostudios.pwp.warfare.server.TacticalActions;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Тактическая радиалка — «коммо-роза» как в Squad (этап 17d): одна клавиша (Y), пункты по роли
 * ({@link TacticalRose}): враг здесь, запросы отряду, приказы, стройка (ралли/ФОБ/разделы), техника, командир.
 * Точку для приказов и командира берёт из взгляда; метку врага сервер ставит сам по взгляду игрока.
 */
@OnlyIn(Dist.CLIENT)
public class TacticalRadialScreen extends WarfareRadialScreen {
    private static final int PICK_RANGE = TacticalRose.SPOT_RANGE;

    private final BlockPos target;
    private final int vehicleId;

    public TacticalRadialScreen(BlockPos target, int vehicleId) {
        super(Component.translatable("gui.pwpwarfare.tactical.title"));
        this.target = target;
        this.vehicleId = vehicleId;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TacticalRadialScreen::onKey);
    }

    private static void onKey(InputEvent.Key e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getAction() != 1 || mc.screen != null || mc.player == null) return;
        if (!ModKeyBindings.TACTICAL_KEY.matches(e.getKey(), e.getScanCode())) return;
        HitResult far = mc.player.pick(PICK_RANGE, 1F, false);
        BlockPos at = far instanceof BlockHitResult b && far.getType() == HitResult.Type.BLOCK
                ? b.getBlockPos()
                : mc.player.blockPosition();
        Entity v = mc.player.getVehicle();
        if (v == null && mc.hitResult instanceof EntityHitResult hit && !(hit.getEntity() instanceof Player)) {
            v = hit.getEntity();
        }
        mc.setScreen(
                new TacticalRadialScreen(at, v == null ? -1 : v.getRootVehicle().getId()));
    }

    /** Роль игрока по данным клиента (для показа; права проверяет сервер). */
    TacticalRose.Context context() {
        var p = Minecraft.getInstance().player;
        String team = p == null || p.getTeam() == null ? "" : p.getTeam().getName();
        boolean inTeam = team.equalsIgnoreCase("BLUE") || team.equalsIgnoreCase("RED");
        String me = p == null ? "" : p.getScoreboardName();
        WarfareWorldData.Squad squad = null;
        for (var s : ClientData.clientSquads) if (s.members.contains(me)) squad = s;
        boolean sl = squad != null && me.equals(squad.leader);
        boolean ftl = squad != null && (me.equals(squad.bravoLeader) || me.equals(squad.charlieLeader));
        int cmd = team.equalsIgnoreCase("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean commander = sl && cmd != -1 && squad.id == cmd;
        return new TacticalRose.Context(inTeam, squad != null, sl, ftl, commander, vehicleId >= 0);
    }

    private static Component t(String key) {
        return Component.translatable("gui.pwpwarfare.tactical." + key);
    }

    private static void send(String kind) {
        PacketHandler.INSTANCE.sendToServer(new TacticalActions.Act(kind));
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (TacticalRose.Item item : TacticalRose.items(context())) {
            switch (item) {
                case SPOT -> out.add(Entry.of(t("spot"), WarfareIcon.TARGET, () -> {
                            send("spot");
                            onClose();
                        })
                        .accent(WTheme.MARKER_ENEMY));
                case REQUESTS -> out.add(Entry.of(t("requests"), WarfareIcon.CHAT, () -> switchTo(new Requests(this))));
                case ORDERS -> out.add(Entry.of(t("orders"), WarfareIcon.MARKER, () -> Minecraft.getInstance()
                                .setScreen(new SquadMarkerRadialScreen(target.getX(), target.getZ(), null)))
                        .accent(WTheme.MARKER_SQUAD));
                case BUILD -> out.add(Entry.of(t("build"), WarfareIcon.WALL, () -> switchTo(new RadioRadialScreen())));
                case VEHICLE -> out.add(Entry.of(t("vehicle"), WarfareIcon.VEHICLE, () -> Minecraft.getInstance()
                        .setScreen(new VehicleRadialScreen(vehicleId))));
                case COMMANDER -> out.add(
                        Entry.of(t("commander"), WarfareIcon.STAR, () -> switchTo(new Commander(this, target))));
            }
        }
        if (out.isEmpty()) out.add(new Entry(t("none"), WarfareIcon.TEAM, null, WTheme.HUD_VALUE, false, null));
        return out;
    }

    /** Запросы отряду: медик, патроны, транспорт. */
    static final class Requests extends WarfareRadialScreen {
        private final Screen parent;

        Requests(Screen parent) {
            super(Component.translatable("gui.pwpwarfare.tactical.requests"));
            this.parent = parent;
        }

        private Entry req(TacticalRose.Request r, WarfareIcon icon) {
            return Entry.of(t("request." + r.id()), icon, () -> {
                send(r.id());
                onClose();
            });
        }

        @Override
        protected List<Entry> entries() {
            return List.of(
                    req(TacticalRose.Request.MEDIC, WarfareIcon.PLUS),
                    req(TacticalRose.Request.AMMO, WarfareIcon.CRATE),
                    req(TacticalRose.Request.RIDE, WarfareIcon.VEHICLE));
        }

        @Override
        protected boolean onBack() {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
    }

    /** Способности командира в точке взгляда (те же, что ПКМ на карте). */
    static final class Commander extends WarfareRadialScreen {
        private final Screen parent;
        private final BlockPos at;

        Commander(Screen parent, BlockPos at) {
            super(Component.translatable("gui.pwpwarfare.tactical.commander"));
            this.parent = parent;
            this.at = at;
        }

        private Entry use(String ability, String key, WarfareIcon icon) {
            return Entry.of(Component.translatable(key), icon, () -> {
                PacketHandler.INSTANCE.sendToServer(new CommanderAbilities.Use(ability, at.getX(), at.getZ()));
                onClose();
            });
        }

        @Override
        protected List<Entry> entries() {
            return List.of(
                    use("uav", "gui.pwpwarfare.map.menu.cmd_uav", WarfareIcon.COMPASS),
                    use("supply", "gui.pwpwarfare.map.menu.cmd_supply", WarfareIcon.CRATE),
                    use("cas", "gui.pwpwarfare.map.menu.cmd_cas", WarfareIcon.TARGET)
                            .accent(WTheme.HUD_DANGER));
        }

        @Override
        protected boolean onBack() {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
    }
}
