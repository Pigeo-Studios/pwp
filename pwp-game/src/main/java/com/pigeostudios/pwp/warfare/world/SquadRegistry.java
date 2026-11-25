package com.pigeostudios.pwp.warfare.world;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Отряды и командование: список отрядов, командиры (CMD) команд и голосования за CMD. Часть {@link WarfareWorldData}. */
public final class SquadRegistry {
    public List<WarfareWorldData.Squad> squads = new ArrayList<>();
    public int blueCMDId = -1;
    public int redCMDId = -1;
    public boolean blueCmdVoteActive = false;
    public String blueCmdCandidateName = "";
    public int blueCmdCandidateId = -1;
    public int blueCmdVoteTimer = 0;
    public Map<UUID, Boolean> blueCmdVotes = new HashMap<>();
    public boolean redCmdVoteActive = false;
    public String redCmdCandidateName = "";
    public int redCmdCandidateId = -1;
    public int redCmdVoteTimer = 0;
    public Map<UUID, Boolean> redCmdVotes = new HashMap<>();

    /** Пишет свои ключи в корень тега WarfareWorldData (формат прежний). */
    public void save(CompoundTag tag) {
        tag.putInt("BlueCMDId", this.blueCMDId);
        tag.putInt("RedCMDId", this.redCMDId);
        tag.putBoolean("BlueCmdActive", this.blueCmdVoteActive);
        tag.putString("BlueCmdCandName", this.blueCmdCandidateName);
        tag.putInt("BlueCmdCandId", this.blueCmdCandidateId);
        tag.putInt("BlueCmdTimer", this.blueCmdVoteTimer);
        CompoundTag blueVotesTag = new CompoundTag();
        this.blueCmdVotes.forEach((uuid, val) -> blueVotesTag.putBoolean(uuid.toString(), val));
        tag.put("BlueCmdVotesMap", blueVotesTag);
        tag.putBoolean("RedCmdActive", this.redCmdVoteActive);
        tag.putString("RedCmdCandName", this.redCmdCandidateName);
        tag.putInt("RedCmdCandId", this.redCmdCandidateId);
        tag.putInt("RedCmdTimer", this.redCmdVoteTimer);
        CompoundTag redVotesTag = new CompoundTag();
        this.redCmdVotes.forEach((uuid, val) -> redVotesTag.putBoolean(uuid.toString(), val));
        tag.put("RedCmdVotesMap", redVotesTag);
        ListTag squadList = new ListTag();

        for (WarfareWorldData.Squad s : this.squads) {
            squadList.add(s.save());
        }

        tag.put("Squads", squadList);
    }

    /** Читает свои ключи из корня тега WarfareWorldData. */
    public void load(CompoundTag tag) {
        this.blueCMDId = tag.getInt("BlueCMDId");
        this.redCMDId = tag.getInt("RedCMDId");
        this.blueCmdVoteActive = tag.getBoolean("BlueCmdActive");
        this.blueCmdCandidateName = tag.getString("BlueCmdCandName");
        this.blueCmdCandidateId = tag.getInt("BlueCmdCandId");
        this.blueCmdVoteTimer = tag.getInt("BlueCmdTimer");
        if (tag.contains("BlueCmdVotesMap")) {
            CompoundTag cv = tag.getCompound("BlueCmdVotesMap");

            for (String key : cv.getAllKeys()) {
                this.blueCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
            }
        }

        this.redCmdVoteActive = tag.getBoolean("RedCmdActive");
        this.redCmdCandidateName = tag.getString("RedCmdCandName");
        this.redCmdCandidateId = tag.getInt("RedCmdCandId");
        this.redCmdVoteTimer = tag.getInt("RedCmdTimer");
        if (tag.contains("RedCmdVotesMap")) {
            CompoundTag cv = tag.getCompound("RedCmdVotesMap");

            for (String key : cv.getAllKeys()) {
                this.redCmdVotes.put(UUID.fromString(key), cv.getBoolean(key));
            }
        }

        if (tag.contains("Squads")) {
            ListTag list = tag.getList("Squads", 10);

            for (int i = 0; i < list.size(); i++) {
                this.squads.add(WarfareWorldData.Squad.load(list.getCompound(i)));
            }
        }
    }
}
