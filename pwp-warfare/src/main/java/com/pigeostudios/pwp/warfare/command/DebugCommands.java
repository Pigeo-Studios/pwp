package com.pigeostudios.pwp.warfare.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class DebugCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pwp")
                .then(Commands.literal("debug")
                    .then(Commands.literal("dumpitems")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> dumpItems(ctx.getSource()))
                    )
                    .then(Commands.literal("hand")
                        .executes(ctx -> showHand(ctx.getSource()))
                    )
                )
        );
    }

    private static int showHand(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendSuccess(() -> Component.literal("§cPlayer only"), false);
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eHold an item in your main hand"), false);
            return 0;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String registryName = key != null ? key.toString() : "§cUNKNOWN";
        int damage = stack.getDamageValue();
        boolean hasTag = stack.hasTag();
        String tagHash = hasTag ? String.valueOf(stack.getTag().hashCode()) : "none";

        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Held Item ===\n");
        sb.append("§7Registry Name: §f").append(registryName).append("\n");
        sb.append("§7Display Name: §f").append(stack.getHoverName().getString()).append("\n");
        sb.append("§7Damage: §f").append(damage).append("\n");
        sb.append("§7Has NBT: §f").append(hasTag);
        if (hasTag) sb.append("  Tag Hash: §f").append(tagHash);
        sb.append("\n");
        sb.append("§7Full ID for skin: §e").append(registryName);
        if (damage != 0 || hasTag) sb.append("@d").append(damage);
        if (hasTag) sb.append("@t").append(tagHash);

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int dumpItems(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== Registered Items ===\n");
        int count = 0;
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            sb.append("§7").append(key.toString()).append("\n");
            count++;
            if (count > 200) {
                sb.append("§e... and more (showing first 200)\n");
                break;
            }
        }
        sb.append("§6Total: ").append(BuiltInRegistries.ITEM.keySet().size()).append(" items");
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
