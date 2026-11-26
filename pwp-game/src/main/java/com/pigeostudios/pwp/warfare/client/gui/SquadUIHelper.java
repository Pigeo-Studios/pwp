package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class SquadUIHelper {

    private SquadUIHelper() {}

    public static boolean isApplyCmdVisible() {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        boolean isBlue = getPlayerTeam().toUpperCase().contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        boolean voteActive = isBlue ? ClientData.blueCmdVoteActive : ClientData.redCmdVoteActive;
        if (teamCMDId != -1 || voteActive) return false;
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.leader.equals(myName)) return true;
        }
        return false;
    }

    public static boolean isPlayerInSquad() {
        String myName = Minecraft.getInstance().player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) return true;
        }
        return false;
    }

    public static boolean isSquadLeaderOrFTL(Player player) {
        String pName = player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.leader.equals(pName) || s.bravoLeader.equals(pName) || s.charlieLeader.equals(pName)) return true;
        }
        return false;
    }

    public static String getPlayerTeam() {
        Player p = Minecraft.getInstance().player;
        return p != null && p.getTeam() != null ? p.getTeam().getName() : "NEUTRAL";
    }

    public static List<WarfareWorldData.Squad> getSortedSquads() {
        String myTeam = getPlayerTeam().toUpperCase();
        String myDim = Minecraft.getInstance().level.dimension().location().toString();
        boolean isBlue = myTeam.contains("BLUE");
        int teamCMDId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        return ClientData.clientSquads.stream()
                .filter(s -> s.team.equalsIgnoreCase(myTeam))
                .filter(s -> s.dimension != null && s.dimension.equals(myDim))
                .sorted((a, b) -> {
                    if (a.id == teamCMDId) return -1;
                    if (b.id == teamCMDId) return 1;
                    return Integer.compare(a.id, b.id);
                })
                .collect(Collectors.toList());
    }

    public static List<String> getSortedMembers(WarfareWorldData.Squad squad) {
        List<String> sorted = new ArrayList<>();
        if (!squad.leader.isEmpty() && squad.members.contains(squad.leader)) sorted.add(squad.leader);
        for (String m : squad.members) {
            if (!m.equals(squad.leader) && !squad.bravoMembers.contains(m) && !squad.charlieMembers.contains(m))
                sorted.add(m);
        }
        if (!squad.bravoLeader.isEmpty() && squad.members.contains(squad.bravoLeader)) sorted.add(squad.bravoLeader);
        for (String m : squad.bravoMembers) {
            if (!m.equals(squad.bravoLeader) && squad.members.contains(m)) sorted.add(m);
        }
        if (!squad.charlieLeader.isEmpty() && squad.members.contains(squad.charlieLeader))
            sorted.add(squad.charlieLeader);
        for (String m : squad.charlieMembers) {
            if (!m.equals(squad.charlieLeader) && squad.members.contains(m)) sorted.add(m);
        }
        return sorted;
    }
}
