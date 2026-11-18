package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.ui.CardGridScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pwp.coreclient.ui.UIIcon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Выбор кита игроком (сетка карточек): иконка кита, имя, отметка выбранного, недоступный —
 * с замком и причиной в тултипе; «глаз» — предпросмотр. Клик — {@link PacketSelectKit} и
 * закрытие (как раньше).
 */
public class PlayerKitSelectScreen extends CardGridScreen {
    private final List<PacketOpenPlayerKitMenu.KitDTO> kits;
    /** Тест-стенд: клик по карточке ничего не отправляет. */
    private boolean preview;

    public PlayerKitSelectScreen(List<PacketOpenPlayerKitMenu.KitDTO> kits) {
        super(Component.translatable("gui.pwpwarfare.kit_select.title"));
        this.kits = kits;
    }

    public PlayerKitSelectScreen asPreview() {
        this.preview = true;
        return this;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.PERSON;
    }

    @Override
    protected Component emptyTitle() {
        return Component.translatable("gui.pwpwarfare.deploy.roles.empty");
    }

    @Override
    protected Component emptyHint() {
        return Component.translatable("gui.pwpwarfare.deploy.roles.empty_hint");
    }

    @Override
    protected List<Card> cards() {
        List<Card> out = new ArrayList<>();
        for (PacketOpenPlayerKitMenu.KitDTO kit : kits) {
            UIIcon cat = UIIcon.forRoleCategory(kit.category);
            Card c = Card.of(kit.name, Component.literal(DeployData.getDisplayName(kit.name)), () -> {
                        if (preview) return;
                        PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit.name));
                        onClose();
                    })
                    .image(kitIcon(kit.name), 1F)
                    .icon(WarfareIcon.PERSON)
                    .selected(kit.isSelected)
                    .actions(new Action(
                            WarfareIcon.TARGET,
                            Component.translatable("gui.pwpwarfare.kit_select.preview"),
                            () -> Minecraft.getInstance().setScreen(new KitPreviewScreen(this, kit.name, kit.items))));
            if (cat != null) c = c.accent(cat.roleColor());
            if (!kit.available) c = c.disabled(com.pigeostudios.pwp.warfare.network.KitReasonText.of(kit.reason));
            out.add(c);
        }
        return out;
    }

    private static ResourceLocation kitIcon(String name) {
        ResourceLocation rl = ResourceLocation.tryBuild(
                "pwpwarfare", "textures/gui/kits/" + DeployData.kitIconFileName(name) + ".png");
        if (rl == null) return null;
        return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent() ? rl : null;
    }
}
