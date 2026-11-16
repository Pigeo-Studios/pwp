package com.pigeostudios.pwp.warfare.client.gui.devtest;

import com.pigeostudios.pwp.warfare.client.gui.DefenseRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pigeostudios.pwp.warfare.client.gui.HubRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.PlayerKitSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.RadioRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.SquadMarkerRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.StaticGunRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.TacticalMapRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.WallRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import com.pwp.coreclient.gui.devtest.UiGallery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Фикстуры экранов pwp-warfare для тест-стенда UI ({@code /pwpui}, галерея core-client).
 * Только клиентские данные: экраны открываются в preview-режиме и НИЧЕГО не отправляют на
 * сервер. Переключатели галереи ({@link UiGallery#longStrings()}, {@link UiGallery#emptyData()})
 * читаются внутри фабрик — при каждом открытии.
 *
 * <p>Регистрация — {@link #registerAll()} из клиентской инициализации, только если
 * {@link UiGallery#enabled()}.
 */
@OnlyIn(Dist.CLIENT)
public final class WarfareUiFixtures {
    private WarfareUiFixtures() {}

    static final String LONG_RU =
            "Штурмовик-гранатомётчик тяжёлой огневой поддержки с расширенным боекомплектом и биноклем";
    static final String LONG_UK =
            "Штурмовик-гранатометник важкої вогневої підтримки з розширеним боєкомплектом і біноклем";

    static String name(String normal, int i) {
        return UiGallery.longStrings() ? (i % 2 == 0 ? LONG_RU : LONG_UK) + " " + normal : normal;
    }

    public static void registerAll() {
        UiGallery.register("warfare.deploy", () -> deploy(1, false));
        UiGallery.register("warfare.deploy.spawn_selected", () -> deploy(1, true));
        UiGallery.register("warfare.deploy.teams", () -> deploy(0, false));
        UiGallery.register("warfare.deploy.rules", () -> deploy(2, false));
        UiGallery.register("warfare.kits.select", WarfareUiFixtures::kitSelect);
        UiGallery.register("warfare.radial.radio", () -> new RadioRadialScreen().asPreview());
        UiGallery.register("warfare.radial.defense", () -> new DefenseRadialScreen(null).asPreview());
        UiGallery.register("warfare.radial.walls", () -> new WallRadialScreen(null).asPreview());
        UiGallery.register("warfare.radial.static_guns", () -> new StaticGunRadialScreen(null).asPreview());
        UiGallery.register("warfare.radial.hub", () -> new HubRadialScreen(BlockPos.ZERO).asPreview());
        UiGallery.register("warfare.radial.markers", () -> new SquadMarkerRadialScreen(0, 0, null).asPreview());
        UiGallery.register("warfare.radial.tactical", () -> new TacticalMapRadialScreen(0, 0, null).asPreview());
    }

    // ------------------------------------------------------------ киты

    private static PlayerKitSelectScreen kitSelect() {
        List<PacketOpenPlayerKitMenu.KitDTO> list = new ArrayList<>();
        if (!UiGallery.emptyData()) {
            String[][] kits = {
                {"Rifleman", "DIRECT_COMBAT"}, {"Medic", "SUPPORT"}, {"LAT", "SPECIALIST"},
                {"Machine Gunner", "FIRE_SUPPORT"}, {"Marksman", "SPECIALIST"}, {"Engineer", "SUPPORT"},
                {"Squad Leader", "DIRECT_COMBAT"}, {"Crewman", "SUPPORT"}
            };
            for (int i = 0; i < kits.length; i++) {
                boolean avail = i % 3 != 2;
                list.add(new PacketOpenPlayerKitMenu.KitDTO(
                        name(kits[i][0], i),
                        kits[i][1],
                        "",
                        avail,
                        avail ? "" : name("Лимит роли в отряде исчерпан", i),
                        i == 0,
                        List.of(new ItemStack(Items.CROSSBOW))));
            }
        }
        return new PlayerKitSelectScreen(list).asPreview();
    }

    // ------------------------------------------------------------ деплой

    private static DeployScreen deploy(int tab, boolean spawnSelected) {
        fillDeployData();
        String kit = DeployData.kits.isEmpty() ? "" : DeployData.kits.get(0).name();
        String spawn = spawnSelected && !DeployData.spawns.isEmpty()
                ? DeployData.spawns.get(0).id()
                : null;
        return new DeployScreen(true).previewState(tab, kit, spawn);
    }

    private static DeployData.LoadoutSlot slot(String label, ItemStack... options) {
        List<DeployData.LoadoutOption> list = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            list.add(new DeployData.LoadoutOption(
                    label + i, name(options[i].getHoverName().getString(), i), options[i]));
        }
        return new DeployData.LoadoutSlot(label, list, 0);
    }

    private static DeployData.KitRecord kit(
            String name, String category, boolean avail, String reason, int count, int max) {
        return new DeployData.KitRecord(
                name,
                category,
                name("Основной стрелок отряда: держит сектор, прикрывает пулемётчика, несёт запасные магазины.", 1),
                avail,
                reason,
                count,
                max,
                List.of(
                        slot("PRIMARY", new ItemStack(Items.CROSSBOW), new ItemStack(Items.BOW)),
                        slot("SECONDARY", new ItemStack(Items.IRON_SWORD)),
                        slot("THROWABLE", new ItemStack(Items.SNOWBALL, 2), new ItemStack(Items.EGG, 3)),
                        slot("SPECIAL", new ItemStack(Items.FIRE_CHARGE)),
                        slot(
                                "BACKPACK",
                                new ItemStack(Items.BREAD, 4),
                                new ItemStack(Items.ARROW, 32),
                                new ItemStack(Items.SPYGLASS),
                                new ItemStack(Items.SHEARS))),
                List.of(new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE)));
    }

    private static void fillDeployData() {
        DeployData.kits.clear();
        DeployData.spawns.clear();
        DeployData.squads.clear();
        DeployData.mapName = UiGallery.longStrings() ? LONG_RU : "TAKMACHKA";
        DeployData.deployTimer = 20;
        if (UiGallery.emptyData()) return;
        String[][] kits = {
            {"Rifleman", "DIRECT_COMBAT"}, {"Squad Leader", "DIRECT_COMBAT"}, {"Medic", "SUPPORT"},
            {"Engineer", "SUPPORT"}, {"LAT", "SPECIALIST"}, {"HAT", "SPECIALIST"},
            {"Machine Gunner", "FIRE_SUPPORT"}, {"Grenadier", "FIRE_SUPPORT"}, {"Marksman", "SPECIALIST"},
            {"Crewman", "SUPPORT"}, {"Pilot", "SUPPORT"}, {"Drone Operator", "SPECIALIST"}
        };
        for (int i = 0; i < kits.length; i++) {
            boolean avail = i % 4 != 3;
            String reason = avail ? "" : name("Лимит роли в команде исчерпан (3/3)", i);
            DeployData.kits.add(kit(kits[i][0], kits[i][1], avail, reason, i % 4, i % 3 == 0 ? 0 : 3));
        }
        DeployData.spawns.add(new DeployData.SpawnPoint(
                "MAIN", name("Основа", 0), new BlockPos(0, 64, 0), DeployData.SpawnStatus.SAFE, 0, 412));
        DeployData.spawns.add(new DeployData.SpawnPoint(
                "RALLY", name("Ралли", 1), new BlockPos(120, 64, -40), DeployData.SpawnStatus.BLOCKED, 0, 96));
        DeployData.spawns.add(new DeployData.SpawnPoint(
                "HUB:300:64:300",
                name("FOB 1", 2),
                new BlockPos(300, 64, 300),
                DeployData.SpawnStatus.SAFE,
                1800,
                230));
        DeployData.spawns.add(new DeployData.SpawnPoint(
                "HUB:-200:64:500",
                name("FOB 2", 3),
                new BlockPos(-200, 64, 500),
                DeployData.SpawnStatus.COOLDOWN,
                40,
                1250));
        DeployData.spawns.add(new DeployData.SpawnPoint(
                "HUB:900:64:-700",
                name("FOB 3", 4),
                new BlockPos(900, 64, -700),
                DeployData.SpawnStatus.DESTROYED,
                0,
                2100));
    }
}
