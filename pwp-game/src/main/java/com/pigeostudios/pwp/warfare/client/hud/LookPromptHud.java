package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.block.AmmoBagBlock;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.EntrenchingToolItem;
import com.pigeostudios.pwp.warfare.match.LookPrompt;
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
 * Подсказка при наведении (этап 17i, как в Squad): плашка в контекстном ярусе низ-центра «что сделать — какой
 * клавишей» для своей техники, ФОБа, каркаса постройки, ящика, мешка с патронами и раненого союзника. Решение —
 * {@link LookPrompt} (pwp-logic); имена клавиш — из текущей раскладки. Уступает стройке, дрону и пациенту
 * (приоритет {@code LOOK_AT}).
 */
@OnlyIn(Dist.CLIENT)
public final class LookPromptHud {
    private static LookPrompt.Prompt prompt;

    private LookPromptHud() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(LookPromptHud::onPre);
        MinecraftForge.EVENT_BUS.addListener(LookPromptHud::onHud);
    }

    private static void onPre(RenderGuiEvent.Pre e) {
        prompt = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !WHud.hudVisible()) return;
        if (mc.player.isSpectator() || ClientPlacementHandler.isPlacing()) return;
        prompt = LookPrompt.of(target(mc), context(mc));
        if (prompt != null) WHud.contextOffer(WHud.CTX_LOOK_AT);
    }

    private static String myTeam(Minecraft mc) {
        return mc.player.getTeam() == null ? "" : mc.player.getTeam().getName();
    }

    private static boolean mine(String team, String my) {
        return team == null || team.isEmpty() || team.equals("NEUTRAL") || team.equalsIgnoreCase(my);
    }

    static LookPrompt.Target target(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return LookPrompt.Target.NONE;
        String my = myTeam(mc);
        if (hit instanceof EntityHitResult eh) {
            Entity en = eh.getEntity();
            if (en instanceof SupplyCrateEntity) return LookPrompt.Target.SUPPLY_CRATE;
            if (en instanceof Player p) {
                boolean ally = p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(my);
                return ally && ClientData.DOWNED_PLAYERS.contains(p.getId())
                        ? LookPrompt.Target.DOWNED_ALLY
                        : LookPrompt.Target.NONE;
            }
            for (WarfareWorldData.VehicleRecord r : ClientData.clientVehicles) {
                if (r.uuid.equals(en.getRootVehicle().getUUID()) && r.team.equalsIgnoreCase(my)) {
                    return mc.player.getVehicle() == null ? LookPrompt.Target.OWN_VEHICLE : LookPrompt.Target.NONE;
                }
            }
            return LookPrompt.Target.NONE;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof AmmoBagBlock) return LookPrompt.Target.AMMO_BAG;
        if (!EntrenchingToolItem.isWarfareConstruction(state)) return LookPrompt.Target.NONE;
        boolean own = mine(EntrenchingToolItem.getStructureTeam(mc.level.getBlockEntity(pos)), my);
        boolean built = EntrenchingToolItem.isConstructed(state);
        if (state.is(ModBlocks.HUB_BLOCK.get())) {
            if (!built) return own ? LookPrompt.Target.OWN_BLUEPRINT : LookPrompt.Target.ENEMY_STRUCTURE;
            return own ? LookPrompt.Target.OWN_FOB : LookPrompt.Target.ENEMY_FOB;
        }
        if (!own) return LookPrompt.Target.ENEMY_STRUCTURE;
        return built ? LookPrompt.Target.OWN_STRUCTURE : LookPrompt.Target.OWN_BLUEPRINT;
    }

    static LookPrompt.Context context(Minecraft mc) {
        var held = mc.player.getMainHandItem();
        var id = ForgeRegistries.ITEMS.getKey(held.getItem());
        boolean revive = id != null && id.toString().equals(WarfareConfig.REVIVE_ITEM.get());
        return new LookPrompt.Context(
                held.getItem() instanceof EntrenchingToolItem,
                held.isEmpty(),
                "Medic".equalsIgnoreCase(ClientData.myCurrentKit),
                revive);
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

    private static void onHud(RenderGuiOverlayEvent.Post e) {
        if (prompt == null || !e.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;
        Minecraft mc = Minecraft.getInstance();
        Component action = Component.translatable("gui.pwpwarfare.prompt." + prompt.action());
        Component title = prompt.altAction() == null
                ? action
                : Component.translatable(
                        "gui.pwpwarfare.prompt.with_alt",
                        action,
                        key(mc, prompt.altKey()),
                        Component.translatable("gui.pwpwarfare.prompt." + prompt.altAction()));
        String value = prompt.key() == null ? "" : key(mc, prompt.key()).getString();
        WHud.contextPlate(
                e.getGuiGraphics(),
                PWPTheme.Fonts.display(),
                WHud.CTX_LOOK_AT,
                WarfareIcon.MARKER,
                title,
                value,
                PWPTheme.Colors.ACCENT,
                -1F,
                PWPTheme.Colors.ACCENT,
                1F);
    }
}
