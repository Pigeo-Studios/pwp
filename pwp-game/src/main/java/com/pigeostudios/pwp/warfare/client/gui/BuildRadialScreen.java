package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.build.Structure;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.ClientPlacementHandler;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareRadialScreen;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Стройка лидера отряда (этап 17c, как в Squad): рация → раздел (оборона, заграждения, установки, логистика)
 * → постройка. Пункты — из каталога {@link Structure} (одна таблица с сервером): цена, лимит у ФОБа, красная
 * подсветка, если у ближайшего своего ФОБа не хватает материалов (клик всё равно уходит — решает сервер).
 * Заменяет три прежние радиалки (оборона → стены, установки) с номерами построек прямо в коде.
 */
@OnlyIn(Dist.CLIENT)
public class BuildRadialScreen extends WarfareRadialScreen {
    private final Screen parent;
    private final Structure.Category category;

    public BuildRadialScreen(Screen parent, Structure.Category category) {
        super(Component.translatable("gui.pwpwarfare.radial.cat." + category.id()));
        this.parent = parent;
        this.category = category;
    }

    public static WarfareIcon icon(Structure.Category c) {
        return switch (c) {
            case DEFENSE -> WarfareIcon.WALL;
            case OBSTACLE -> WarfareIcon.WIRE;
            case EMPLACEMENT -> WarfareIcon.GUN;
            case LOGISTICS -> WarfareIcon.VEHICLE;
        };
    }

    private static WarfareIcon icon(Structure s) {
        return switch (s) {
            case BUNKER, EMBRASURE -> WarfareIcon.SHIELD;
            case MORTAR, TOW -> WarfareIcon.TARGET;
            case REB, REB_MINI -> WarfareIcon.RADIO;
            default -> icon(s.category);
        };
    }

    /** Цена для показа: из каталога или (РЭБ) из конфига. */
    static int cost(Structure s) {
        if (s.cost != Structure.CONFIG_COST) return s.cost;
        return s == Structure.REB_MINI ? WarfareConfig.REB_MINI_BUILD_COST.get() : WarfareConfig.REB_BUILD_COST.get();
    }

    /** Материалы ближайшего своего ФОБа в радиусе стройки; -1 — ФОБа рядом нет (материалы не известны). */
    public static int nearFobMaterials() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.getTeam() == null) return -1;
        String team = p.getTeam().getName();
        double r2 = Math.pow(WarfareConfig.HUB_BUILD_RADIUS.get(), 2);
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (WarfareWorldData.HubInfo h : ClientData.clientHubs) {
            if (h.pos == null || !h.team.equalsIgnoreCase(team)) continue;
            double d = h.pos.distToCenterSqr(p.position());
            if (d <= r2 && d < bestD) {
                bestD = d;
                best = h.materials;
            }
        }
        return best;
    }

    @Override
    protected List<Entry> entries() {
        int materials = nearFobMaterials();
        boolean creative = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isCreative();
        List<Entry> out = new ArrayList<>();
        for (Structure s : Structure.of(category)) {
            int c = cost(s);
            Component detail = s.perFob > 0
                    ? Component.translatable("gui.pwpwarfare.radial.cost_per_fob", c, s.perFob)
                    : Component.translatable("gui.pwpwarfare.radial.cost", c);
            out.add(Entry.of(Component.translatable("gui.pwpwarfare.radial.item." + s.id()), icon(s), () -> {
                        ClientPlacementHandler.startPlacing(s.wireId);
                        onClose();
                    })
                    .detail(detail)
                    .warn(!creative && materials >= 0 && materials < c));
        }
        return out;
    }

    @Override
    protected Component centerInfo() {
        int m = nearFobMaterials();
        return m < 0
                ? Component.translatable("gui.pwpwarfare.radial.no_fob")
                : Component.translatable("gui.pwpwarfare.radial.materials", m);
    }

    @Override
    protected boolean radioAnimations() {
        return true;
    }

    @Override
    protected boolean onBack() {
        Minecraft.getInstance().setScreen(parent);
        return true;
    }
}
