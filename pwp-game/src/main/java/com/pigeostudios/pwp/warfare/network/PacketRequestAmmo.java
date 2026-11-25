package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

// Пакет запроса боеприпасов из хаба (FOB)
// Поддерживает пополнение кита, боеприпасы для M2, АГС, миномёта и TOW
public class PacketRequestAmmo {
    private final BlockPos pos;
    private final int type;

    public PacketRequestAmmo(BlockPos pos, int type) {
        this.pos = pos;
        this.type = type;
    }

    public static void encode(PacketRequestAmmo msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.type);
    }

    public static PacketRequestAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestAmmo(buf.readBlockPos(), buf.readInt());
    }

    // Обрабатывает запрос: проверяет дистанцию, стоимость, кулдаун и выдаёт предметы
    public static void handle(PacketRequestAmmo msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (!(player.distanceToSqr(msg.pos.getX(), msg.pos.getY(), msg.pos.getZ()) > 64.0)) {
                    if (player.level().getBlockEntity(msg.pos) instanceof HubBlockEntity hub) {
                        int cost = 0;
                        int cooldownTime = 0;
                        boolean isOnCooldown = false;
                        switch (msg.type) {
                            case 0:
                                String pendingKit = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
                                String currentKitName = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
                                boolean hasPending = !pendingKit.isEmpty();
                                String targetKit = hasPending ? pendingKit : currentKitName;
                                if (!targetKit.isEmpty() && !targetKit.equals("Unassigned")) {
                                    long lastFobUse = WarfareData.of(player).getLong(WarfareKeys.LAST_FOB_RESUPPLY);
                                    long currentTime = GameClock.worldTicks(player.level());
                                    if (!player.isCreative() && currentTime < lastFobUse + 1200L) {
                                        long secondsLeft = (lastFobUse + 1200L - currentTime) / 20L;
                                        player.sendSystemMessage(
                                                Component.translatable("pwpwarfare.msg.resupply.cooldown", secondsLeft)
                                                        .withStyle(ChatFormatting.RED));
                                        return;
                                    }

                                    cost = (Integer) WarfareConfig.HUB_RESUPPLY_COST.get();
                                    if (!player.isCreative() && hub.getMaterials() < cost) {
                                        player.sendSystemMessage(
                                                Component.translatable("pwpwarfare.msg.resupply.not_enough", cost)
                                                        .withStyle(ChatFormatting.RED));
                                        return;
                                    }

                                    WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                                    if (hasPending) {
                                        ResupplyHandler.tryApplyPendingKit(player, data);
                                        if (!player.isCreative()) {
                                            hub.consumeMaterials(cost);
                                            WarfareData.of(player).putLong(WarfareKeys.LAST_FOB_RESUPPLY, currentTime);
                                        }

                                        player.level()
                                                .sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                                        player.sendSystemMessage(
                                                Component.translatable("pwpwarfare.msg.resupply.kit_new", cost)
                                                        .withStyle(ChatFormatting.GREEN));
                                    } else {
                                        String t = player.getTeam() != null
                                                ? player.getTeam().getName().toUpperCase()
                                                : "NEUTRAL";
                                        WarfareWorldData.KitInfo kit = t.equals("BLUE")
                                                ? data.blueKits.get(currentKitName)
                                                : data.redKits.get(currentKitName);
                                        if (kit != null) {
                                            if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                                if (!player.isCreative()) {
                                                    hub.consumeMaterials(cost);
                                                    WarfareData.of(player)
                                                            .putLong(WarfareKeys.LAST_FOB_RESUPPLY, currentTime);
                                                }

                                                player.level()
                                                        .sendBlockUpdated(
                                                                msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                                                player.sendSystemMessage(Component.translatable(
                                                                "pwpwarfare.msg.resupply.kit_resupplied", cost)
                                                        .withStyle(ChatFormatting.GREEN));
                                            } else {
                                                player.sendSystemMessage(
                                                        Component.translatable("pwpwarfare.msg.ammo.full")
                                                                .withStyle(ChatFormatting.YELLOW));
                                            }
                                        }
                                    }

                                    return;
                                }

                                player.sendSystemMessage(Component.translatable("pwpwarfare.msg.kit.not_equipped")
                                        .withStyle(ChatFormatting.RED));
                                return;
                            case 3:
                                cost = 20;
                                cooldownTime = 1200;
                                if (hub.cooldownMortar > 0) {
                                    isOnCooldown = true;
                                }
                                break;
                            case 4:
                                cost = 50;
                                cooldownTime = 2400;
                                if (hub.cooldownTOW > 0) {
                                    isOnCooldown = true;
                                }
                        }

                        if (!player.isCreative() && isOnCooldown) {
                            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.resupply.supply_cooldown")
                                    .withStyle(ChatFormatting.RED));
                            return;
                        }

                        if (!player.isCreative() && hub.getMaterials() < cost) {
                            player.sendSystemMessage(
                                    Component.translatable("pwpwarfare.msg.resupply.not_enough_build", cost)
                                            .withStyle(ChatFormatting.RED));
                            return;
                        }

                        boolean success = false;
                        if (msg.type == 3) {
                            Item mortarItem = (Item) ForgeRegistries.ITEMS.getValue(
                                    new ResourceLocation("superbwarfare", "mortar_shell"));
                            if (mortarItem == null || mortarItem == Items.AIR) {
                                mortarItem = Items.ARROW;
                            }

                            ItemStack stack = new ItemStack(mortarItem, 8);
                            if (player.getInventory().add(stack)) {
                                success = true;
                            } else {
                                player.drop(stack, false);
                            }

                            success = true;
                        } else if (msg.type == 4) {
                            Item towItem = (Item) ForgeRegistries.ITEMS.getValue(
                                    new ResourceLocation("superbwarfare", "medium_anti_ground_missile"));
                            if (towItem == null || towItem == Items.AIR) {
                                towItem = Items.SPECTRAL_ARROW;
                            }

                            ItemStack stack = new ItemStack(towItem, 2);
                            if (player.getInventory().add(stack)) {
                                success = true;
                            } else {
                                player.drop(stack, false);
                            }

                            success = true;
                        }

                        if (success) {
                            if (!player.isCreative()) {
                                hub.consumeMaterials(cost);
                                hub.setCooldown(msg.type, cooldownTime);
                            }

                            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.resupply.done", cost)
                                    .withStyle(ChatFormatting.GREEN));
                            player.level().sendBlockUpdated(msg.pos, hub.getBlockState(), hub.getBlockState(), 3);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
