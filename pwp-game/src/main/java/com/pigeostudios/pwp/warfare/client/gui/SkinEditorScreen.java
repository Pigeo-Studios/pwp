package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.admin.AdminFormScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.network.AdminActions;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Админ: добавление/редактирование скина. Форма на {@link AdminFormScreen}: название, слот,
 * редкость (выбор по кругу), тег оружия; превью предмета; сетка инвентаря 9x4 — клик по
 * предмету берёт его как модель (SNBT) и угадывает слот/тег, как раньше. Сохранение/удаление —
 * {@link AdminActions#saveSkin}/{@link AdminActions#deleteSkin} с теми же аргументами.
 */
public class SkinEditorScreen extends AdminFormScreen {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(SkinEditorScreen.class);
    private static final String[] SLOT_TYPES = {"WEAPON", "PRIMARY", "SECONDARY", "KNIFE", "MELEE", "UNIFORM"};
    private static final String[] RARITIES = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
    private static final int[] RARITY_COLORS = {
        Colors.RARITY_COMMON,
        Colors.RARITY_RARE,
        Colors.RARITY_EPIC,
        Colors.RARITY_LEGENDARY,
        Colors.RARITY_MYTHIC,
        0xFFFFD700
    };
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 4;
    private static final int SLOT = 18;

    private final SkinListScreen parent;
    private EditBox nameBox;
    private UIButton slotButton;
    private String slotType = "PRIMARY";
    private String weaponTag = "any";
    private String rarity = "COMMON";
    private ItemStack previewItem = ItemStack.EMPTY;
    private String previewRegistryName = "";
    private String previewItemName = "";
    private int previewSlotIndex = -1;
    private String editSkinId;
    private String pendingEditName;
    private String pendingEditRegistryName;
    private String nameValue = "";
    /** Источник предметов сетки (в игре — инвентарь игрока; в тест-стенде — фикстура). */
    private Supplier<List<ItemStack>> items = () -> {
        var p = Minecraft.getInstance().player;
        return p != null ? p.getInventory().items : List.of();
    };

    private Rect gridR = Rect.of(0, 0, 0, 0);

    public SkinEditorScreen(SkinListScreen parent) {
        super(Component.translatable("gui.pwpwarfare.skin_editor.add"));
        this.parent = parent;
    }

    public void editExisting(String skinId, String name, String st, String wt, String r, String registryName) {
        editSkinId = skinId;
        slotType = st;
        weaponTag = wt;
        rarity = r;
        pendingEditName = name;
        pendingEditRegistryName = registryName;
    }

    /** Тест-стенд UI: предметы из фикстуры; сохранение/удаление не отправляются. */
    public SkinEditorScreen previewItems(List<ItemStack> fixture) {
        this.items = () -> fixture;
        asPreview();
        return this;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                editSkinId != null ? "gui.pwpwarfare.skin_editor.edit" : "gui.pwpwarfare.skin_editor.add");
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.STAR;
    }

    @Override
    protected void build() {
        if (pendingEditName != null) {
            nameValue = pendingEditName;
            if (pendingEditRegistryName != null && pendingEditRegistryName.startsWith("{")) {
                previewRegistryName = pendingEditRegistryName;
                try {
                    previewItem = ItemStack.of(TagParser.parseTag(pendingEditRegistryName));
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(previewItem.getItem());
                    previewItemName = rl != null ? rl.toString() : "unknown";
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                    PWP_LOG.warn("SkinEditorScreen: некорректный SNBT модели {}", editSkinId, e);
                }
            }
            pendingEditName = null;
        }
        section(Component.translatable("gui.pwpwarfare.skin_editor.section.info"));
        nameBox = text(Component.translatable("gui.pwpwarfare.skin_editor.name"), nameValue, 64);
        nameBox.setHint(Component.translatable("gui.pwpwarfare.skin_editor.name_hint"));
        nameBox.setResponder(v -> nameValue = v);
        slotButton = cycle(
                Component.translatable("gui.pwpwarfare.skin_editor.slot_type"),
                labels(SLOT_TYPES, "pwp_cosmetics.slot."),
                Math.max(0, Arrays.asList(SLOT_TYPES).indexOf(slotType)),
                i -> slotType = SLOT_TYPES[i]);
        cycle(
                Component.translatable("gui.pwpwarfare.skin_editor.rarity"),
                labels(RARITIES, "pwp_cosmetics.rarity."),
                Math.max(0, Arrays.asList(RARITIES).indexOf(rarity)),
                i -> rarity = RARITIES[i]);
        info(Component.translatable("gui.pwpwarfare.skin_editor.weapon_tag"), () -> Component.literal(weaponTag));

        section(Component.translatable("gui.pwpwarfare.skin_editor.model_path"));
        custom(SLOT + 2 * Spacing.XS, this::renderPreview);
        custom(INV_ROWS * (SLOT + 2) + Spacing.XS, this::renderGrid);

        action(
                Component.translatable("gui.pwpwarfare.skin_editor.cancel"),
                UIButton.Style.GHOST,
                UIIcon.BACK,
                this::close);
        UIButton del = action(
                Component.translatable("gui.pwpwarfare.admin.delete"),
                UIButton.Style.DANGER,
                UIIcon.CLOSE,
                this::delete);
        del.setEnabled(editSkinId != null);
        action(
                Component.translatable("gui.pwpwarfare.skin_editor.save"),
                UIButton.Style.ACCENT,
                UIIcon.CHECK,
                this::save);
    }

    private static List<Component> labels(String[] ids, String prefix) {
        List<Component> out = new ArrayList<>();
        for (String id : ids) out.add(Component.translatable(prefix + id));
        return out;
    }

    private void renderPreview(GuiGraphics g, Rect r) {
        Rect slot = Rect.of(r.x(), r.y() + Spacing.XS, SLOT, SLOT);
        int ri = Math.max(0, Arrays.asList(RARITIES).indexOf(rarity));
        WDraw.well(g, slot, 1F);
        UIDraw.rect(g, slot.inset(1).bottomStrip(Spacing.ACCENT_BAR), RARITY_COLORS[ri]);
        Rect text = r.rightOf(SLOT, Spacing.SM);
        if (!previewItem.isEmpty()) {
            g.renderItem(previewItem, slot.x() + 1, slot.y() + 1);
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, Component.literal(previewItemName), text.w()),
                    text.x(),
                    text.y() + (text.h() - font.lineHeight) / 2 + 1,
                    PWPTheme.Colors.INFO,
                    false);
        } else {
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, Component.translatable("gui.pwpwarfare.skin_editor.pick_item"), text.w()),
                    text.x(),
                    text.y() + (text.h() - font.lineHeight) / 2 + 1,
                    PWPTheme.Text.CAPTION,
                    false);
        }
    }

    private void renderGrid(GuiGraphics g, Rect r) {
        int gw = INV_COLS * (SLOT + 2) - 2;
        gridR = Rect.of(r.x() + Math.max(0, (r.w() - gw) / 2), r.y(), Math.min(gw, r.w()), INV_ROWS * (SLOT + 2) - 2);
        List<ItemStack> inv = items.get();
        for (int i = 0; i < INV_COLS * INV_ROWS; i++) {
            Rect s = slotRect(i);
            boolean sel = i == previewSlotIndex;
            WDraw.well(g, s, 1F);
            if (sel) {
                UIDraw.rect(g, s.inset(1), Styles.Control.BG_SELECTED);
                UIDraw.outline(g, s, Colors.ACCENT);
            }
            ItemStack st = i < inv.size() ? inv.get(i) : ItemStack.EMPTY;
            if (!st.isEmpty()) g.renderItem(st, s.x() + 1, s.y() + 1);
        }
    }

    private Rect slotRect(int i) {
        return Rect.of(gridR.x() + (i % INV_COLS) * (SLOT + 2), gridR.y() + (i / INV_COLS) * (SLOT + 2), SLOT, SLOT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && gridR.contains(mx, my)) {
            List<ItemStack> inv = items.get();
            for (int i = 0; i < INV_COLS * INV_ROWS; i++) {
                if (!slotRect(i).contains(mx, my)) continue;
                ItemStack st = i < inv.size() ? inv.get(i) : ItemStack.EMPTY;
                if (!st.isEmpty()) {
                    setPreviewItem(st);
                    previewSlotIndex = i;
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void setPreviewItem(ItemStack stack) {
        previewItem = stack.copy();
        previewItem.setCount(1);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        previewItemName = key != null ? key.toString() : "unknown";
        String id = key != null ? key.toString() : "";
        // скин ствола TaCZ (этап 11): в руке ствол с нужным видом (GunDisplayId) — сохраняем «для GunId — этот
        // display»; характеристики ствола не меняются, на кит привязывать не нужно
        CompoundTag held = stack.getTag();
        if (held != null && held.contains("GunId", 8)) {
            slotType = com.pwp.api.cosmetics.WeaponSkin.SLOT;
            weaponTag = held.getString("GunId");
            String display = held.getString(com.pwp.api.cosmetics.WeaponSkin.DISPLAY_TAG);
            previewRegistryName = display.isEmpty() ? "" : com.pwp.api.cosmetics.WeaponSkin.modelPath(display);
            previewItemName = display.isEmpty() ? weaponTag : display;
            if (slotButton != null) slotButton.label(Component.translatable("pwp_cosmetics.slot." + slotType));
            return;
        }
        // скин-предмет: id предмета (SNBT не проходит проверку пакета — раньше сохранение молча падало)
        previewRegistryName = id;
        if (id.contains("knife") || id.contains("bayonet") || id.contains("dagger") || id.contains("sword")) {
            slotType = "KNIFE";
            weaponTag = "knife";
        } else if (id.contains("pistol") || id.contains("deagle") || id.contains("glock") || id.contains("revolver")) {
            slotType = "SECONDARY";
            weaponTag = id.contains("deagle") ? "deagle" : "pistol";
        } else {
            slotType = "PRIMARY";
            if (id.contains("ak")) weaponTag = "ak47";
            else if (id.contains("m4")) weaponTag = "m4";
            else if (id.contains("sniper") || id.contains("l96") || id.contains("svd")) weaponTag = "sniper";
            else if (id.contains("lmg") || id.contains("pk") || id.contains("rpk")) weaponTag = "lmg";
            else if (id.contains("shotgun") || id.contains("spas")) weaponTag = "shotgun";
            else if (id.contains("smg") || id.contains("mp5") || id.contains("uzi")) weaponTag = "smg";
            else if (id.contains("rifle") || id.contains("carbine")) weaponTag = "rifle";
            else weaponTag = "any";
        }
        if (slotButton != null) slotButton.label(Component.translatable("pwp_cosmetics.slot." + slotType));
    }

    private void save() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            status(Component.translatable("gui.pwpwarfare.skin_editor.need_name"), true);
            return;
        }
        if (previewItem.isEmpty()) {
            status(Component.translatable("gui.pwpwarfare.skin_editor.need_item"), true);
            return;
        }
        if (previewRegistryName.isEmpty()) {
            // ствол без своего вида: сначала /pwpskin try <display> с этим стволом в руке
            status(Component.translatable("gui.pwpwarfare.skin_editor.need_display"), true);
            return;
        }
        String saveSkinId = editSkinId != null
                ? editSkinId
                : Math.abs(previewRegistryName.hashCode()) + "_"
                        + name.toLowerCase()
                                .replaceAll("[^a-z0-9_]", "_")
                                .replaceAll("_+", "_")
                                .replaceAll("^_|_$", "");
        if (preview) {
            status(Component.translatable("gui.pwpwarfare.admin.preview_skip"), false);
            return;
        }
        status(Component.translatable("gui.pwpwarfare.admin.saving"), false);
        AdminActions.saveSkin(
                new AdminAction.SaveSkin(saveSkinId, name, "", slotType, weaponTag, rarity, previewRegistryName), r -> {
                    if (r.success()) close();
                    else status(error(r.error()), true);
                });
    }

    private void delete() {
        if (editSkinId == null) {
            status(Component.translatable("gui.pwpwarfare.skin_editor.save_first"), true);
            return;
        }
        if (preview) {
            status(Component.translatable("gui.pwpwarfare.admin.preview_skip"), false);
            return;
        }
        status(Component.translatable("gui.pwpwarfare.admin.deleting"), false);
        AdminActions.deleteSkin(editSkinId, r -> {
            if (r.success()) close();
            else status(error(r.error()), true);
        });
    }

    private static Component error(String e) {
        return e == null || e.isEmpty()
                ? Component.translatable("gui.pwpwarfare.admin.api_error")
                : Component.literal(e);
    }

    private void close() {
        if (preview) return;
        Minecraft.getInstance().setScreen(new SkinListScreen());
    }
}
