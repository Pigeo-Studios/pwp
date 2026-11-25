package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Радиалка рации: точка сбора (с кулдауном отряда), оборона, установки. */
public class RadioRadialScreen extends WarfareRadialScreen {
    public RadioRadialScreen() {
        super(Component.translatable("gui.pwpwarfare.radial.radio"));
    }

    /** Секунд до следующего ралли отряда (0 — доступно), как раньше. */
    private static long rallyCooldownSeconds() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;
        String pName = player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(pName)) {
                long gameTime = player.level().getGameTime();
                if (gameTime < s.nextRallyAvailableTick && !player.isCreative()) {
                    return (s.nextRallyAvailableTick - gameTime) / 20L;
                }
                return 0;
            }
        }
        return 0;
    }

    @Override
    protected List<Entry> entries() {
        long cd = rallyCooldownSeconds();
        // клик по ралли уходит на сервер и при кулдауне (сервер сам откажет) — как раньше
        Entry rally = Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.rally"), WarfareIcon.SPAWN_RALLY, () -> {
                            PacketHandler.INSTANCE.sendToServer(new PacketRadioAction(0));
                            onClose();
                        })
                .warn(cd > 0);
        if (cd > 0) rally = rally.detail(Component.translatable("gui.pwpwarfare.radial.cooldown", cd));
        return List.of(
                rally,
                Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.defense"),
                        WarfareIcon.SHIELD,
                        () -> switchTo(new DefenseRadialScreen(this))),
                Entry.of(
                        Component.translatable("gui.pwpwarfare.radial.item.emplacements"),
                        WarfareIcon.GUN,
                        () -> switchTo(new StaticGunRadialScreen(this))));
    }

    @Override
    protected boolean radioAnimations() {
        return true;
    }
}
