package com.arnav.serverowner.command;

import com.arnav.serverowner.module.ModerationModule;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class VanishCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("vanish")
            .requires(s -> s.hasPermissionLevel(2))
            .executes(ctx -> {
                ServerCommandSource src = ctx.getSource();
                ServerPlayerEntity player;
                try { player = src.getPlayerOrThrow(); }
                catch (Exception e) {
                    src.sendFeedback(() -> Text.literal("§cMust be run by a player."), false);
                    return 0;
                }
                boolean nowVanished = ModerationModule.toggleVanish(player);
                player.sendMessage(Text.literal(nowVanished
                    ? "§7[ServerOwner] You are now vanished."
                    : "§a[ServerOwner] You are now visible."));
                return 1;
            })
        );
    }
}
