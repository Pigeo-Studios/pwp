package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.block.AmmoBagBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.MainSupplyBlock;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.EntrenchingToolItem;
import com.pigeostudios.pwp.warfare.match.LookPrompt;
import com.pigeostudios.pwp.warfare.network.PacketVehicleStatus;
import com.pigeostudios.pwp.warfare.vehicles.CrewRules;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * ЕДИНАЯ подсказка у прицела (этапы 17i–17j, как в Squad): что сделать с объектом под взглядом и какой клавишей,
 * плюс статус своей техники, пока сидишь в ней. Одна плашка в контекстном ярусе низ-центра вместо трёх прежних
 * рисунков (текст «Материалы» с ромбом над ФОБом/ящиком, текстовая подсказка техники и склада). Что показать —
 * решает {@link LookPrompt} (pwp-logic); имена клавиш — из текущей раскладки. Уступает стройке, дрону и пациенту
 * (приоритет {@code LOOK_AT}).
 */
@OnlyIn(Dist.CLIENT)
public final class LookPromptHud {
    /** Статус техники приходит с сервера раз в секунду; старше — не показываем. */
    private static final long STATUS_FRESH_MS = 5000L;

    private static LookPrompt.Prompt prompt;
    private static int materials = -1;
    private static Component status;
    private static int statusColor;

    private LookPromptHud() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(LookPromptHud::onPre);
        MinecraftForge.EVENT_BUS.addListener(LookPromptHud::onHud);
    }

    private static void onPre(RenderGuiEvent.Pre e) {
        prompt = null;
        status = null;
        materials = -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !WHud.hudVisible()) return;
        if (mc.player.isSpectator() || ClientPlacementHandler.isPlacing()) return;
        if (mc.player.getVehicle() != null) {
            vehicleStatus(mc);
        } else {
            prompt = LookPrompt.of(target(mc), context(mc));
        }
        if (prompt != null || status != null) WHud.contextOffer(WHud.CTX_LOOK_AT);
    }

    /** В технике: пополнение/погрузка/кулдаун — с сервера (PacketVehicleStatus). */
    private static void vehicleStatus(Minecraft mc) {
        ClientData.VehicleStatus st =
                ClientData.vehicleStatuses.get(mc.player.getVehicle().getId());
        if (st == null || System.currentTimeMillis() - st.receivedAt >= STATUS_FRESH_MS) return;
        int left = Math.max(0, st.secondsLeft - (int) ((System.currentTimeMillis() - st.receivedAt) / 1000L));
        String key =
                switch (st.kind) {
                    case PacketVehicleStatus.KIND_REARMING -> "gui.pwpwarfare.hud.veh.rearming";
                    case PacketVehicleStatus.KIND_LOADING_CRATE -> "gui.pwpwarfare.hud.veh.loading_crate";
                    case PacketVehicleStatus.KIND_REARM_COOLDOWN -> "gui.pwpwarfare.hud.veh.rearm_cooldown";
                    case PacketVehicleStatus.KIND_CRATE_RESUPPLY -> "gui.pwpwarfare.hud.veh.crate_resupply";
                    default -> null;
                };
        if (key == null) return;
        status = Component.translatable(key, left);
        statusColor = st.kind == PacketVehicleStatus.KIND_REARM_COOLDOWN ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT;
    }

    private static String myTeam(Minecraft mc) {
        return com.pigeostudios.pwp.warfare.util.Teams.of(mc.player);
    }

    private static boolean mine(String team, String my) {
        return team == null || team.isEmpty() || team.equals("NEUTRAL") || team.equalsIgnoreCase(my);
    }

    private static WarfareWorldData.VehicleRecord vehicleRecord(Entity en) {
        for (WarfareWorldData.VehicleRecord r : ClientData.clientVehicles) {
            if (r.uuid.equals(en.getRootVehicle().getUUID())) return r;
        }
        return null;
    }

    static LookPrompt.Target target(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return LookPrompt.Target.NONE;
        String my = myTeam(mc);
        if (hit instanceof EntityHitResult eh) {
            Entity en = eh.getEntity();
            if (en instanceof SupplyCrateEntity crate) {
                materials = crate.getMaterials();
                return LookPrompt.Target.SUPPLY_CRATE;
            }
            if (en instanceof Player p) {
                boolean ally = p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(my);
                return ally && ClientData.DOWNED_PLAYERS.contains(p.getId())
                        ? LookPrompt.Target.DOWNED_ALLY
                        : LookPrompt.Target.NONE;
            }
            WarfareWorldData.VehicleRecord r = vehicleRecord(en);
            if (r == null) return LookPrompt.Target.NONE;
            return mine(r.team, my) ? LookPrompt.Target.OWN_VEHICLE : LookPrompt.Target.ENEMY_VEHICLE;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof AmmoBagBlock) return LookPrompt.Target.AMMO_BAG;
        if (state.getBlock() instanceof MainSupplyBlock) return LookPrompt.Target.MAIN_SUPPLY;
        if (!EntrenchingToolItem.isWarfareConstruction(state)) return LookPrompt.Target.NONE;
        var be = mc.level.getBlockEntity(pos);
        boolean own = mine(EntrenchingToolItem.getStructureTeam(be), my);
        boolean built = EntrenchingToolItem.isConstructed(state);
        if (state.is(ModBlocks.HUB_BLOCK.get())) {
            if (!built) return own ? LookPrompt.Target.OWN_BLUEPRINT : LookPrompt.Target.ENEMY_STRUCTURE;
            if (be instanceof HubBlockEntity hub && own) materials = hub.getMaterials();
            return own ? LookPrompt.Target.OWN_FOB : LookPrompt.Target.ENEMY_FOB;
        }
        if (!own) return LookPrompt.Target.ENEMY_STRUCTURE;
        return built ? LookPrompt.Target.OWN_STRUCTURE : LookPrompt.Target.OWN_BLUEPRINT;
    }

    static LookPrompt.Context context(Minecraft mc) {
        var held = mc.player.getMainHandItem();
        var id = ForgeRegistries.ITEMS.getKey(held.getItem());
        boolean revive = id != null && id.toString().equals(WarfareConfig.REVIVE_ITEM.get());
        boolean noKit = false;
        if (mc.hitResult instanceof EntityHitResult eh) {
            WarfareWorldData.VehicleRecord r = vehicleRecord(eh.getEntity());
            noKit = r != null
                    && CrewRules.isSpecialist(r.type)
                    && !CrewRules.hasCorrectKit(r.type, ClientData.myCurrentKit);
        }
        return new LookPrompt.Context(
                held.getItem() instanceof EntrenchingToolItem,
                held.isEmpty(),
                "Medic".equalsIgnoreCase(ClientData.myCurrentKit),
                revive,
                noKit);
    }

    static Component key(Minecraft mc, LookPrompt.Key k) {
        Component use = mc.options.keyUse.getTranslatedKeyMessage();
        return switch (k) {
            case USE -> use;
            case HOLD_USE -> Component.translatable("gui.pwpwarfare.prompt.key.hold", use);
            case SHIFT_USE -> Component.translatable(
                    "gui.pwpwarfare.prompt.key.combo", mc.options.keyShift.getTranslatedKeyMessage(), use);
            case VEHICLE_MENU -> ModKeyBindings.VEHICLE_MENU_KEY.getTranslatedKeyMessage();
            case TACTICAL -> ModKeyBindings.TACTICAL_KEY.getTranslatedKeyMessage();
        };
    }

    private static Component p(String suffix) {
        return Component.translatable("gui.pwpwarfare.prompt." + suffix);
    }

    /** «Действие · клавиша — действие · …» и, если нужно, «· Материалы: N». */
    static Component title(Minecraft mc, LookPrompt.Prompt pr, int mats) {
        net.minecraft.network.chat.MutableComponent t = p(pr.action()).copy();
        if (pr.altAction() != null) {
            t.append(Component.translatable("gui.pwpwarfare.prompt.alt", key(mc, pr.altKey()), p(pr.altAction())));
        }
        if (pr.alt2Action() != null) {
            t.append(Component.translatable("gui.pwpwarfare.prompt.alt", key(mc, pr.alt2Key()), p(pr.alt2Action())));
        }
        if (pr.materials() && mats >= 0) t.append(Component.translatable("gui.pwpwarfare.prompt.materials", mats));
        return t;
    }

    private static void onHud(RenderGuiOverlayEvent.Post e) {
        if ((prompt == null && status == null) || !e.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id()))
            return;
        Minecraft mc = Minecraft.getInstance();
        if (status != null) {
            WHud.contextPlate(
                    e.getGuiGraphics(),
                    PWPTheme.Fonts.display(),
                    WHud.CTX_LOOK_AT,
                    WarfareIcon.VEHICLE,
                    status,
                    "",
                    statusColor,
                    -1F,
                    statusColor,
                    1F);
            return;
        }
        int accent = prompt.warn() ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT;
        WHud.contextPlate(
                e.getGuiGraphics(),
                PWPTheme.Fonts.display(),
                WHud.CTX_LOOK_AT,
                prompt.warn() ? WarfareIcon.TARGET : WarfareIcon.MARKER,
                title(mc, prompt, materials),
                prompt.key() == null ? "" : key(mc, prompt.key()).getString(),
                accent,
                -1F,
                accent,
                1F);
    }
}
