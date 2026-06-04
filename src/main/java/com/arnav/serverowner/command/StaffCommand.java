package com.arnav.serverowner.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class StaffCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("staff")
            .executes(ctx -> {
                ServerCommandSource src = ctx.getSource();
                var ops = src.getServer().getPlayerManager().getPlayerList().stream()
                    .filter(p -> p.hasPermissionLevel(2))
                    .toList();
                if (ops.isEmpty()) {
                    src.sendFeedback(() -> Text.literal("§7[ServerOwner] No staff online."), false);
                } else {
                    StringBuilder sb = new StringBuilder("§e[ServerOwner] §fOnline staff:\n");
                    ops.forEach(p -> sb.append("  §7- §f").append(p.getName().getString()).append("\n"));
                    src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                }
                return 1;
            })
        );
    }
}
