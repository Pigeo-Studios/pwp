package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Снабжение ФОБа: пункты — {@link SupplyRadialScreen}; материалы и кулдауны установок — из хаба. */
public class HubRadialScreen extends SupplyRadialScreen {
    private final BlockPos hubPos;
    private HubBlockEntity hub;

    public HubRadialScreen(BlockPos pos) {
        super(Component.translatable("gui.pwpwarfare.radial.hub_supply"));
        this.hubPos = pos;
    }

    @Override
    protected boolean refresh() {
        var level = Minecraft.getInstance().level;
        // превью (/pwpui) без мира: пункты рисуются с нулями, меню не закрывается
        if (level == null) return true;
        hub = level.getBlockEntity(hubPos) instanceof HubBlockEntity h ? h : null;
        return true;
    }

    @Override
    protected int materials() {
        return hub == null ? 0 : hub.getMaterials();
    }

    @Override
    protected int mortarCooldownTicks() {
        return hub == null ? 0 : hub.cooldownMortar;
    }

    @Override
    protected int towCooldownTicks() {
        return hub == null ? 0 : hub.cooldownTOW;
    }

    @Override
    protected void send(int sector) {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestAmmo(hubPos, sector));
    }

    @Override
    protected Component centerInfo() {
        return Component.translatable("gui.pwpwarfare.radial.materials", materials());
    }
}
