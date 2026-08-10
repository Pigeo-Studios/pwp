package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import net.minecraft.core.BlockPos;
import com.pigeostudios.pwp.warfare.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/** Data holder for DeployScreen panels. Populated from ClientData. */
public class DeployData {

    public enum SpawnStatus { SAFE, COOLDOWN, BLOCKED, DESTROYED, HEALTHY }

    public record SpawnPoint(String id, String name, BlockPos pos, SpawnStatus status, int supplies, int distance) {}

    public record LoadoutOption(String id, String name, ItemStack stack) {}

    public record LoadoutSlot(String label, List<LoadoutOption> options, int defaultIndex) {
        public LoadoutOption getSelected() { return options.get(defaultIndex); }
        public boolean hasAlternatives() { return options.size() > 1; }
    }

    public record KitRecord(
        String name, String category, String description,
        boolean available, String reason,
        int inTeamCount, int maxInTeam,
        int inSquadCount, int maxInSquad,
        List<LoadoutSlot> loadout,
        List<ItemStack> armor
    ) {}

    public record SquadRecord(int id, String name, String leader, List<String> members, boolean isLocked,
        String bravoLeader, String charlieLeader, List<String> bravoMembers, List<String> charlieMembers) {}

    /** Try to create a TACZ gun item; fall back to vanilla. */
    private static ItemStack gun(String taczId, ItemStack fallback) {
        return TaczHelper.getGunItem(taczId, fallback);
    }

    // ══════════ State ══════════
    public static List<SpawnPoint> spawns = new ArrayList<>();
    public static List<KitRecord> kits = new ArrayList<>();
    public static List<SquadRecord> squads = new ArrayList<>();
    public static int blueTickets = 800;
    public static int redTickets = 800;
    public static String blueFaction = "none";
    public static String redFaction = "none";
    public static String mapName = "TAKMACHKA";
    public static String playerName = "";
    public static String selectedSpawn = "";
    public static int deployTimer = 18;
    public static Map<String, Set<String>> expandedSlots = new HashMap<>();
    public static Map<String, Map<String, Integer>> slotSelections = new HashMap<>();
    public static Map<String, Long> animStart = new HashMap<>();
    public static Map<String, Boolean> animDir = new HashMap<>(); // true=expanding

    // Имена китов для dev-фолбэка (populateHardcoded), когда сервер не прислал DTO
    private static final String[] HARDCODED_KIT_NAMES = {
        "Officer", "Pilot Officer", "Mechanic Officer",
        "Rifleman", "Medic", "LMG", "Assault", "Grenadier", "Marksman",
        "Sniper", "HMG", "LAT", "HAT", "Sapper", "Scout",
        "Pilot", "Mechanic", "Drone Operator", "Anti_air"
    };

    // Группировка сетки ролей (RoleGrid): роль -> группа. «Офицерские» первыми вверху.
    // Группы берутся на клиенте (как раньше в KIT_META), категории из БД на сетку не влияют.
    private static final Map<String, String> KIT_GROUP = new HashMap<>();
    static {
        KIT_GROUP.put("Officer", "COMMANDER");
        KIT_GROUP.put("Pilot Officer", "COMMANDER");
        KIT_GROUP.put("Mechanic Officer", "COMMANDER");
        KIT_GROUP.put("Rifleman", "INFANTRY");
        KIT_GROUP.put("Medic", "INFANTRY");
        KIT_GROUP.put("LMG", "INFANTRY");
        KIT_GROUP.put("Assault", "INFANTRY");
        KIT_GROUP.put("Grenadier", "SPECIALIST");
        KIT_GROUP.put("Marksman", "SPECIALIST");
        KIT_GROUP.put("Sniper", "SPECIALIST");
        KIT_GROUP.put("HMG", "SPECIALIST");
        KIT_GROUP.put("LAT", "SPECIALIST");
        KIT_GROUP.put("HAT", "SPECIALIST");
        KIT_GROUP.put("Sapper", "SPECIALIST");
        KIT_GROUP.put("Scout", "SPECIALIST");
        KIT_GROUP.put("Pilot", "CREWMAN");
        KIT_GROUP.put("Mechanic", "CREWMAN");
        KIT_GROUP.put("Drone Operator", "CREWMAN");
        KIT_GROUP.put("Anti_air", "CREWMAN");
    }

    public static final Map<String, String> KIT_DISPLAY_NAMES = new LinkedHashMap<>();
    static {
        KIT_DISPLAY_NAMES.put("Officer", "Офицер");
        KIT_DISPLAY_NAMES.put("Pilot Officer", "Пилот-Офицер");
        KIT_DISPLAY_NAMES.put("Mechanic Officer", "Офицер-Механик");
        KIT_DISPLAY_NAMES.put("Scout", "Скаут");
        KIT_DISPLAY_NAMES.put("LAT", "LAT");
        KIT_DISPLAY_NAMES.put("HAT", "HAT");
        KIT_DISPLAY_NAMES.put("Sapper", "Сапёр");
        KIT_DISPLAY_NAMES.put("Sniper", "Снайпер");
        KIT_DISPLAY_NAMES.put("Marksman", "Марксман");
        KIT_DISPLAY_NAMES.put("LMG", "LMG");
        KIT_DISPLAY_NAMES.put("HMG", "HMG");
        KIT_DISPLAY_NAMES.put("Rifleman", "Рифлмен");
        KIT_DISPLAY_NAMES.put("Medic", "Медик");
        KIT_DISPLAY_NAMES.put("Grenadier", "Гранатомётчик");
        KIT_DISPLAY_NAMES.put("Assault", "Штурмовик");
        KIT_DISPLAY_NAMES.put("Pilot", "Пилот");
        KIT_DISPLAY_NAMES.put("Mechanic", "Механик");
        KIT_DISPLAY_NAMES.put("Drone Operator", "Дроновод");
        KIT_DISPLAY_NAMES.put("Anti_air", "анти воздух");
        KIT_DISPLAY_NAMES.put("COMMANDER", "КОМАНДИР");
        KIT_DISPLAY_NAMES.put("INFANTRY", "ПЕХОТА");
        KIT_DISPLAY_NAMES.put("SPECIALIST", "СПЕЦИАЛИСТ");
        KIT_DISPLAY_NAMES.put("CREWMAN", "ЭКИПАЖ");
        // Страховка для нестандартных китов с категориями из БД
        KIT_DISPLAY_NAMES.put("DIRECT_COMBAT", "ПЕХОТА");
        KIT_DISPLAY_NAMES.put("FIRE_SUPPORT", "ОГНЕВАЯ ПОДДЕРЖКА");
        KIT_DISPLAY_NAMES.put("SUPPORT", "ПОДДЕРЖКА");
    }

    public static String getDisplayName(String en) {
        return KIT_DISPLAY_NAMES.getOrDefault(en, en);
    }

    /** Имя файла иконки кита: нижний регистр, только [a-z0-9/._-] (скобки/пробелы → `_`). */
    public static String kitIconFileName(String kitName) {
        String s = kitName.toLowerCase().replaceAll("[^a-z0-9._/-]", "_");
        return s.replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    public static void populate() {
        populate(List.of());
    }

    public static void populate(List<PacketOpenPlayerKitMenu.KitDTO> serverKits) {
        spawns.clear();
        kits.clear();
        squads.clear();

        if (serverKits != null && !serverKits.isEmpty()) {
            for (PacketOpenPlayerKitMenu.KitDTO dto : serverKits) {
                String cat = KIT_GROUP.getOrDefault(dto.name,
                    dto.category != null && !dto.category.isEmpty() ? dto.category : "INFANTRY");
                // Серверные категории (в которые не попала KIT_GROUP) тоже переводим
                cat = getDisplayName(cat);
                String desc = dto.description != null ? dto.description : "";
                List<LoadoutSlot> loadout = buildLoadoutFromItems(dto.items, dto.slotSkins);
                List<ItemStack> armor = extractArmorFromItems(dto.items);
                kits.add(new KitRecord(dto.name, cat, desc, dto.available,
                    dto.available ? "" : dto.reason,
                    dto.inTeamCount, dto.maxInTeam,
                    dto.inSquadCount, dto.maxInSquad,
                    loadout, armor));
            }
        } else {
            populateHardcoded();
        }
    }

    private static void populateHardcoded() {
        LoadoutOption rifleM4  = new LoadoutOption("m4a1",  "M4A1",  gun("m4a1",    new ItemStack(Items.IRON_SWORD)));
        LoadoutOption rifleAk  = new LoadoutOption("ak74",  "AK-74",  gun("ak47",    new ItemStack(Items.DIAMOND_SWORD)));
        LoadoutOption rifleM16 = new LoadoutOption("m16a4", "M16A4",  gun("m16a4",   new ItemStack(Items.GOLDEN_SWORD)));
        LoadoutOption rifleAug = new LoadoutOption("aug",   "AUG A3", gun("aug",     new ItemStack(Items.GOLDEN_AXE)));
        LoadoutOption pistolM9 = new LoadoutOption("m9",    "M9",     gun("glock_17",new ItemStack(Items.IRON_SHOVEL)));
        LoadoutOption frag     = new LoadoutOption("frag",  "M67 Frag", new ItemStack(Items.GUNPOWDER));
        LoadoutOption smoke    = new LoadoutOption("smoke", "M18 Smoke",new ItemStack(Items.GRAY_DYE));
        LoadoutOption smokeRed = new LoadoutOption("smoke_red","Red Smoke",new ItemStack(Items.RED_DYE));
        LoadoutOption bag      = new LoadoutOption("ammo_bag","Ammo Bag",new ItemStack(Items.BUNDLE));
        LoadoutOption bandage  = new LoadoutOption("bandage","Bandage", new ItemStack(Items.PAPER));
        LoadoutOption bino     = new LoadoutOption("bino",  "Binoculars",new ItemStack(ModItems.BINOCULARS.get()));

        int i = 0;
        for (String name : HARDCODED_KIT_NAMES) {
            boolean avail = true;
            List<LoadoutSlot> loadout = List.of(
                new LoadoutSlot("PRIMARY", List.of(rifleM4, rifleAk, rifleM16, i % 2 == 0 ? rifleAug : rifleM4), 0),
                new LoadoutSlot("SECONDARY", List.of(pistolM9), 0),
                new LoadoutSlot("SPECIAL", List.of(bandage), 0),
                new LoadoutSlot("BACKPACK", List.of(frag, smoke, i % 3 == 0 ? smokeRed : smoke, bag, bino), 0)
            );
            kits.add(new KitRecord(name, "INFANTRY", "", avail, "", 0, -1, 0, -1, loadout, List.of()));
            i++;
        }
    }

    private static List<LoadoutSlot> buildLoadoutFromItems(List<ItemStack> items, Map<Integer, List<String>> slotSkins) {
        List<LoadoutSlot> slots = new ArrayList<>();
        var primary = new ArrayList<LoadoutOption>();
        var secondary = new ArrayList<LoadoutOption>();
        var special = new ArrayList<LoadoutOption>();
        var backpack = new ArrayList<LoadoutOption>();

        for (int i = 0; i < Math.min(9, items.size()); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getDescriptionId();
            String name = stack.getHoverName().getString();
            LoadoutOption opt = new LoadoutOption(id, name.isEmpty() ? stack.getItem().toString() : name, stack);
            if (i == 0) primary.add(opt);
            else if (i == 1) secondary.add(opt);
            else if (i == 2) backpack.add(opt);
            else if (i == 3) special.add(opt);
            else {
                String cat = getAltCategory(i, slotSkins);
                if (cat != null) {
                    switch (cat) {
                        case "PRIMARY" -> primary.add(opt);
                        case "SECONDARY" -> secondary.add(opt);
                        case "THROWABLE" -> backpack.add(opt);
                        case "SPECIAL" -> special.add(opt);
                        default -> backpack.add(opt);
                    }
                } else {
                    // Слоты 4-8 без __ALT__-метки: всегда в рюкзак.
                    // Метки PRIMARY/SECONDARY/SPECIAL задаются только слотами 0-3
                    // или __ALT__-метками на слотах 41+ — эвристика isWeapon() тут не нужна
                    // (иначе ракеты/трубы в рюкзаке получали метку «СТВОЛ»)
                    backpack.add(opt);
                }
            }
        }

        // Process extra slots 41-48
        for (int i = 41; i < Math.min(49, items.size()); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getDescriptionId();
            String name = stack.getHoverName().getString();
            LoadoutOption opt = new LoadoutOption(id, name.isEmpty() ? stack.getItem().toString() : name, stack);
            String cat = getAltCategory(i, slotSkins);
            if (cat != null) {
                switch (cat) {
                    case "PRIMARY" -> primary.add(opt);
                    case "SECONDARY" -> secondary.add(opt);
                    case "THROWABLE" -> backpack.add(opt);
                    case "SPECIAL" -> special.add(opt);
                    default -> backpack.add(opt);
                }
            } else {
                backpack.add(opt);
            }
        }

        // Слоты добавляются ВСЕГДА (пустая вторичка/спец = «—»), чтобы парная карточка
        // и ховеры не разваливались на пустых опциях.
        LoadoutOption empty = new LoadoutOption("empty", "—", ItemStack.EMPTY);
        slots.add(new LoadoutSlot("PRIMARY", primary.isEmpty() ? List.of(empty) : primary, 0));
        slots.add(new LoadoutSlot("SECONDARY", secondary.isEmpty() ? List.of(empty) : secondary, 0));
        slots.add(new LoadoutSlot("SPECIAL", special.isEmpty() ? List.of(empty) : special, 0));
        if (!backpack.isEmpty()) slots.add(new LoadoutSlot("BACKPACK", backpack, 0));
        return slots;
    }

    private static String getAltCategory(int slot, Map<Integer, List<String>> slotSkins) {
        if (slotSkins == null) return null;
        List<String> list = slotSkins.get(slot);
        if (list == null) return null;
        String prefix = "__ALT__";
        for (String s : list) {
            if (s.startsWith(prefix)) return s.substring(prefix.length());
        }
        return null;
    }

    private static List<ItemStack> extractArmorFromItems(List<ItemStack> items) {
        List<ItemStack> armor = new ArrayList<>(4);
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            var item = stack.getItem();
            if (item instanceof net.minecraft.world.item.ArmorItem) armor.add(stack);
            if (armor.size() >= 4) break;
        }
        return armor;
    }

    public static int getSelectedIndex(String kitName, String slotLabel) {
        return slotSelections.getOrDefault(kitName, Map.of()).getOrDefault(slotLabel, 0);
    }

    public static void setSelectedIndex(String kitName, String slotLabel, int idx) {
        slotSelections.computeIfAbsent(kitName, k -> new HashMap<>()).put(slotLabel, idx);
    }

    public static Map<String, Integer> getSlotSelections(String kitName) {
        return slotSelections.getOrDefault(kitName, Map.of());
    }

    public static boolean isSlotExpanded(String kitName, String slotLabel) {
        return expandedSlots.getOrDefault(kitName, Set.of()).contains(slotLabel);
    }

    public static void setSlotExpanded(String kitName, String slotLabel, boolean expanded) {
        expandedSlots.computeIfAbsent(kitName, k -> new HashSet<>());
        var set = expandedSlots.get(kitName);
        if (expanded) set.add(slotLabel);
        else set.remove(slotLabel);
    }

    public static void toggleSlotExpanded(String kitName, String slotLabel) {
        expandedSlots.computeIfAbsent(kitName, k -> new HashSet<>());
        var set = expandedSlots.get(kitName);
        if (set.contains(slotLabel)) set.remove(slotLabel);
        else set.add(slotLabel);
    }
}
