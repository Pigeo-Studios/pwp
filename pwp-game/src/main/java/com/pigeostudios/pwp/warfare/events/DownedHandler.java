package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.CombatRules;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncDownedState;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "pwpwarfare")
// Обработчик состояния «ранен» (knockout)
// Управляет нокаутом игроков, возможностью оживления и истечением крови
public class DownedHandler {
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof ServerPlayer
                && event.getSource().getEntity() instanceof ServerPlayer) {
            WarfareWorldData data =
                    WarfareWorldData.get((ServerLevel) event.getEntity().level());
            if (!data.match.isGameStarted || data.match.isPaused) {
                event.setCanceled(true);
                return;
            }
        }
        // Track damage dealt by a player to any living entity
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getEntity() != event.getEntity()) {
            MatchStatsTracker.get().recordDamage(attacker, event.getAmount());
            // Tag vehicles with last damager for destruction tracking
            if (WarfareData.of(event.getEntity()).contains(WarfareKeys.TICKET_PENALTY)) {
                WarfareData.of(event.getEntity()).putString(WarfareKeys.LAST_DAMAGER, attacker.getStringUUID());
            }
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            if ((Boolean) WarfareConfig.ENABLE_KNOCKOUT.get()) {
                if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    if (!WarfareData.of(player).getBoolean(WarfareKeys.GIVING_UP)) {
                        if (WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED)) {
                            if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                                WarfareData.of(player).putString(WarfareKeys.KNOCKED_BY, attacker.getScoreboardName());
                                if (attacker.getTeam() != null
                                        && player.getTeam() != null
                                        && attacker.getTeam().isAlliedTo(player.getTeam())) {
                                    handleTeamkill(attacker);
                                }
                            }
                            if (event.getAmount() >= player.getHealth()) {
                                event.setCanceled(true);
                                forceGiveUp(player);
                            }
                            return;
                        } else {
                            if (event.getAmount() >= player.getHealth()) {
                                if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                                    return;
                                }

                                long lastRevive = WarfareData.of(player).getLong(WarfareKeys.LAST_REVIVE_TIME);
                                if (lastRevive > 0L
                                        && GameClock.worldTicks(player.level()) - lastRevive
                                                < (Integer) WarfareConfig.REVIVE_COOLDOWN_SECONDS.get() * 20) {
                                    return;
                                }

                                event.setCanceled(true);
                                if (event.getSource().getEntity() instanceof Player attackingPlayer) {
                                    WarfareData.of(player)
                                            .putString(WarfareKeys.KNOCKED_BY, attackingPlayer.getScoreboardName());
                                } else if (player.getLastHurtByMob() instanceof Player lastAttacker) {
                                    WarfareData.of(player)
                                            .putString(WarfareKeys.KNOCKED_BY, lastAttacker.getScoreboardName());
                                } else {
                                    WarfareData.of(player).remove(WarfareKeys.KNOCKED_BY);
                                }

                                enterDownedState(player);
                            }
                        }
                    }
                }
            }
        }
    }

    // Перевод игрока в состояние «ранен»
    public static void enterDownedState(ServerPlayer player) {
        if (player.isPassenger()) {
            player.stopRiding();
        }

        WarfareData.of(player).putBoolean(WarfareKeys.IS_DOWNED, true);
        WarfareData.of(player).putLong(WarfareKeys.DOWNED_TICK, GameClock.worldTicks(player.level()));
        player.setHealth(6.0F);
        player.setPose(Pose.SWIMMING);
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        (SoundEvent) ModSounds.PLAYER_DOWNED.get(),
                        SoundSource.PLAYERS,
                        1.5F,
                        1.0F);
        PacketHandler.syncDowned(player, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;
            if (WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED)) {
                player.setPose(Pose.SWIMMING);
                player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                long downedTick = WarfareData.of(player).getLong(WarfareKeys.DOWNED_TICK);
                long maxDownedTicks = ((Integer) WarfareConfig.MAX_DOWNED_TIME_SECONDS.get()).intValue() * 20L;
                if (GameClock.worldTicks(player.level()) - downedTick >= maxDownedTicks) {
                    forceGiveUp(player);
                }
            }
            enforceBaseRestriction(player);
            if (WarfareData.of(player).getBoolean(WarfareKeys.TEAM_KILL_SPECTATOR)) {
                enforceTeamkillSpectator(player);
            }
        }
    }

    // Принудительная смерть игрока (истечение крови или сдача)
    public static void forceGiveUp(ServerPlayer player) {
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.PLAYER_DOWNED.get(),
                        SoundSource.PLAYERS,
                        1.5F,
                        1.0F);
        String victimName = player.getScoreboardName();
        String killerName = WarfareData.of(player).getString(WarfareKeys.KNOCKED_BY);
        ServerLevel level = player.serverLevel();
        boolean showMessages = level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (showMessages) {
            WarfareWorldData wd = WarfareWorldData.get(level);
            if (!wd.match.hideDeathMessages) {
                MutableComponent logMsg;
                if (killerName != null && !killerName.isEmpty()) {
                    logMsg = Component.translatable(
                                    "pwpwarfare.msg.downed.finished_by",
                                    Component.literal(victimName).withStyle(ChatFormatting.WHITE),
                                    Component.literal(killerName).withStyle(ChatFormatting.GOLD))
                            .withStyle(ChatFormatting.GRAY);
                } else {
                    logMsg = Component.translatable(
                                    "pwpwarfare.msg.downed.bled_out",
                                    Component.literal(victimName).withStyle(ChatFormatting.WHITE))
                            .withStyle(ChatFormatting.GRAY);
                }

                player.server.getPlayerList().broadcastSystemMessage(logMsg, false);
            }
        }

        if (killerName != null && !killerName.isEmpty()) {
            ServerPlayer killer = player.server.getPlayerList().getPlayerByName(killerName);
            if (killer != null) {
                player.setLastHurtByPlayer(killer);
            }
        }

        WarfareData.of(player).remove(WarfareKeys.KNOCKED_BY);
        WarfareData.of(player).putBoolean(WarfareKeys.GIVING_UP, true);
        WarfareData.of(player).putBoolean(WarfareKeys.IS_DOWNED, false);
        PacketHandler.syncDowned(player, false);
        player.kill();
    }

    @SubscribeEvent
    public static void onReviveInteract(EntityInteract event) {
        if (event.getTarget() instanceof ServerPlayer target) {
            ServerPlayer medic = (ServerPlayer) event.getEntity();
            if (WarfareData.of(target).getBoolean(WarfareKeys.IS_DOWNED)) {
                if (medic.getTeam() == null
                        || target.getTeam() == null
                        || !medic.getTeam().isAlliedTo(target.getTeam())) {
                    if (!medic.level().isClientSide) {
                        medic.displayClientMessage(
                                Component.translatable("pwpwarfare.revive.enemy")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }

                    event.setCanceled(true);
                    return;
                }

                ItemStack held = medic.getItemInHand(event.getHand());

                ResourceLocation heldId = ForgeRegistries.ITEMS.getKey(held.getItem());
                if (heldId.equals(new ResourceLocation("pwp_medicine", "bandage"))) {
                    return;
                }

                String reviveItemName = (String) WarfareConfig.REVIVE_ITEM.get();
                Item reviveItem = (Item) ForgeRegistries.ITEMS.getValue(new ResourceLocation(reviveItemName));
                if (held.getItem() == reviveItem) {
                    event.setCanceled(true);
                    // как в Squad: поднимает только медик, удерживая предмет REVIVE_TIME_SECONDS
                    if (!CombatRules.canRevive(
                            WarfareData.of(medic).getString(WarfareKeys.CURRENT_KIT), medic.isCreative())) {
                        medic.displayClientMessage(
                                Component.translatable("pwpwarfare.revive.medic_only")
                                        .withStyle(ChatFormatting.RED),
                                true);
                        return;
                    }
                    ReviveChannel cur = REVIVES.get(medic.getUUID());
                    if (cur != null && cur.target.equals(target.getUUID())) return; // уже поднимает
                    REVIVES.put(
                            medic.getUUID(),
                            new ReviveChannel(target.getUUID(), GameClock.serverTicks(medic.server), event.getHand()));
                    medic.displayClientMessage(
                            Component.translatable("pwpwarfare.revive.progress", 0)
                                    .withStyle(ChatFormatting.GREEN),
                            true);
                }
            }
        }
    }

    // Оживление игрока из состояния «ранен»
    public static void handleTeamkill(ServerPlayer attacker) {
        int teamKills = WarfareData.of(attacker).getInt(WarfareKeys.TEAM_KILLS) + 1;
        WarfareData.of(attacker).putInt(WarfareKeys.TEAM_KILLS, teamKills);

        if (teamKills >= WarfareConfig.TEAMKILL_SPECTATOR_THRESHOLD.get()) {
            attacker.setGameMode(GameType.SPECTATOR);
            WarfareData.of(attacker).putBoolean(WarfareKeys.TEAM_KILL_SPECTATOR, true);
            MutableComponent broadcast = Component.translatable(
                            "pwpwarfare.message.teamkill_spectator_broadcast", attacker.getScoreboardName())
                    .withStyle(ChatFormatting.RED);
            attacker.server.getPlayerList().broadcastSystemMessage(broadcast, false);
            attacker.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_spectator_self")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (teamKills >= 5) {
            sendTeamkillWarning(attacker);
            long restrictionUntil = GameClock.worldTicks(attacker.level()) + 20L * 60L * 5L;
            WarfareData.of(attacker).putLong(WarfareKeys.BASE_RESTRICTED_UNTIL, restrictionUntil);
            attacker.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (teamKills >= 1) {
            sendTeamkillWarning(attacker);
        }
    }

    private static void sendTeamkillWarning(ServerPlayer target) {
        target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 140, 20));
        target.connection.send(
                new ClientboundSetSubtitleTextPacket(Component.translatable("pwpwarfare.message.teamkill_warn_subtitle")
                        .withStyle(ChatFormatting.YELLOW)));
        target.connection.send(
                new ClientboundSetTitleTextPacket(Component.translatable("pwpwarfare.msg.teamkill.warning_title")
                        .withStyle(new ChatFormatting[] {ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
        target.playNotifySound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0F, 0.8F);
    }

    private static void enforceBaseRestriction(ServerPlayer player) {
        long restrictedUntil = WarfareData.of(player).getLong(WarfareKeys.BASE_RESTRICTED_UNTIL);
        if (restrictedUntil <= 0) return;

        if (GameClock.worldTicks(player.level()) >= restrictedUntil) {
            WarfareData.of(player).remove(WarfareKeys.BASE_RESTRICTED_UNTIL);
            player.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict_expired")
                    .withStyle(ChatFormatting.GREEN));
            return;
        }

        String teamName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        if (teamName.isEmpty()) return;

        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        String currentDim = player.level().dimension().location().toString();

        boolean insideZone = false;
        for (WarfareWorldData.MainProtectionZone zone : data.structures.mainZones) {
            if (zone.team.equalsIgnoreCase(teamName) && zone.isInside(player.position())) {
                insideZone = true;
                break;
            }
        }

        if (insideZone) return;

        BlockPos spawn =
                teamName.equals("BLUE") ? data.match.blueSpawns.get(currentDim) : data.match.redSpawns.get(currentDim);
        if (spawn == null) return;

        player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        long remaining = (restrictedUntil - GameClock.worldTicks(player.level())) / 20L;
        player.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict_remaining", remaining)
                .withStyle(ChatFormatting.RED));
        player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 1.0F, 1.0F);
    }

    public static void enforceTeamkillSpectator(ServerPlayer player) {
        if (WarfareData.of(player).getBoolean(WarfareKeys.TEAM_KILL_SPECTATOR)) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                player.setGameMode(GameType.SPECTATOR);
            }
        }
    }

    public static void clearTeamkillPunishment(ServerPlayer player) {
        WarfareData.of(player).remove(WarfareKeys.TEAM_KILLS);
        WarfareData.of(player).remove(WarfareKeys.BASE_RESTRICTED_UNTIL);
        boolean wasSpectator = WarfareData.of(player).getBoolean(WarfareKeys.TEAM_KILL_SPECTATOR);
        WarfareData.of(player).remove(WarfareKeys.TEAM_KILL_SPECTATOR);
        if (wasSpectator && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(GameType.SURVIVAL);
        }
        player.sendSystemMessage(
                Component.translatable("pwpwarfare.msg.teamkill.cleared").withStyle(ChatFormatting.GREEN));
    }

    public static void revivePlayer(ServerPlayer player) {
        WarfareData.of(player).putBoolean(WarfareKeys.IS_DOWNED, false);
        WarfareData.of(player).remove(WarfareKeys.DOWNED_TICK);
        WarfareData.of(player).remove(WarfareKeys.GIVING_UP);
        WarfareData.of(player).putLong(WarfareKeys.LAST_REVIVE_TIME, GameClock.worldTicks(player.level()));
        player.setHealth(2.0F);
        player.setPose(Pose.STANDING);
        PacketHandler.syncDowned(player, false);
    }

    /**
     * Враг вошёл в обзор раненого — досылаем состояние нока (поза лежащего). Своей команде
     * оно уходит сразу из {@code PacketHandler.syncDowned}, врагам — только при видимости.
     */
    @SubscribeEvent
    public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target
                && event.getEntity() instanceof ServerPlayer tracker
                && WarfareData.of(target).getBoolean(WarfareKeys.IS_DOWNED)) {
            PacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> tracker), new PacketSyncDownedState(target.getId(), true));
        }
    }

    // ===== Канал подъёма (в памяти: serverTicks; рестарт сервера = канал сброшен) =====

    private record ReviveChannel(java.util.UUID target, long startTick, net.minecraft.world.InteractionHand hand) {}

    private static final java.util.Map<java.util.UUID, ReviveChannel> REVIVES = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onReviveTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != Phase.END || REVIVES.isEmpty()) return;
        net.minecraft.server.MinecraftServer server = event.getServer();
        long now = GameClock.serverTicks(server);
        long required = WarfareConfig.REVIVE_TIME_SECONDS.get() * 20L;
        var it = REVIVES.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer medic = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer target = server.getPlayerList().getPlayer(e.getValue().target());
            if (medic == null || target == null) {
                it.remove();
                continue;
            }
            Item reviveItem =
                    ForgeRegistries.ITEMS.getValue(new ResourceLocation((String) WarfareConfig.REVIVE_ITEM.get()));
            ItemStack held = medic.getItemInHand(e.getValue().hand());
            boolean stateOk = WarfareData.of(target).getBoolean(WarfareKeys.IS_DOWNED)
                    && medic.isAlive()
                    && !WarfareData.of(medic).getBoolean(WarfareKeys.IS_DOWNED)
                    && medic.level() == target.level()
                    && held.getItem() == reviveItem;
            long elapsed = now - e.getValue().startTick();
            switch (CombatRules.reviveStep(elapsed, required, medic.distanceToSqr(target), stateOk)) {
                case CONTINUE -> {
                    if (elapsed % 10 == 0) {
                        medic.displayClientMessage(
                                Component.translatable(
                                                "pwpwarfare.revive.progress",
                                                CombatRules.revivePercent(elapsed, required))
                                        .withStyle(ChatFormatting.GREEN),
                                true);
                    }
                }
                case DONE -> {
                    it.remove();
                    revivePlayer(target);
                    MatchStatsTracker.get().recordRevive(medic);
                    if (!medic.isCreative()) held.shrink(1);
                    medic.displayClientMessage(
                            Component.translatable("pwpwarfare.revive.done").withStyle(ChatFormatting.GREEN), true);
                }
                case CANCEL_DISTANCE, CANCEL_STATE -> {
                    it.remove();
                    if (WarfareData.of(target).getBoolean(WarfareKeys.IS_DOWNED)) {
                        medic.displayClientMessage(
                                Component.translatable("pwpwarfare.revive.cancelled")
                                        .withStyle(ChatFormatting.YELLOW),
                                true);
                    }
                }
            }
        }
    }
}
