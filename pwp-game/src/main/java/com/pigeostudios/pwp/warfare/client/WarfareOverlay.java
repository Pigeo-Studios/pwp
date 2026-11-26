package com.pigeostudios.pwp.warfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
import com.pigeostudios.pwp.warfare.client.hud.SideHud;
import com.pigeostudios.pwp.warfare.client.hud.TopHud;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.entity.AGS30Entity;
import com.pigeostudios.pwp.warfare.entity.M2BrowningEntity;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
// Основной оверлей мода (после CHAT_PANEL): верх — TopHud, боковые стеки — SideHud, плашки БК/припасов и
// стройка — кит WHud; на старых координатах остались только look-at подсказки (материалы хаба/ящика,
// техника), Build Mode, виньетка нока и боковая карта (client/gui/map). Центр (дрон) — CenterHud.
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
    private static SquadMapRenderer HUD_SIDE_MAP;
    private static final ResourceLocation MOUSE_LEFT = new ResourceLocation("pwpwarfare", "textures/gui/dig_icon.png");
    private static final ResourceLocation MOUSE_RIGHT =
            new ResourceLocation("pwpwarfare", "textures/gui/build_icon.png");
    private static final ResourceLocation ICON_ROTATE =
            new ResourceLocation("pwpwarfare", "textures/gui/icon_rotate.png");
    private static final ResourceLocation ICON_CONFIRM =
            new ResourceLocation("pwpwarfare", "textures/gui/icon_confirm.png");

    // ------------------------------------------------------------ строки HUD (lang, кэш компонентов)
    private static final String HUD = "gui.pwpwarfare.hud.";
    private static final TopHud TOP = new TopHud();
    private static final TopHud.Model TOP_MODEL = new TopHud.Model();
    private static final SideHud SIDE = new SideHud();
    private static final SideHud.Model SIDE_MODEL = new SideHud.Model();
    private static final Component BUILD_DESTROY = Component.translatable(HUD + "build.destroy");
    private static final Component BUILD_BUILD = Component.translatable(HUD + "build.build");
    private static final Component BUILD_DESTROYING = Component.translatable(HUD + "build.destroying");
    private static final Component BUILD_DISMANTLING = Component.translatable(HUD + "build.dismantling");
    private static final Component BUILD_ENEMY = Component.translatable(HUD + "build.enemy");
    private static final Component BUILD_FINISHED = Component.translatable(HUD + "build.finished");
    private static final Component SUPPLY_LABEL = Component.translatable(HUD + "supply.label");
    private static final WHud.Fmt VALUE_OF_SUPPLY = new WHud.Fmt(HUD + "value_of");
    private static final Component SUPPLY_LOADING = Component.translatable(HUD + "supply.loading");
    private static final Component AMMO_LABEL = Component.translatable(HUD + "ammo.label");
    private static final WHud.Fmt VALUE_OF_AMMO = new WHud.Fmt(HUD + "value_of");

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
        // боковые стеки (этап 4): голосования, арта, раненые у медика, голос; кровотечение
        // (pwp-medicine) заняло верх левого стека ещё в RenderGuiEvent.Pre
        SideHud.fromClient(SIDE_MODEL, mc);
        SIDE.render(gui, PWPTheme.Fonts.display(), WHud.zones(), SIDE_MODEL);
        // этап 4 (gameplay-setup): «ПОДГОТОВКА · М:СС» + плашка канала рации
        ClientStaging.renderHud(gui, width);

        renderVehicleAmmo(gui, mc, width, height);
        renderSupplyTruckInfo(gui, mc, width, height);
        renderPlacementHints(gui, mc, width, height);
        // подсказки при наведении — LookPromptHud (единая плашка); Build Mode резервирует низ-центр, стройка — над ним
        renderBuildProgress(gui);
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
            gui.drawCenteredString(
                    PWPTheme.Fonts.display(),
                    I18n.get("gui.pwpwarfare.hud.build_mode"),
                    xStart + uiWidth / 2,
                    yStart + 4,
                    goldLight);
            RenderSystem.enableBlend();
            int row1Y = yStart + 16;
            gui.blit(MOUSE_LEFT, xStart + 8, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
            gui.drawString(
                    PWPTheme.Fonts.display(),
                    I18n.get("gui.pwpwarfare.hud.build_rotate"),
                    xStart + 26,
                    row1Y + 2,
                    -1,
                    true);
            gui.blit(ICON_ROTATE, xStart + uiWidth - 20, row1Y, 0.0F, 0.0F, 12, 12, 12, 12);
            int row2Y = yStart + 30;
            gui.blit(MOUSE_RIGHT, xStart + 8, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
            gui.drawString(
                    PWPTheme.Fonts.display(),
                    I18n.get("gui.pwpwarfare.hud.build_confirm"),
                    xStart + 26,
                    row2Y + 2,
                    -1,
                    true);
            gui.blit(ICON_CONFIRM, xStart + uiWidth - 20, row2Y, 0.0F, 0.0F, 12, 12, 12, 12);
            RenderSystem.disableBlend();
        }
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

    // ------------------------------------------------------------ геометрия боковой карты
    // Прежние значения renderSideMap; публичны для HUD (SideHud ужимает правый стек над картой).
    private static final int SIDE_MAP_TOP_BAR = 35;
    private static final int SIDE_MAP_SIDE_PAD = 15;
    private static final int SIDE_MAP_BOTTOM_PAD = 5;
    private static final int SIDE_MAP_EDGE = 10;

    /** Сторона карты (настройка «Размер боковой карты»). */
    public static int sideMapSize(int screenHeight) {
        return (int) ((screenHeight - 60) / 1.3F * WarfareUiSettings.SIDE_MAP_SIZE.get());
    }

    /** Ширина контейнера боковой карты (GUI-пиксели). */
    public static int sideMapWidth(int screenHeight) {
        return sideMapSize(screenHeight) + SIDE_MAP_SIDE_PAD * 2;
    }

    public static int sideMapHeight(int screenHeight) {
        return sideMapSize(screenHeight) + SIDE_MAP_TOP_BAR + SIDE_MAP_BOTTOM_PAD;
    }

    /** X контейнера в конечном (открытом) положении. */
    public static int sideMapX(int screenWidth, int screenHeight) {
        return screenWidth - sideMapWidth(screenHeight) - SIDE_MAP_EDGE;
    }

    public static int sideMapY(int screenHeight) {
        return (screenHeight - sideMapHeight(screenHeight)) / 2;
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
            int mapSize = sideMapSize(screenHeight);
            int topBarHeight = SIDE_MAP_TOP_BAR;
            int sidePadding = SIDE_MAP_SIDE_PAD;
            int containerWidth = sideMapWidth(screenHeight);
            float hiddenX = screenWidth + 20.0F;
            float visibleX = sideMapX(screenWidth, screenHeight);
            int xPos = (int) Mth.lerp(ClientData.mapTransition, hiddenX, visibleX);
            int yPos = sideMapY(screenHeight);
            int containerHeight = sideMapHeight(screenHeight);
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

        ResourceLocation ticketIcon = com.pigeostudios.pwp.warfare.client.Tex.of("textures/gui/minimap_tickets.png");
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
