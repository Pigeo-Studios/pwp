package com.pigeostudios.pwp.warfare.world;

import java.util.UUID;
import net.minecraft.core.BlockPos;

public class MapMarker {
    public UUID id;
    public String team, category, iconType;
    public BlockPos pos;
    public UUID ownerUUID;
    public long createdAt;

    public long clientExpiryTick;

    public MapMarker(UUID id, String team, String category, String iconType, BlockPos pos, UUID ownerUUID, long createdAt) {
        this.id = id; this.team = team; this.category = category; this.iconType = iconType;
        this.pos = pos; this.ownerUUID = ownerUUID; this.createdAt = createdAt;
    }

    public boolean isExpired(long now) { return (now - createdAt) > 6000; }

    public float getAlpha(long now) {
        long elapsed = now - createdAt;
        if (elapsed >= 6000) return 0;
        if (elapsed <= 4000) return 1;
        return 1 - (elapsed - 4000) / 2000f;
    }
}
