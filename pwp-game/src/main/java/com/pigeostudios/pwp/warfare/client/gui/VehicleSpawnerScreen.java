package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.menu.VehicleSpawnerMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketUpdateSpawner;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Настройка спавнера техники (контейнер). Слоты ставит {@link VehicleSpawnerMenu} (модификатор
 * 15,45; груз 8x4 от 26,75; инвентарь игрока от 17,160) — здесь только отрисовка вокруг них.
 *
 * <p>Раскладка (от угла панели): шапка 0..16; слева ID техники и рыскание рядом со слотом
 * модификатора, справа таймеры «−/поле/+» (возрождение и начальный); ниже груз и инвентарь.
 */
public class VehicleSpawnerScreen extends AbstractContainerScreen<VehicleSpawnerMenu> {
    private EditBox vehicleIdField;
    private EditBox respawnTimeField;
    private EditBox initialTimeField;
    private EditBox yawField;
    private final UILayer layer = new UILayer();

    private static final int PAD = Spacing.XS + Spacing.XXS;
    /** Левая колонка: строка ID и строка рыскания (рядом со слотом модификатора). */
    private static final int ROW_ID = WTheme.CONTAINER_HEADER_H + Spacing.XS;

    private static final int CAPTION_Y = ROW_ID + WTheme.FIELD_H + Spacing.XXS;
    private static final int ROW_YAW = CAPTION_Y + Spacing.SM + Spacing.XXS;
    private static final int YAW_X = 36;
    private static final int YAW_W = 36;
    /** Правая колонка таймеров. */
    private static final int RIGHT_X = 112;

    private static final int ROW_RESPAWN = ROW_ID + Spacing.SM + Spacing.XXS;
    private static final int ROW_INITIAL = ROW_RESPAWN + WTheme.FIELD_H + Spacing.XXS + Spacing.SM + Spacing.XXS;

    public VehicleSpawnerScreen(VehicleSpawnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WTheme.SPAWNER_W;
        this.imageHeight = WTheme.SPAWNER_H;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    protected void init() {
        super.init();
        layer.clear();
        int x = leftPos;
        int y = topPos;
        var be = this.menu.blockEntity;

        int idX = x + PAD + font.width(tr("id")) + Spacing.XS;
        this.vehicleIdField = field(idX, y + ROW_ID, x + RIGHT_X - Spacing.SM - idX, tr("vehicle_id"));
        this.vehicleIdField.setMaxLength(64);
        this.vehicleIdField.setValue(be.vehicleIdString);
        this.vehicleIdField.setResponder(val -> this.sendUpdatePacket());

        this.yawField = field(x + YAW_X, y + ROW_YAW, YAW_W, tr("yaw"));
        this.yawField.setValue(String.valueOf((int) be.vehicleYaw));
        this.yawField.setFilter(s -> s.matches("-?\\d*"));
        this.yawField.setResponder(val -> {
            // Промежуточный ввод («», «-») пропускаем — обновляем только корректное число
            float yaw = Parse.floatOr(val, Float.NaN);
            if (!Float.isNaN(yaw)) {
                this.menu.blockEntity.vehicleYaw = yaw;
                this.sendUpdatePacket();
            }
        });
        int rx = x + YAW_X + YAW_W + Spacing.XXS;
        step(rx, y + ROW_YAW, "↺", () -> this.adjustYaw(-45.0F));
        step(rx + WTheme.STEP_BUTTON_W + Spacing.XXS, y + ROW_YAW, "↻", () -> this.adjustYaw(45.0F));

        int tw = imageWidth - RIGHT_X - PAD - 2 * (WTheme.STEP_BUTTON_W + Spacing.XXS);
        int fx = x + RIGHT_X + WTheme.STEP_BUTTON_W + Spacing.XXS;
        step(x + RIGHT_X, y + ROW_RESPAWN, "-", () -> this.adjustTimer(false, -5));
        this.respawnTimeField = field(fx, y + ROW_RESPAWN, tw, tr("respawn_time"));
        this.respawnTimeField.setValue(String.valueOf(be.respawnTimeSettings));
        this.respawnTimeField.setResponder(val -> this.onTimeFieldChanged(val, false));
        step(fx + tw + Spacing.XXS, y + ROW_RESPAWN, "+", () -> this.adjustTimer(false, 5));

        step(x + RIGHT_X, y + ROW_INITIAL, "-", () -> this.adjustTimer(true, -5));
        this.initialTimeField = field(fx, y + ROW_INITIAL, tw, tr("initial_time"));
        this.initialTimeField.setValue(String.valueOf(be.initialTimeSettings));
        this.initialTimeField.setResponder(val -> this.onTimeFieldChanged(val, true));
        step(fx + tw + Spacing.XXS, y + ROW_INITIAL, "+", () -> this.adjustTimer(true, 5));
    }

    private static Component tr(String key) {
        return Component.translatable("gui.pwpwarfare.vehicle_spawner." + key);
    }

    /** EditBox без рамки поверх подложки {@link WDraw#field} (прямоугольник x,y,w,FIELD_H). */
    private EditBox field(int x, int y, int w, Component narration) {
        EditBox box = new EditBox(
                font,
                x + Spacing.XXS,
                y + (WTheme.FIELD_H - font.lineHeight) / 2 + 1,
                w - Spacing.XS,
                font.lineHeight,
                narration);
        box.setBordered(false);
        addRenderableWidget(box);
        return box;
    }

    private static Rect fieldRect(EditBox box) {
        return Rect.of(
                box.getX() - Spacing.XXS,
                box.getY() - (WTheme.FIELD_H - box.getHeight()) / 2 - 1,
                box.getWidth() + Spacing.XS,
                WTheme.FIELD_H);
    }

    private void step(int x, int y, String label, Runnable run) {
        UIButton b = new UIButton(Component.literal(label), run).style(UIButton.Style.DARK);
        b.setBounds(x, y, WTheme.STEP_BUTTON_W, WTheme.FIELD_H);
        layer.add(b);
    }

    private void onTimeFieldChanged(String value, boolean isInitial) {
        if (!value.isEmpty()) {
            int time = Parse.intOr(value, Integer.MIN_VALUE);
            if (time == Integer.MIN_VALUE) return; // не число — ждём корректный ввод
            if (time < 0) time = 0;
            if (isInitial) {
                this.menu.blockEntity.initialTimeSettings = time;
            } else {
                this.menu.blockEntity.respawnTimeSettings = time;
            }
            this.sendUpdatePacket();
        }
    }

    private void adjustTimer(boolean isInitial, int change) {
        if (isInitial) {
            int newVal = Math.max(0, this.menu.blockEntity.initialTimeSettings + change);
            this.menu.blockEntity.initialTimeSettings = newVal;
            this.initialTimeField.setValue(String.valueOf(newVal));
        } else {
            int newVal = Math.max(0, this.menu.blockEntity.respawnTimeSettings + change);
            this.menu.blockEntity.respawnTimeSettings = newVal;
            this.respawnTimeField.setValue(String.valueOf(newVal));
        }
        this.sendUpdatePacket();
    }

    private void sendUpdatePacket() {
        if (this.menu.blockEntity.vehicleIdString == null) this.menu.blockEntity.vehicleIdString = "";
        PacketHandler.INSTANCE.sendToServer(new PacketUpdateSpawner(
                this.menu.blockEntity.getBlockPos(),
                this.menu.blockEntity.respawnTimeSettings,
                this.menu.blockEntity.initialTimeSettings,
                this.vehicleIdField.getValue(),
                this.menu.blockEntity.vehicleYaw));
    }

    private void adjustYaw(float amount) {
        float current = Parse.floatOr(this.yawField.getValue(), 0.0F);
        float next = (current + amount) % 360.0F;
        if (next < 0.0F) next += 360.0F;
        this.yawField.setValue(String.valueOf((int) next));
    }

    // ================================================================ рисование

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        Rect panel = Rect.of(x, y, imageWidth, imageHeight);
        UIDraw.glass(gui, panel, 1F, true);
        int cargo = menu.slots.size() - 1 - 36; // минус модификатор и инвентарь игрока
        WDraw.header(
                gui,
                font,
                panel.top(WTheme.CONTAINER_HEADER_H),
                WarfareIcon.EDIT,
                getTitle(),
                Component.translatable("gui.pwpwarfare.vehicle_spawner.cargo", cargo),
                1F);

        // --- подписи и поля
        Rect idField = fieldRect(vehicleIdField);
        int iw = font.width(tr("id"));
        WDraw.text(
                gui,
                font,
                tr("id"),
                Rect.of(idField.x() - Spacing.XS - iw, idField.y(), iw, idField.h()),
                Align.START,
                PWPTheme.Text.MUTED,
                false,
                1F);
        Slot modifier = menu.slots.get(0);
        int capH = font.lineHeight;
        Rect modCaption = Rect.of(x + PAD, y + CAPTION_Y, YAW_X - PAD - Spacing.XXS, capH);
        WDraw.text(gui, font, tr("modifier"), modCaption, Align.START, PWPTheme.Text.ACCENT, false, 1F);
        Rect yawCaption = Rect.of(x + YAW_X, y + CAPTION_Y, RIGHT_X - YAW_X - Spacing.SM, capH);
        WDraw.text(gui, font, tr("yaw"), yawCaption, Align.START, PWPTheme.Text.MUTED, false, 1F);
        Rect timers = Rect.of(x + RIGHT_X, 0, imageWidth - RIGHT_X - PAD, capH);
        WDraw.text(
                gui,
                font,
                Component.translatable("gui.pwpwarfare.vehicle_spawner.seconds", tr("respawn_time")),
                timers.translate(0, y + ROW_RESPAWN - capH - 1),
                Align.START,
                PWPTheme.Text.MUTED,
                false,
                1F);
        WDraw.text(
                gui,
                font,
                Component.translatable("gui.pwpwarfare.vehicle_spawner.seconds", tr("initial_time")),
                timers.translate(0, y + ROW_INITIAL - capH - 1),
                Align.START,
                PWPTheme.Text.MUTED,
                false,
                1F);
        UIDraw.vline(
                gui,
                x + RIGHT_X - Spacing.XS,
                y + ROW_ID,
                y + ROW_INITIAL + WTheme.FIELD_H,
                PWPTheme.Styles.Glass.HEADER_DIVIDER);
        for (EditBox b : new EditBox[] {vehicleIdField, yawField, respawnTimeField, initialTimeField})
            WDraw.field(gui, fieldRect(b), b.isFocused(), 1F);

        // --- подложки слотов (модификатор — с акцентной обводкой)
        for (Slot s : menu.slots) {
            WDraw.well(gui, Rect.of(x + s.x - 1, y + s.y - 1, WTheme.SLOT_WELL, WTheme.SLOT_WELL), 1F);
        }
        UIDraw.outline(
                gui,
                Rect.of(x + modifier.x - 1, y + modifier.y - 1, WTheme.SLOT_WELL, WTheme.SLOT_WELL),
                PWPTheme.Colors.ACCENT_DIM);

        // --- инвентарь игрока
        Slot firstInv = menu.slots.get(menu.slots.size() - 36);
        int invX = x + firstInv.x - 1;
        Rect invRow = Rect.of(invX, y + firstInv.y - capH - Spacing.XXS, x + imageWidth - PAD - invX, capH);
        WDraw.section(gui, font, invRow, Component.translatable("container.inventory"), PWPTheme.Text.MUTED, 1F);

        layer.render(gui, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }
}
