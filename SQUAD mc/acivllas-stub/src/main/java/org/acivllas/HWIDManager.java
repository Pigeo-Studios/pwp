package org.acivllas;

import java.util.*;

public class HWIDManager {
    private static final Set<String> BLACKLIST = new HashSet<>();
    private static final Map<String, String> PLAYER_HWIDS = new HashMap<>();

    public static void addHWID(String player, String hwid) {
        PLAYER_HWIDS.put(player, hwid);
        System.out.println("[AntiCheat] HWID: " + hwid + " for " + player);
    }

    public static boolean isBlacklisted(String hwid) {
        return BLACKLIST.contains(hwid);
    }

    public static String getHWID(String player) {
        return PLAYER_HWIDS.get(player);
    }

    public static void blacklistPlayer(String player, String hwid) {
        if (hwid != null && !hwid.isEmpty()) {
            BLACKLIST.add(hwid);
            try {
                java.nio.file.Files.write(
                    java.nio.file.Paths.get("hwid_blacklist.txt"),
                    java.util.Collections.singletonList(player + " | " + hwid),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                );
            } catch (java.io.IOException e) {
                System.err.println("[AntiCheat] Error writing HWID blacklist: " + e.getMessage());
            }
        }
    }
}
