package com.pigeostudios.pwp.warfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pigeostudios.pwp.warfare.block.AGSConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlockEntity;
import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.MainSupplyBlock;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.RebConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlock;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlockEntity;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.block.WallBlockEntity;
import com.pigeostudios.pwp.warfare.client.gui.SquadMapRenderer;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.hud.TopHud;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.network.PacketVehicleStatus;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
// Основной оверлей мода: HUD — компас, захват точек, постройки,
// тикеты, уведомления, рация и другие элементы (комментарий восстановлен: исходник был повреждён)
public class WarfareOverlay {
    private static final ResourceLocation FLAG_UKRAINE =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/ukraine.png");
    private static final ResourceLocation FLAG_RUSSIA =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/russia.png");
    private static final ResourceLocation FLAG_USA = new ResourceLocation("pwpwarfare", "textures/gui/flags/usa.png");
    private static final ResourceLocation FLAG_NATO = new ResourceLocation("pwpwarfare", "textures/gui/flags/nato.png");
    private static final ResourceLocation FLAG_BLUEFOR =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/bluefor.png");
    private static final ResourceLocation FLAG_REDFOR =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/redfor.png");
    private static final ResourceLocation FLAG_INSURGENCY =
            new ResourceLocation("pwpwarfare", "textures/gui/flags/insurgency.png");
    private static final ResourceLocation FLAG_PMC = new ResourceLocation("pwpwarfare", "textures/gui/flags/pmc.png");
    private static final ResourceLocation VIGNETTE_TEXTURE =
            new ResourceLocation("pwpwarfare", "textures/misc/vignette.png");
    private static final ResourceLocation ARROW_TEX =
            new ResourceLocation("pwpwarfare", "textures/gui/capture_arrow.png");
    private static SquadMapRenderer HUD_SIDE_MAP;
    private static final ResourceLocation MOUSE_LEFT = new ResourceLocation("pwpwarfare", "textures/gui/dig_icon.png");
    private static final ResourceLocation MOUSE_RIGHT =
            new ResourceLocation("pwpwarfare", "textures/gui/build_icon.png");
    private static final ResourceLocation ICON_ROTATE =
            new ResourceLocation("pwpwarfare", "textures/gui/icon_rotate.png");
    private static final ResourceLocation ICON_CONFIRM =
            new ResourceLocation("pwpwarfare", "textures/gui/icon_confirm.png");
    private static final ResourceLocation VOICE_ICON_TEX =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon.png");
    private static final ResourceLocation VOICE_ICON_RADIO_TEX =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_radio.png");
    private static final ResourceLocation VOICE_ICON_STAR_TEX =
            new ResourceLocation("pwpwarfare", "textures/gui/voice_icon_star.png");

    // ------------------------------------------------------------ строки HUD (lang, кэш компонентов)
    private static final String HUD = "gui.pwpwarfare.hud.";
    private static final TopHud TOP = new TopHud();
    private static final TopHud.Model TOP_MODEL = new TopHud.Model();
    private static final WHud.Fmt CMD_VOTE_TITLE = new WHud.Fmt(HUD + "cmd_vote.title");
    private static final Component CMD_VOTE_RULE = Component.translatable(HUD + "cmd_vote.rule");
    private static final Component CMD_VOTE_WAITING = Component.translatable(HUD + "cmd_vote.waiting");
    private static final WHud.KeyHint CMD_VOTE_KEYS =
            new WHud.KeyHint(HUD + "vote.keys", ModKeyBindings.VOTE_CMD_YES_KEY, ModKeyBindings.VOTE_CMD_NO_KEY);
    private static final Component ART_TITLE = Component.translatable(HUD + "art.title");
    private static final WHud.Fmt ART_FROM = new WHud.Fmt(HUD + "art.from");
    private static final WHud.Fmt ART_POS = new WHud.Fmt(HUD + "art.pos");
    private static final WHud.KeyHint ART_KEYS =
            new WHud.KeyHint(HUD + "art.keys", ModKeyBindings.ART_CONFIRM_KEY, ModKeyBindings.ART_DENY_KEY);
    private static final Component BUILD_DESTROY = Component.translatable(HUD + "build.destroy");
    private static final Component BUILD_BUILD = Component.translatable(HUD + "build.build");
    private static final Component BUILD_DESTROYING = Component.translatable(HUD + "build.destroying");
    private static final Component BUILD_DISMANTLING = Component.translatable(HUD + "build.dismantling");
    private static final Component BUILD_ENEMY = Component.translatable(HUD + "build.enemy");
    private static final Component BUILD_FINISHED = Component.translatable(HUD + "build.finished");
    private static final Component VOTE_TITLE = Component.translatable(HUD + "vote.title");
    private static final WHud.Clock VOTE_CLOCK = new WHud.Clock();
    private static final WHud.Fmt VOTE_BLUE_READY = new WHud.Fmt(HUD + "vote.ready");
    private static final WHud.Fmt VOTE_BLUE_WAITING = new WHud.Fmt(HUD + "vote.waiting");
    private static final WHud.Fmt VOTE_RED_READY = new WHud.Fmt(HUD + "vote.ready");
    private static final WHud.Fmt VOTE_RED_WAITING = new WHud.Fmt(HUD + "vote.waiting");
    private static final WHud.KeyHint VOTE_KEYS =
            new WHud.KeyHint(HUD + "vote.keys", ModKeyBindings.VOTE_START_YES_KEY, ModKeyBindings.VOTE_START_NO_KEY);
    private static final Component SUPPLY_LABEL = Component.translatable(HUD + "supply.label");
    private static final WHud.Fmt VALUE_OF_SUPPLY = new WHud.Fmt(HUD + "value_of");
    private static final Component SUPPLY_LOADING = Component.translatable(HUD + "supply.loading");
    private static final Component AMMO_LABEL = Component.translatable(HUD + "ammo.label");
    private static final WHud.Fmt VALUE_OF_AMMO = new WHud.Fmt(HUD + "value_of");
    /** Строки списка ноков у медика: кэш по номеру строки (имя/дистанция меняются редко). */
    private static final WHud.Fmt[] MEDIC_ROWS = new WHud.Fmt[16];

    private static WHud.Fmt medicRow(int i) {
        int idx = Math.min(i, MEDIC_ROWS.length - 1);
        WHud.Fmt f = MEDIC_ROWS[idx];
        if (f == null) {
            f = new WHud.Fmt(HUD + "medic.downed");
            MEDIC_ROWS[idx] = f;
        }
        return f;
    }

    /** Строка уведомления о захвате в верхнем регистре (как раньше); пересборка только при смене. */
    @SubscribeEvent
    public static void onRenderOverlay(Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.CHAT_PANEL.type()) {
            GuiGraphics gui = event.getGuiGraphics();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();
                // общий гейт HUD (F1 скрывает); виньетка нока и боковая карта — вне гейта
                boolean hud = WHud.hudVisible();
                if (hud) renderHud(gui, mc, width, height, event.getPartialTick());
                if (WarfareData.of(mc.player).getBoolean(WarfareKeys.IS_DOWNED)) {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(0.5F, 0.0F, 0.0F, 0.9F);
                    gui.blit(VIGNETTE_TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                }

                if (hud) {
                    if ("Medic".equalsIgnoreCase(ClientData.myCurrentKit)) {
                        renderMedicUI(gui, mc, width);
                    }
                    renderRadioSpeakers(gui, mc, height);
                    renderVoiceSpeakers(gui, mc, height);
                }
                if (ClientData.isMapOpen || ClientData.mapTransition > 0.0F) {
                    renderSideMap(gui, mc, width, height, event.getPartialTick());
                }
            }
        }
    }

    /** HUD-элементы под общим гейтом {@link WHud#hudVisible()}. */
    private static void renderHud(GuiGraphics gui, Minecraft mc, int width, int height, float partialTick) {
        // верх (этап 3): компас, тикеты + таймер, таймер Invasion, точка захвата, баннер захвата
        TopHud.fromClient(TOP_MODEL, mc, partialTick);
        TOP.render(gui, PWPTheme.Fonts.display(), WHud.zones(), TOP_MODEL);
        renderVotePanel(gui, mc, height);

        renderHubMaterials(gui, mc, width, height);
        renderVehicleHud(gui, mc, width, height);
        renderVehicleAmmo(gui, mc, width, height);
        renderSupplyTruckInfo(gui, mc, width, height);
        renderPlacementHints(gui, mc, width, height);
        // после look-at/Build Mode: они резервируют низ-центр, стройка встаёт над ними
        renderBuildProgress(gui);
        renderCMDVotePanel(gui, mc, width);
        renderArtStrikeRequest(gui, mc, width);
    }

    private static void renderCMDVotePanel(GuiGraphics gui, Minecraft mc, int width) {
        String myTeam =
                mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
        if (!myTeam.equals("NEUTRAL")) {
            boolean isBlue = myTeam.equals("BLUE");
            boolean active = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
            if (active) {
                String candidateName = isBlue ? ClientData.blueCmdCandidateName : ClientData.redCmdCandidateName;
                Map<UUID, Boolean> currentVotes = isBlue ? ClientData.blueCmdVotes : ClientData.redCmdVotes;
                List<MapPlayerInfo> slPlayers = ClientData.mapPlayers.values().stream()
                        .filter(infox -> infox.team.equalsIgnoreCase(myTeam))
                        .filter(infox -> infox.isLeader && !infox.name.equals(candidateName))
                        .toList();
                int headerH = 32;
                int rowH = 12;
                int footerH = 15;
                int pWidth = 150;
                int pHeight = headerH + slPlayers.size() * rowH + footerH;
                int x = width - pWidth - 10;
                int y = 70;
                gui.fill(x, y, x + pWidth, y + pHeight, -1442840576);
                gui.renderOutline(x, y, pWidth, pHeight, -11141291);
                gui.drawCenteredString(
                        PWPTheme.Fonts.display(), CMD_VOTE_TITLE.of(candidateName), x + pWidth / 2, y + 5, -10496);
                gui.pose().pushPose();
                gui.pose().scale(0.75F, 0.75F, 1.0F);
                int sx = (int) ((x + pWidth / 2) / 0.75F);
                gui.drawCenteredString(PWPTheme.Fonts.display(), CMD_VOTE_RULE, sx, (int) ((y + 16) / 0.75F), -5592406);
                gui.pose().popPose();
                gui.fill(x + 5, y + 28, x + pWidth - 5, y + 29, 1442840575);
                int curY = y + headerH;

                for (MapPlayerInfo info : slPlayers) {
                    Boolean vote = currentVotes.get(info.uuid);
                    String icon = "\u25CB";
                    int iconCol = -5592406;
                    if (vote != null) {
                        icon = vote ? "\u2714" : "\u2718";
                        iconCol = vote ? -11141291 : -43691;
                    }

                    gui.drawString(PWPTheme.Fonts.display(), icon, x + 8, curY, iconCol, true);
                    int nameCol = info.name.equals(mc.player.getScoreboardName()) ? -171 : -1;
                    gui.drawString(PWPTheme.Fonts.display(), info.name, x + 22, curY, nameCol, true);
                    curY += rowH;
                }

                if (!mc.player.getScoreboardName().equals(candidateName)) {
                    gui.drawCenteredString(
                            PWPTheme.Fonts.display(), CMD_VOTE_KEYS.get(), x + pWidth / 2, y + pHeight - 12, -4473925);
                } else {
                    gui.drawCenteredString(
                            PWPTheme.Fonts.display(), CMD_VOTE_WAITING, x + pWidth / 2, y + pHeight - 12, -11141291);
                }
            }
        }
    }

    private static void renderArtStrikeRequest(GuiGraphics gui, Minecraft mc, int width) {
        if (mc.player != null) {
            String myTeam =
                    mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
            if (!myTeam.equals("NEUTRAL")) {
                int timer = myTeam.equals("BLUE") ? ClientData.blueArtTimer : ClientData.redArtTimer;
                String requester = myTeam.equals("BLUE") ? ClientData.blueArtReqName : ClientData.redArtReqName;
                BlockPos pos = myTeam.equals("BLUE") ? ClientData.blueArtPos : ClientData.redArtPos;
                if (timer > 0 && !requester.isEmpty() && pos != null) {
                    int mySquadId = WarfareData.of(mc.player).getInt(WarfareKeys.SQUAD_ID);
                    boolean isSL = WarfareData.of(mc.player).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
                    int teamCmdId = myTeam.equals("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
                    if (mc.player.isCreative() || isSL && mySquadId != -1 && mySquadId == teamCmdId) {
                        int x = width - 160;
                        int y = 140;
                        gui.fill(x, y, x + 150, y + 45, -1442840576);
                        gui.renderOutline(x, y, 150, 45, -43691);
                        gui.drawCenteredString(PWPTheme.Fonts.display(), ART_TITLE, x + 75, y + 5, -43691);
                        gui.pose().pushPose();
                        gui.pose().scale(0.8F, 0.8F, 1.0F);
                        int sx = (int) ((x + 75) / 0.8F);
                        gui.drawCenteredString(
                                PWPTheme.Fonts.display(), ART_FROM.of(requester), sx, (int) ((y + 18) / 0.8F), -1);
                        gui.drawCenteredString(
                                PWPTheme.Fonts.display(),
                                ART_POS.of(pos.getX(), pos.getZ()),
                                sx,
                                (int) ((y + 28) / 0.8F),
                                -5592406);
                        gui.pose().popPose();
                        gui.drawCenteredString(PWPTheme.Fonts.display(), ART_KEYS.get(), x + 75, y + 35, -171);
                        float progress = Math.max(0.0F, timer / 200.0F);
                        gui.fill(x + 5, y + 43, x + 5 + (int) (140.0F * progress), y + 44, -1);
                    }
                }
            }
        }
    }

    private static void renderPlacementHints(GuiGraphics gui, Minecraft mc, int width, int height) {
        if (ClientPlacementHandler.isPlacing()) {
            int uiWidth = 140;
            int uiHeight = 44;
            int xStart = (width - uiWidth) / 2;
            int yStart = height - uiHeight - 60;
            WHud.contextReserve(yStart); // ярус занят — контекстная плашка встанет выше
            int goldLight = -10496;
            gui.fill(xStart, yStart, xStart + uiWidth, yStart + uiHeight, -1879048192);
            gui.fill(xStart, yStart, xStart + 2, yStart + uiHeight, goldLight);
            gui.drawCenteredString(PWPTheme.Fonts.display(), "Build Mode", xStart + uiWidth / 2, yStart + 4, goldLight);
            RenderSystem.enableBlend();
            int row1Y = yStart + 16;
            gui.blit(MOUSE_LEFT, xStart + 8, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
            gui.drawString(PWPTheme.Fonts.display(), "Rotate", xStart + 26, row1Y + 2, -1, true);
            gui.blit(ICON_ROTATE, xStart + uiWidth - 20, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
            int row2Y = yStart + 30;
            gui.blit(MOUSE_RIGHT, xStart + 8, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
            gui.drawString(PWPTheme.Fonts.display(), "Confirm", xStart + 26, row2Y + 2, -1, true);
            gui.blit(ICON_CONFIRM, xStart + uiWidth - 20, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
            RenderSystem.disableBlend();
        }
    }

    private static void renderRadioSpeakers(GuiGraphics gui, Minecraft mc, int height) {
        if (!ClientData.RADIO_SPEAKERS.isEmpty()) {
            long now = System.currentTimeMillis();
            int yOffset = height / 2 - 40;
            int xOffset = 5;

            for (Entry<String, Long> entry : new ArrayList<>(ClientData.RADIO_SPEAKERS.entrySet())) {
                if (now - entry.getValue() > 1000L) {
                    ClientData.RADIO_SPEAKERS.remove(entry.getKey());
                } else {
                    String speakerName = entry.getKey();
                    int color = -256;
                    ResourceLocation icon = isCMD(speakerName) ? VOICE_ICON_STAR_TEX : VOICE_ICON_RADIO_TEX;
                    int textWidth = PWPTheme.Fonts.display().width(speakerName) + 15;
                    gui.fill(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
                    RenderSystem.enableBlend();
                    float r = (color >> 16 & 0xFF) / 255.0F;
                    float g = (color >> 8 & 0xFF) / 255.0F;
                    float b = (color & 0xFF) / 255.0F;
                    RenderSystem.setShaderColor(r, g, b, 1.0F);
                    gui.blit(icon, xOffset + 3, yOffset, 0.0F, 0.0F, 8, 8, 8, 8);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    gui.drawString(PWPTheme.Fonts.display(), speakerName, xOffset + 14, yOffset, color, false);
                    yOffset += 14;
                }
            }
        }
    }

    private static void renderVoiceSpeakers(GuiGraphics gui, Minecraft mc, int height) {
        if (!ClientData.SQUAD_SPEAKERS.isEmpty()) {
            long now = System.currentTimeMillis();
            int radioLines = ClientData.RADIO_SPEAKERS.size();
            int yOffset = height / 2 - 40 + radioLines * 14;
            int xOffset = 5;

            for (Entry<String, Long> entry : new ArrayList<>(ClientData.SQUAD_SPEAKERS.entrySet())) {
                if (now - entry.getValue() > 1000L) {
                    ClientData.SQUAD_SPEAKERS.remove(entry.getKey());
                } else {
                    String speakerName = entry.getKey();
                    int color = -11141291;
                    ResourceLocation icon = isCMD(speakerName) ? VOICE_ICON_STAR_TEX : VOICE_ICON_TEX;
                    int textWidth = PWPTheme.Fonts.display().width(speakerName) + 15;
                    gui.fill(xOffset, yOffset - 2, xOffset + 5 + textWidth, yOffset + 10, Integer.MIN_VALUE);
                    RenderSystem.enableBlend();
                    float r = (color >> 16 & 0xFF) / 255.0F;
                    float g = (color >> 8 & 0xFF) / 255.0F;
                    float b = (color & 0xFF) / 255.0F;
                    RenderSystem.setShaderColor(r, g, b, 1.0F);
                    gui.blit(icon, xOffset + 3, yOffset, 0.0F, 0.0F, 8, 8, 8, 8);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    gui.drawString(PWPTheme.Fonts.display(), speakerName, xOffset + 14, yOffset, color, false);
                    yOffset += 14;
                }
            }
        }
    }

    private static boolean isCMD(String playerName) {
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(playerName)) {
                int cmdid = s.team.equalsIgnoreCase("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
                return s.id == cmdid;
            }
        }
        return false;
    }

    /** Верх ромба команды над текстом «Материалы» (центр textY-12, повёрнутый 10x10 + рамка). */
    private static final int LOOK_AT_DIAMOND_ABOVE = 19;

    private static void renderHubMaterials(GuiGraphics gui, Minecraft mc, int width, int height) {
        if (mc.hitResult != null) {
            int mats = -1;
            String team = "NEUTRAL";
            if (mc.hitResult.getType() == Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) mc.hitResult;
                BlockPos pos = hit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                if (mc.level.getBlockEntity(pos) instanceof HubBlockEntity hub) {
                    if (state.hasProperty(HubBlock.CONSTRUCTED) && !(Boolean) state.getValue(HubBlock.CONSTRUCTED)) {
                        return;
                    }

                    mats = hub.getMaterials();
                    team = hub.getTeam();
                }
            } else if (mc.hitResult.getType() == Type.ENTITY) {
                EntityHitResult hit = (EntityHitResult) mc.hitResult;
                if (hit.getEntity() instanceof SupplyCrateEntity crate) {
                    mats = crate.getMaterials();
                    team = crate.getTeamOwner();
                }
            }

            if (mats != -1) {
                int teamColor = -1;
                if (team.equalsIgnoreCase("BLUE")) {
                    teamColor = -11184641;
                } else if (team.equalsIgnoreCase("RED")) {
                    teamColor = -43691;
                }

                String text = "Материалы: " + mats;
                int textWidth = PWPTheme.Fonts.display().width(text);
                int textX = (width - textWidth) / 2;
                int textY = height - 112; // ярус look-at подсказок (лента h-76/h-94 и фидбек h-58 ниже)
                WHud.contextReserve(textY - LOOK_AT_DIAMOND_ABOVE); // ромб выше текста
                PoseStack pose = gui.pose();
                pose.pushPose();
                pose.translate(width / 2.0F, textY - 12, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
                gui.fill(-5, -5, 5, 5, -16777216);
                gui.fill(-4, -4, 4, 4, teamColor);
                pose.popPose();
                drawOutlinedString(gui, mc, text, textX, textY, -22016);
            }
        }
    }

    private static float vehicleHintAlpha = 0.0F;
    private static final UIDamp.Frame VEHICLE_HINT_FRAME = new UIDamp.Frame();
    private static final UIDamp.Frame VOTE_FRAME = new UIDamp.Frame();

    // Единый HUD-блок техники: подсказка при наведении прицела (спецкит/шифт+ПКМ)
    // и статус-бар при нахождении в технике. Позиция — ярус look-at подсказок
    // (height - 112, выше ленты h-76/h-94 и фидбек-блока h-58, коллизий нет),
    // появляется и исчезает плавно.
    private static void renderVehicleHud(GuiGraphics gui, Minecraft mc, int width, int height) {
        String text = null;
        int color = -1;
        float target = 0.0F;
        if (mc.player.isSpectator() || ClientPlacementHandler.isPlacing()) {
            target = 0.0F;
        } else if (mc.player.getVehicle() != null) {
            ClientData.VehicleStatus st =
                    ClientData.vehicleStatuses.get(mc.player.getVehicle().getId());
            if (st != null && System.currentTimeMillis() - st.receivedAt < 5000L) {
                text = vehicleStatusText(st);
                if (text != null) {
                    color = vehicleStatusColor(st.kind);
                    target = 1.0F;
                }
            }
        } else if (mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) mc.hitResult).getEntity();
            WarfareWorldData.VehicleRecord record = null;

            for (WarfareWorldData.VehicleRecord r : ClientData.clientVehicles) {
                if (r.uuid.equals(targetEntity.getUUID())) {
                    record = r;
                    break;
                }
            }

            if (record != null && isFriendlyVehicle(record, mc.player)) {
                if (isSpecialistVehicle(record.type) && !hasCorrectKit(record.type, ClientData.myCurrentKit)) {
                    text = "Нужен кит Механик/Пилот";
                    color = -43691;
                } else {
                    text = "Шифт+ПКМ — пополнить БК";
                    color = -1;
                }
                target = 1.0F;
            }
        }

        // 0.2/кадр при 60 FPS, в реальном времени
        vehicleHintAlpha = UIDamp.approach(vehicleHintAlpha, target, 0.2F, VEHICLE_HINT_FRAME.dtMs());
        if (vehicleHintAlpha <= 0.02F || text == null) return;

        int alpha = Math.min(255, (int) (vehicleHintAlpha * 255.0F));
        int textWidth = PWPTheme.Fonts.display().width(text);
        int textX = (width - textWidth) / 2;
        int textY = height - 112; // ярус look-at подсказок (лента h-76/h-94 и фидбек h-58 ниже)
        WHud.contextReserve(textY - 3);
        gui.fill(textX - 8, textY - 3, textX + textWidth + 8, textY + 12, (alpha / 4) << 24);
        drawVehicleHintText(gui, mc, text, textX, textY, color, alpha);
    }

    // Текст с полной прозрачностью (и обводка, и сам текст) для плавного фейда
    private static void drawVehicleHintText(
            GuiGraphics gui, Minecraft mc, String text, int x, int y, int color, int alpha) {
        int outline = alpha << 24;
        int textColor = (alpha << 24) | (color & 0xFFFFFF);
        gui.drawString(PWPTheme.Fonts.display(), text, x - 1, y, outline, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x + 1, y, outline, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y - 1, outline, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y + 1, outline, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y, textColor, false);
    }

    private static String vehicleStatusText(ClientData.VehicleStatus st) {
        int left = Math.max(0, st.secondsLeft - (int) ((System.currentTimeMillis() - st.receivedAt) / 1000L));
        return switch (st.kind) {
            case PacketVehicleStatus.KIND_REARMING -> "Пополнение БК: " + left + "с";
            case PacketVehicleStatus.KIND_LOADING_CRATE -> "Загрузка ящика: " + left + "с";
            case PacketVehicleStatus.KIND_REARM_COOLDOWN -> "Перезарядка БК: " + left + "с";
            case PacketVehicleStatus.KIND_CRATE_RESUPPLY -> "Пополнение: " + left + "с";
            default -> null;
        };
    }

    private static int vehicleStatusColor(int kind) {
        return switch (kind) {
            case PacketVehicleStatus.KIND_LOADING_CRATE -> 16776917;
            case PacketVehicleStatus.KIND_REARM_COOLDOWN -> 16733525;
            default -> 5636095;
        };
    }

    private static boolean isFriendlyVehicle(WarfareWorldData.VehicleRecord record, Player player) {
        if (record.team == null || record.team.isEmpty() || record.team.equalsIgnoreCase("NEUTRAL")) return true;
        String pTeam = player.getTeam() != null ? player.getTeam().getName() : "";
        return record.team.equalsIgnoreCase(pTeam);
    }

    // Те же условия, что на сервере (InteractionEvents) — подсказка информационная,
    // сервер всё равно валидирует посадку
    private static boolean isSpecialistVehicle(String vType) {
        if (vType == null) return false;
        if (vType.equalsIgnoreCase("TANK")) return true;
        if (vType.equalsIgnoreCase("APC")) return true;
        if (vType.equalsIgnoreCase("Mobile ZU")) return true;
        if (vType.equalsIgnoreCase("HELICOPTER")) return true;
        if (vType.toUpperCase().contains("CAS")) return true;
        return vType.toUpperCase().contains("SUPPLY HELICOPTER");
    }

    private static boolean hasCorrectKit(String vType, String kit) {
        if (vType.equalsIgnoreCase("HELICOPTER")
                || vType.toUpperCase().contains("CAS")
                || vType.toUpperCase().contains("SUPPLY HELICOPTER")) {
            return kit.equals("Pilot") || kit.equals("Pilot Officer");
        }
        if (vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU")) {
            return kit.equals("Mechanic") || kit.equals("Mechanic Officer");
        }
        return true;
    }

    // ------------------------------------------------------------ прогресс стройки (контекстный ярус)
    private static boolean buildActive;
    private static float buildProgress;
    private static boolean buildDestroy;
    private static boolean buildEnemy;
    /** Статус под прогрессом (демонтаж/готово/чужая) или null. */
    private static Component buildStatus;

    private static Component buildStatusSrc;
    private static String buildStatusText;
    private static final WHud.Fmt BUILD_PERCENT = new WHud.Fmt(HUD + "percent");

    /**
     * Кадр: собирает состояние стройки (лопата + взгляд на постройку) и подаёт заявку в
     * контекстный ярус. RenderGuiEvent.Pre — до отрисовки остальных заявителей (сброс яруса — в
     * Hud core-client на HIGHEST).
     */
    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        buildActive = false;
        if (mc.player == null || mc.level == null || !WHud.hudVisible()) return;
        computeBuild(mc);
        if (buildActive) WHud.contextOffer(WHud.CTX_CONSTRUCTION);
    }

    private static void computeBuild(Minecraft mc) {
        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        boolean holdingTool = mainHand.getItem() == ModItems.ENTRENCHING_TOOL.get()
                || offHand.getItem() == ModItems.ENTRENCHING_TOOL.get();
        if (holdingTool) {
            if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
                BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
                BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
                if (be != null) {
                    float progress = -1.0F;
                    String structureTeam = "NEUTRAL";
                    boolean finished = false;
                    if (be instanceof HubBlockEntity hub) {
                        progress = hub.getPercentage();
                        structureTeam = hub.getTeam();
                        finished = (Boolean) state.getValue(HubBlock.CONSTRUCTED);
                    } else if (be instanceof VehicleStationBlockEntity supply) {
                        progress = supply.getPercentage();
                        structureTeam = supply.getTeam();
                        finished = (Boolean) state.getValue(VehicleStationBlock.CONSTRUCTED);
                    } else if (be instanceof WallBlockEntity wall) {
                        progress = wall.getPercentage();
                        structureTeam = wall.getTeam();
                        // camo_net — трансформ стены без property CONSTRUCTED: построен.
                        finished = !state.hasProperty(WallBlock.CONSTRUCTED)
                                || (Boolean) state.getValue(WallBlock.CONSTRUCTED);
                    } else if (be instanceof BarbedWireBlockEntity wire) {
                        progress = wire.getPercentage();
                        structureTeam = wire.getTeam();
                        finished = (Boolean) state.getValue(BarbedWireBlock.CONSTRUCTED);
                    } else if (be instanceof M2ConstructionBlockEntity m2) {
                        progress = m2.getPercentage();
                        structureTeam = m2.getTeam();
                    } else if (be instanceof AGSConstructionBlockEntity ags) {
                        progress = ags.getPercentage();
                        structureTeam = ags.getTeam();
                    } else if (be instanceof MortarConstructionBlockEntity mortar) {
                        progress = mortar.getPercentage();
                        structureTeam = mortar.getTeam();
                    } else if (be instanceof TOWConstructionBlockEntity tow) {
                        progress = tow.getPercentage();
                        structureTeam = tow.getTeam();
                    } else if (be instanceof RebConstructionBlockEntity reb) {
                        progress = reb.getPercentage();
                        structureTeam = reb.getTeam();
                    }

                    if (!(progress < 0.0F)) {
                        String playerTeam = mc.player.getTeam() != null
                                ? mc.player.getTeam().getName()
                                : "NEUTRAL";
                        boolean isEnemy = !structureTeam.equals("NEUTRAL")
                                && !structureTeam.equalsIgnoreCase(playerTeam)
                                && !mc.player.isCreative();
                        boolean dismantling = false;
                        if (be instanceof WallBlockEntity w) {
                            dismantling = w.isDismantling();
                        } else if (be instanceof BarbedWireBlockEntity w) {
                            dismantling = w.isDismantling();
                        } else if (be instanceof HubBlockEntity h) {
                            dismantling = h.isDismantling();
                        } else if (be instanceof VehicleStationBlockEntity s) {
                            dismantling = s.isDismantling();
                        }

                        buildActive = true;
                        buildProgress = progress;
                        buildDestroy = finished || dismantling;
                        buildEnemy = isEnemy;
                        if (dismantling) {
                            buildStatus = isEnemy ? BUILD_DESTROYING : BUILD_DISMANTLING;
                        } else if (finished) {
                            buildStatus = isEnemy ? BUILD_ENEMY : BUILD_FINISHED;
                        } else {
                            buildStatus = isEnemy ? BUILD_ENEMY : null;
                        }
                    }
                }
            }
        }
    }

    /** Плашка стройки в контекстном ярусе: иконка, «Строить/Разрушить», статус или %, полоса. */
    private static void renderBuildProgress(GuiGraphics gui) {
        if (!buildActive) return;
        String value;
        if (buildStatus != null) {
            if (buildStatus != buildStatusSrc) {
                buildStatusSrc = buildStatus;
                buildStatusText = buildStatus.getString();
            }
            value = buildStatusText;
        } else {
            BUILD_PERCENT.of(Math.round(Mth.clamp(buildProgress, 0.0F, 1.0F) * 100.0F));
            value = BUILD_PERCENT.string();
        }
        int accent = buildEnemy ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT;
        WHud.contextPlate(
                gui,
                PWPTheme.Fonts.display(),
                WHud.CTX_CONSTRUCTION,
                buildDestroy ? WarfareIcon.TRASH : WarfareIcon.WALL,
                buildDestroy ? BUILD_DESTROY : BUILD_BUILD,
                value,
                buildEnemy ? WTheme.HUD_DANGER : WTheme.HUD_VALUE,
                buildProgress,
                accent,
                1.0F);
    }

    private static void drawOutlinedString(GuiGraphics gui, Minecraft mc, String text, int x, int y, int color) {
        int black = -16777216;
        gui.drawString(PWPTheme.Fonts.display(), text, x - 1, y, black, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x + 1, y, black, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y - 1, black, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y + 1, black, false);
        gui.drawString(PWPTheme.Fonts.display(), text, x, y, color, false);
    }

    private static void renderVotePanel(GuiGraphics gui, Minecraft mc, int width) {
        // 0.05/кадр при 60 FPS, в реальном времени
        ClientData.voteTransition = UIDamp.approach(
                ClientData.voteTransition, ClientData.voteActive ? 1.0F : 0.0F, 0.05F, VOTE_FRAME.dtMs());
        if (!(ClientData.voteTransition <= 0.001F)) {
            int xPos = (int) Mth.lerp(ClientData.voteTransition, -180.0F, 10.0F);
            int yPos = 60;
            String myTeam =
                    mc.player.getTeam() != null ? mc.player.getTeam().getName().toUpperCase() : "NEUTRAL";
            List<MapPlayerInfo> teamPlayers = ClientData.mapPlayers.values().stream()
                    .filter(infox -> infox.team.equalsIgnoreCase(myTeam))
                    .toList();
            int panelWidth = 165;
            int rowHeight = 12;
            int headerHeight = 32;
            int statusHeight = 28;
            int footerHeight = 15;
            int panelHeight = headerHeight + statusHeight + teamPlayers.size() * rowHeight + footerHeight;
            gui.pose().pushPose();
            gui.pose().translate(0.0F, 0.0F, 500.0F);
            gui.fill(xPos, yPos, xPos + panelWidth, yPos + panelHeight, -1442840576);
            gui.renderOutline(xPos, yPos, panelWidth, panelHeight, -1);
            gui.drawCenteredString(PWPTheme.Fonts.display(), VOTE_TITLE, xPos + panelWidth / 2, yPos + 5, -10496);
            int seconds = Math.max(0, ClientData.voteTimer);
            String timeStr = VOTE_CLOCK.of(seconds);
            gui.drawCenteredString(PWPTheme.Fonts.display(), timeStr, xPos + panelWidth / 2, yPos + 16, -1);
            gui.fill(xPos + 5, yPos + 28, xPos + panelWidth - 5, yPos + 29, 1442840575);
            String blueName = ClientData.customBlueName;
            String redName = ClientData.customRedName;
            int blueColor = ClientData.blueReady ? -11141291 : -43691;
            int redColor = ClientData.redReady ? -11141291 : -43691;
            gui.drawString(
                    PWPTheme.Fonts.display(),
                    (ClientData.blueReady ? VOTE_BLUE_READY : VOTE_BLUE_WAITING).of(blueName),
                    xPos + 8,
                    yPos + 32,
                    blueColor,
                    true);
            gui.drawString(
                    PWPTheme.Fonts.display(),
                    (ClientData.redReady ? VOTE_RED_READY : VOTE_RED_WAITING).of(redName),
                    xPos + 8,
                    yPos + 44,
                    redColor,
                    true);
            int currentY = yPos + headerHeight + statusHeight;

            for (MapPlayerInfo info : teamPlayers) {
                Boolean vote = ClientData.votes.get(info.uuid);
                String icon = "\u25CB";
                int iconColor = -5592406;
                if (vote != null) {
                    icon = vote ? "\u2714" : "\u2718";
                    iconColor = vote ? -11141291 : -43691;
                }

                gui.drawString(PWPTheme.Fonts.display(), icon, xPos + 8, currentY, iconColor, true);
                int nameColor = info.name.equals(mc.player.getScoreboardName()) ? -171 : -1;
                gui.drawString(PWPTheme.Fonts.display(), info.name, xPos + 22, currentY, nameColor, true);
                currentY += rowHeight;
            }

            gui.drawCenteredString(
                    PWPTheme.Fonts.display(),
                    VOTE_KEYS.get(),
                    xPos + panelWidth / 2,
                    yPos + panelHeight - 12,
                    -4473925);
            gui.pose().popPose();
        }
    }

    private static void renderProgressBars(GuiGraphics gui, Minecraft mc, int centerX, int y, int teamColor) {
        int barsTotalWidth = 80;
        int barHeight = 4;
        int barsStartX = centerX - barsTotalWidth / 2;
        int barGap = 2;
        int singleBarWidth = (barsTotalWidth - barGap * 3) / 4;

        for (int i = 0; i < 4; i++) {
            int currentBarX = barsStartX + i * (singleBarWidth + barGap);
            gui.fill(currentBarX, y, currentBarX + singleBarWidth, y + barHeight, -12303292);
            float threshold = i * 0.25F;
            if (ClientData.pointProgress > threshold) {
                float fillAmount = Math.min(1.0F, (ClientData.pointProgress - threshold) / 0.25F);
                int fillWidth = (int) (singleBarWidth * fillAmount);
                int finalBarColor = teamColor;
                if (ClientData.pointOwner.equals("NEUTRAL")) {
                    if (ClientData.pointCapturingTeam.equals("BLUE")) {
                        finalBarColor = -13408564;
                    } else if (ClientData.pointCapturingTeam.equals("RED")) {
                        finalBarColor = -3394765;
                    }
                }

                gui.fill(currentBarX, y, currentBarX + fillWidth, y + barHeight, finalBarColor);
            }
        }
    }

    private static void renderCaptureArrows(GuiGraphics gui, Minecraft mc, int centerX, int y) {
        int rate = ClientData.pointCaptureRate;
        if (rate != 0) {
            int absRate = Math.min(Math.abs(rate), 4);
            boolean isForward = rate > 0;
            String capTeam = ClientData.pointCapturingTeam;
            int color = -1;
            if (capTeam.equalsIgnoreCase("BLUE")) {
                color = -13408564;
            } else if (capTeam.equalsIgnoreCase("RED")) {
                color = -3394765;
            }

            RenderSystem.enableBlend();
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(r, g, b, 1.0F);
            int arrowSize = 12;
            int barsStartX = centerX - 40;
            int segmentWidth = 20;

            for (int i = 0; i < absRate; i++) {
                int segmentIndex = isForward ? i : 3 - i;
                int xPos = barsStartX + segmentIndex * segmentWidth + 2;
                int yPos = y - 4;
                if (isForward) {
                    gui.blit(ARROW_TEX, xPos, yPos, 0.0F, 0.0F, arrowSize, arrowSize, arrowSize, arrowSize);
                } else {
                    gui.pose().pushPose();
                    gui.pose().translate(xPos + arrowSize / 2.0, yPos + arrowSize / 2.0, 0.0);
                    gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
                    gui.blit(
                            ARROW_TEX,
                            -arrowSize / 2,
                            -arrowSize / 2,
                            0.0F,
                            0.0F,
                            arrowSize,
                            arrowSize,
                            arrowSize,
                            arrowSize);
                    gui.pose().popPose();
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static long chargeScanTick = Long.MIN_VALUE;
    private static boolean chargeScanResult;

    private static void renderSupplyTruckInfo(GuiGraphics gui, Minecraft mc, int width, int height) {
        Entity ridingEntity = mc.player.getVehicle();
        if (ridingEntity != null) {
            Entity supplyTruck = getSupplyTruckEntity(ridingEntity);
            if (supplyTruck != null) {
                int crates = WarfareData.of(supplyTruck).getInt(WarfareKeys.SUPPLY_AMMO);
                int maxCrates = (Integer) WarfareConfig.SUPPLY_TRUCK_CRATES.get();
                boolean isCharging = false;
                if (crates < maxCrates && ClientData.clientTickCounter == chargeScanTick) {
                    isCharging = chargeScanResult;
                } else if (crates < maxCrates) {
                    BlockPos vPos = supplyTruck.blockPosition();

                    for (BlockPos pos : BlockPos.betweenClosed(vPos.offset(-5, -2, -5), vPos.offset(5, 2, 5))) {
                        if (mc.level.getBlockState(pos).getBlock() instanceof MainSupplyBlock) {
                            isCharging = true;
                            break;
                        }
                    }
                    // скан 11x5x11 блоков — раз в клиентский тик, а не в каждый кадр
                    chargeScanTick = ClientData.clientTickCounter;
                    chargeScanResult = isCharging;
                }

                VALUE_OF_SUPPLY.of(crates, maxCrates);
                boolean empty = crates == 0;
                var stack = WHud.zones().bottomRightStack();
                WHud.stackPlate(
                        gui,
                        PWPTheme.Fonts.display(),
                        stack,
                        WarfareIcon.CRATE,
                        SUPPLY_LABEL,
                        VALUE_OF_SUPPLY.string(),
                        empty ? WTheme.HUD_DANGER : WTheme.HUD_VALUE,
                        maxCrates > 0 ? (float) crates / maxCrates : -1.0F,
                        empty ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT,
                        1.0F);
                if (isCharging) {
                    WHud.stackPlate(
                            gui,
                            PWPTheme.Fonts.display(),
                            stack,
                            WarfareIcon.PLUS,
                            SUPPLY_LOADING,
                            null,
                            WTheme.HUD_POSITIVE,
                            -1.0F,
                            WTheme.HUD_POSITIVE,
                            1.0F);
                }
            }
        }
    }

    private static Entity getSupplyTruckEntity(Entity entity) {
        if (WarfareData.of(entity).getBoolean(WarfareKeys.IS_SUPPLY_TRUCK)) {
            return entity;
        }

        Entity parent = entity.getVehicle();
        return parent != null && WarfareData.of(parent).getBoolean(WarfareKeys.IS_SUPPLY_TRUCK) ? parent : null;
    }

    private static void renderVehicleAmmo(GuiGraphics gui, Minecraft mc, int width, int height) {
        Entity vehicle = mc.player.getVehicle();
        int currentAmmo = -1;
        int maxAmmo = -1;
        if (vehicle instanceof M2BrowningEntity m2) {
            currentAmmo = m2.getAmmoCount();
            maxAmmo = 200;
        } else if (vehicle instanceof AGS30Entity ags) {
            currentAmmo = ags.getAmmoCount();
            maxAmmo = 30;
        }

        if (currentAmmo != -1) {
            VALUE_OF_AMMO.of(currentAmmo, maxAmmo);
            boolean empty = currentAmmo == 0;
            // низ-лево над полосой HP (pwp-limits занимает низ стека раньше — priority HIGH)
            WHud.stackPlate(
                    gui,
                    PWPTheme.Fonts.display(),
                    WHud.zones().bottomLeftStack(),
                    WarfareIcon.GUN,
                    AMMO_LABEL,
                    VALUE_OF_AMMO.string(),
                    empty ? WTheme.HUD_DANGER : WTheme.HUD_VALUE,
                    maxAmmo > 0 ? (float) currentAmmo / maxAmmo : -1.0F,
                    empty ? WTheme.HUD_DANGER : PWPTheme.Colors.ACCENT,
                    1.0F);
        }
    }

    // Список ноков у медика: ТОЛЬКО союзники (раньше были видны и вражеские ноки
    // с дистанцией; мёртвый дубль renderDownedUI с фильтром удалён).
    private static void renderMedicUI(GuiGraphics gui, Minecraft mc, int width) {
        if (mc.player.getTeam() == null) return;
        int yOffset = 60;
        int row = 0;

        for (Integer id : ClientData.DOWNED_PLAYERS) {
            if (mc.level.getEntity(id) instanceof Player downed
                    && downed != mc.player
                    && downed.getTeam() != null
                    && mc.player.getTeam().isAlliedTo(downed.getTeam())) {
                int distance = (int) mc.player.distanceTo(downed);
                if (distance < 100) {
                    Component text = medicRow(row++).of(downed.getScoreboardName(), distance);
                    int x = width - PWPTheme.Fonts.display().width(text) - 10;
                    gui.fill(x - 2, yOffset - 1, width - 5, yOffset + 9, -2130771968);
                    gui.drawString(PWPTheme.Fonts.display(), text, x, yOffset, 16777215, false);
                    yOffset += 12;
                }
            }
        }
    }

    private static void renderSideMap(
            GuiGraphics gui, Minecraft mc, int screenWidth, int screenHeight, float partialTick) {
        if (HUD_SIDE_MAP == null) {
            HUD_SIDE_MAP = new SquadMapRenderer();
        }

        float speed = 0.12F;
        float target = ClientData.isMapOpen ? 1.0F : 0.0F;
        ClientData.mapTransition = Mth.lerp(speed, ClientData.mapTransition, target);
        if (!ClientData.isMapOpen && ClientData.mapTransition < 0.001F) {
            ClientData.mapTransition = 0.0F;
        } else {
            // настройка «Размер боковой карты»
            int mapSize = (int) ((screenHeight - 60) / 1.3F * WarfareUiSettings.SIDE_MAP_SIZE.get());
            int topBarHeight = 35;
            int sidePadding = 15;
            int bottomPadding = 5;
            int containerWidth = mapSize + sidePadding * 2;
            int containerHeight = mapSize + topBarHeight + bottomPadding;
            float hiddenX = screenWidth + 20.0F;
            float visibleX = (float) screenWidth - containerWidth - 10.0F;
            int xPos = (int) Mth.lerp(ClientData.mapTransition, hiddenX, visibleX);
            int yPos = (screenHeight - containerHeight) / 2;
            // настройка «Фон боковой карты» — множитель альфы подложки 0xAA000000
            gui.fill(
                    xPos,
                    yPos,
                    xPos + containerWidth,
                    yPos + containerHeight,
                    com.pwp.coreclient.ui.UIDraw.fade(-1442840576, WarfareUiSettings.SIDE_MAP_BG.get()));
            renderMinimapStatus(gui, mc, xPos, yPos, containerWidth, topBarHeight);
            HUD_SIDE_MAP.init(xPos + sidePadding, yPos + topBarHeight, mapSize, mapSize);
            HUD_SIDE_MAP.render(gui, -1, -1, partialTick);
        }
    }

    private static void renderMinimapStatus(
            GuiGraphics gui, Minecraft mc, int x, int y, int containerWidth, int topBarHeight) {
        String team = "NEUTRAL";
        if (mc.player.getTeam() != null) {
            team = mc.player.getTeam().getName().toUpperCase();
        }

        int tickets = team.equals("BLUE") ? ClientData.BLUE_TICKETS : (team.equals("RED") ? ClientData.RED_TICKETS : 0);
        String faction =
                team.equals("BLUE") ? ClientData.BLUE_FACTION : (team.equals("RED") ? ClientData.RED_FACTION : "none");
        String tText = String.valueOf(tickets);
        int flagW = 22;
        int flagH = 13;
        int iconSize = 12;
        int textW = PWPTheme.Fonts.display().width(tText);
        int gap = 6;
        int totalContentWidth = flagW + gap + iconSize + gap + textW;
        int startX = x + containerWidth / 2 - totalContentWidth / 2;
        int contentY = y + topBarHeight / 2 - flagH / 2;
        ResourceLocation flagTex = getFlagTexture(faction);
        if (flagTex != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            gui.blit(flagTex, startX, contentY, flagW, flagH, 0.0F, 0.0F, 64, 36, 64, 36);
        }

        ResourceLocation ticketIcon = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");
        int iconX = startX + flagW + gap;
        RenderSystem.enableBlend();
        gui.blit(ticketIcon, iconX, contentY, iconSize, iconSize, 0.0F, 0.0F, 16, 16, 16, 16);
        int textX = iconX + iconSize + gap;
        gui.drawString(PWPTheme.Fonts.display(), tText, textX, contentY + 2, -1, true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Флаг фракции (64x36) или null; общий для боковой карты и верха HUD. */
    public static ResourceLocation getFlagTexture(String faction) {
        if (faction == null) {
            return null;
        }

        switch (faction.toLowerCase()) {
            case "ukraine":
                return FLAG_UKRAINE;
            case "russia":
                return FLAG_RUSSIA;
            case "usa":
                return FLAG_USA;
            case "nato":
                return FLAG_NATO;
            case "bluefor":
                return FLAG_BLUEFOR;
            case "redfor":
                return FLAG_REDFOR;
            case "insurgency":
                return FLAG_INSURGENCY;
            case "pmc":
                return FLAG_PMC;
            default:
                return null;
        }
    }
}
