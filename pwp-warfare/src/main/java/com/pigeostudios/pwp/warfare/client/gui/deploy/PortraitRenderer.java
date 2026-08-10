package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.authlib.GameProfile;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Портрет в стиле Squad: кукла-СТАТУЯ — рендерит ОТДЕЛЬНУЮ сущность-клона
 * ({@link DummyPlayer}), а НЕ живого игрока. Клон не в мире, не тикается →
 * все поля поворота всегда статичны (0): кукла НЕ повторяет повороты/позы
 * реального персонажа, который живёт за меню в хабе.
 *
 * <p>Бонус: живого игрока вообще не мутируем (нет swap/restore предметов) —
 * превью-пушка физически не может «остаться» в руках у реального персонажа.</p>
 *
 * <p>Драг вращает по оси Y с низкой сенсой ({@link #ROTATE_PER_PIXEL}),
 * ПКМ сбрасывает ракурс. Масштаб — от обеих сторон бокса, кукла всегда
 * полностью внутри панели. Скин — тот же GameProfile → тот же PlayerInfo →
 * та же текстура, что у игрока.</p>
 */
public class PortraitRenderer {

    /** Базовый доворот куклы от анфаса (0 = строго лицом к камере). */
    private static final float DOLL_YAW = 0f;
    /** Сенса вращения: градусов на пиксель драга. */
    private static final float ROTATE_PER_PIXEL = 0.4f;
    /** Доля бокса, которую занимает силуэт (рост 1.8 блока). */
    private static final float FILL = 0.85f;

    /** Пользовательский доворот куклы, без ограничений; при рендере — % 360. */
    private float userYaw;
    /** Идёт ли драг-вращение (ЛКМ зажата). */
    private boolean rotating;

    /** Кукла-клон: пере-создаётся при смене уровня. */
    private DummyPlayer doll;

    /** Последнее одетое оружие/броня — для кэша одежды (НЕ переодеваем каждый кадр). */
    private ItemStack dressedWeapon = ItemStack.EMPTY;
    private List<ItemStack> dressedArmor = List.of();

    public boolean isRotating() { return rotating; }

    public void render(GuiGraphics gui, int x, int y, int w, int h, int mx, int my,
                       String desc, ItemStack weapon, List<ItemStack> armorPieces) {
        int panelH = h;

        RoundedRect.fill(gui, x, y, w, panelH, 6, 0xFF0E1117);
        RoundedRect.border(gui, x, y, w, panelH, 6, 1, PWPTheme.Colors.BORDER);

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (!ensureDoll(mc, level)) return;

        // Масштаб по обеим осям бокса: рост 1.8 блока, ширина 1.0 — кукла не вылезает
        int sc = Math.max(1, Math.min((int) (panelH * FILL / 1.8f), (int) (w * FILL / 1.0f)));
        int entityX = x + w / 2;
        // Ступни у нижнего края панели (vanilla рендерит ноги в точке y).
        int entityY = y + panelH - 12;
        float yaw = (180f + DOLL_YAW + userYaw) % 360f;

        // Одеваем ТОЛЬКО при смене снаряжения: setItemSlot каждый кадр пересобирает
        // рендер-состояние рук/брони и даёт микродёрганье позы.
        if (!isSameGear(weapon, armorPieces)) {
            dress(doll, weapon, armorPieces);
            dressedWeapon = weapon.isEmpty() ? ItemStack.EMPTY : weapon.copy();
            dressedArmor = armorPieces == null ? List.of()
                : armorPieces.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
        }

        freeze(doll);

        markHighDetail();

        gui.enableScissor(x, y, x + w, y + panelH);
        try {
            // 1.20.1 принимает кватернионы позы: разворот в ¾ (rotateZ PI) + пользовательский yaw.
            var pose = new org.joml.Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateY(yaw * (float) Math.PI / 180f);
            InventoryScreen.renderEntityInInventory(gui, entityX, entityY, sc,
                    pose, new org.joml.Quaternionf(), doll);
        } catch (Exception ignored) {}
        gui.disableScissor();
    }

    private static void markHighDetail() {
        try {
            WeaponPreviewRenderer.TaczHolder.markGuiRenderTimestamp();
        } catch (LinkageError ignored) {}
    }

    private boolean isSameGear(ItemStack weapon, List<ItemStack> armor) {
        if (!ItemStack.isSameItemSameTags(weapon, dressedWeapon)) return false;
        List<ItemStack> a = armor == null ? List.of()
            : armor.stream().filter(s -> !s.isEmpty()).toList();
        if (a.size() != dressedArmor.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameTags(a.get(i), dressedArmor.get(i))) return false;
        }
        return true;
    }

    /** Создать/обновить клона: при смене уровня или игрока. */
    private boolean ensureDoll(Minecraft mc, ClientLevel level) {
        if (mc.player == null) return false;
        if (doll == null || doll.level() != level || !doll.getUUID().equals(mc.player.getUUID())) {
            doll = new DummyPlayer(level, mc.player.getGameProfile());
            // Клон не в мире и не тикается — но на всякий случай выставим статичную позу
            freeze(doll);
        }
        return true;
    }

    /**
     * Жёсткая заморозка позы: клон никогда не тикается, однако рендер может читать
     * lerp-пары (O-поля) и walk-анимацию — явно обнуляем всё, чтобы кадры были
     * пиксель-идентичны (защита от любых микро-мутаций извне).
     */
    private static void freeze(DummyPlayer d) {
        d.setYRot(0);
        d.setYHeadRot(0);
        d.setYBodyRot(0);
        d.setXRot(0);
        d.hurtTime = 0;
        d.deathTime = 0;
        try { d.walkAnimation.setSpeed(0); } catch (Exception ignored) {}
    }

    public void startRotate() { rotating = true; }

    public void stopRotate() { rotating = false; }

    /** Драг-вращение: без ограничений, значение оборачивается при рендере. */
    public void rotateBy(float dragX) {
        userYaw += dragX * ROTATE_PER_PIXEL;
    }

    /** ПКМ — вернуть стандартный ракурс. */
    public void resetView() {
        userYaw = 0f;
        rotating = false;
    }

    /** Одеть клона предметами кита (клон не тикается — предметы хранятся стабильно). */
    private static void dress(DummyPlayer d, ItemStack weapon, List<ItemStack> armor) {
        d.setItemSlot(EquipmentSlot.MAINHAND, weapon.isEmpty() ? ItemStack.EMPTY : weapon.copy());
        d.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getType() != EquipmentSlot.Type.ARMOR) continue;
            d.setItemSlot(s, ItemStack.EMPTY);
        }
        if (armor != null) {
            for (ItemStack a : armor) {
                if (a.isEmpty()) continue;
                var item = a.getItem();
                if (item instanceof ArmorItem ai) {
                    d.setItemSlot(ai.getEquipmentSlot(), a.copy());
                }
            }
        }
    }

    /**
     * Кукла-статуя: обычный AbstractClientPlayer с тем же профилем (тот же скин),
     * но НЕ добавленный в мир — поля поворота никогда не меняются, анимации пусты.
     */
    private static final class DummyPlayer extends AbstractClientPlayer {
        DummyPlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isSpectator() { return false; }

        @Override
        public boolean isCreative() { return false; }
    }
}