package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.network.PacketOpenPlayerKitMenu;
import net.minecraft.core.BlockPos;
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

    private static final Map<String, String[]> KIT_META = new LinkedHashMap<>();
    static {
        KIT_META.put("Officer", new String[]{"COMMANDER", "Squad leader. Leads infantry elements."});
        KIT_META.put("Pilot Officer", new String[]{"COMMANDER", "Helicopter pilot squad leader."});
        KIT_META.put("Mechanic Officer", new String[]{"COMMANDER", "Vehicle squad leader."});
        KIT_META.put("Rifleman", new String[]{"INFANTRY", "Standard infantry."});
        KIT_META.put("Medic", new String[]{"INFANTRY", "Combat medic."});
        KIT_META.put("LMG", new String[]{"INFANTRY", "Light machine gunner."});
        KIT_META.put("Assault", new String[]{"INFANTRY", "Close quarters assault."});
        KIT_META.put("Grenadier", new String[]{"SPECIALIST", "Grenade launcher."});
        KIT_META.put("Marksman", new String[]{"SPECIALIST", "Designated marksman."});
        KIT_META.put("Sniper", new String[]{"SPECIALIST", "Sniper."});
        KIT_META.put("HMG", new String[]{"SPECIALIST", "Heavy machine gunner."});
        KIT_META.put("LAT", new String[]{"SPECIALIST", "Light anti-tank."});
        KIT_META.put("HAT", new String[]{"SPECIALIST", "Heavy anti-tank."});
        KIT_META.put("Sapper", new String[]{"SPECIALIST", "Combat engineer."});
        KIT_META.put("Scout", new String[]{"SPECIALIST", "Reconnaissance."});
        KIT_META.put("Pilot", new String[]{"CREWMAN", "Helicopter pilot."});
        KIT_META.put("Mechanic", new String[]{"CREWMAN", "Vehicle crewman."});
        KIT_META.put("Drone Operator", new String[]{"CREWMAN", "Drone operator."});
        KIT_META.put("Anti_air", new String[]{"CREWMAN", "Anti-air specialist."});
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
    }

    public static String getDisplayName(String en) {
        return KIT_DISPLAY_NAMES.getOrDefault(en, en);
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
                String[] meta = KIT_META.get(dto.name);
                if (meta == null) {
                    String dtoNorm = dto.name.toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
                    for (var entry : KIT_META.entrySet()) {
                        String keyNorm = entry.getKey().toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
                        if (keyNorm.equals(dtoNorm) || dtoNorm.contains(keyNorm) || keyNorm.contains(dtoNorm)) {
                            meta = entry.getValue();
                            break;
                        }
                    }
                }
                String cat = meta != null ? meta[0]
                    : (dto.category != null && !dto.category.isEmpty() ? dto.category : "INFANTRY");
                String desc = (dto.description != null && !dto.description.isEmpty()) ? dto.description
                    : (meta != null ? meta[1] : "");
                List<LoadoutSlot> loadout = buildLoadoutFromItems(dto.items, dto.slotSkins);
                List<ItemStack> armor = extractArmorFromItems(dto.items);
                int inTeam = 0, maxTeam = -1;
                kits.add(new KitRecord(dto.name, cat, desc, dto.available,
                    dto.available ? "" : dto.reason, inTeam, maxTeam, loadout, armor));
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
        LoadoutOption bino     = new LoadoutOption("bino",  "Binoculars",new ItemStack(Items.SPYGLASS));

        int i = 0;
        for (var e : KIT_META.entrySet()) {
            String name = e.getKey();
            String[] meta = e.getValue();
            boolean avail = true;
            List<LoadoutSlot> loadout = List.of(
                new LoadoutSlot("PRIMARY", List.of(rifleM4, rifleAk, rifleM16, i % 2 == 0 ? rifleAug : rifleM4), 0),
                new LoadoutSlot("SECONDARY", List.of(pistolM9), 0),
                new LoadoutSlot("THROWABLE", List.of(frag, smoke, i % 3 == 0 ? smokeRed : smoke), 0),
                new LoadoutSlot("SPECIAL", List.of(bandage), 0),
                new LoadoutSlot("BACKPACK", List.of(bag, bino), 0)
            );
            kits.add(new KitRecord(name, meta[0], meta[1], avail, "", 0, -1, loadout, List.of()));
            i++;
        }
    }

    private static List<LoadoutSlot> buildLoadoutFromItems(List<ItemStack> items, Map<Integer, List<String>> slotSkins) {
        List<LoadoutSlot> slots = new ArrayList<>();
        var primary = new ArrayList<LoadoutOption>();
        var secondary = new ArrayList<LoadoutOption>();
        var throwable = new ArrayList<LoadoutOption>();
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
            else if (i == 2) throwable.add(opt);
            else if (i == 3) special.add(opt);
            else {
                String cat = getAltCategory(i, slotSkins);
                if (cat != null) {
                    switch (cat) {
                        case "PRIMARY" -> primary.add(opt);
                        case "SECONDARY" -> secondary.add(opt);
                        case "THROWABLE" -> throwable.add(opt);
                        case "SPECIAL" -> special.add(opt);
                        default -> backpack.add(opt);
                    }
                } else if (isWeapon(stack)) {
                    primary.add(opt);
                } else {
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
                    case "THROWABLE" -> throwable.add(opt);
                    case "SPECIAL" -> special.add(opt);
                    default -> backpack.add(opt);
                }
            } else {
                backpack.add(opt);
            }
        }

        if (!primary.isEmpty()) slots.add(new LoadoutSlot("PRIMARY", primary, 0));
        if (!secondary.isEmpty()) slots.add(new LoadoutSlot("SECONDARY", secondary, 0));
        if (!throwable.isEmpty()) slots.add(new LoadoutSlot("THROWABLE", throwable, 0));
        if (!special.isEmpty()) slots.add(new LoadoutSlot("SPECIAL", special, 0));
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

    private static boolean isWeapon(ItemStack stack) {
        var item = stack.getItem();
        String id = item.getDescriptionId();
        return id.contains("gun") || id.contains("rifle") || id.contains("pistol") || id.contains("smg")
            || id.contains("shotgun") || id.contains("sniper") || id.contains("lmg") || id.contains("rocket")
            || id.contains("launcher") || id.contains("sword") || id.contains("axe")
            || item instanceof net.minecraft.world.item.SwordItem
            || item instanceof net.minecraft.world.item.AxeItem;
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
