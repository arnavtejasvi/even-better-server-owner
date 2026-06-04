package com.arnav.serverowner.command;

import com.arnav.serverowner.ServerOwnerMod;
import com.arnav.serverowner.module.ModerationModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarnCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("warn")
            .requires(s -> s.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .then(argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "player");
                        String reason = StringArgumentType.getString(ctx, "reason");
                        ServerCommandSource src = ctx.getSource();
                        ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
                        if (target == null) {
                            src.sendFeedback(() -> Text.literal("§c" + name + " is not online."), false);
                            return 0;
                        }
                        int count = ModerationModule.addWarning(target, reason, src.getName());
                        int threshold = ServerOwnerMod.config.moderation.autoKickAfterWarnings;
                        String msg = "§e[ServerOwner] §f" + name + " §7warned (" + count + "/" + threshold + "): §f" + reason;
                        src.getServer().getPlayerManager().getPlayerList().stream()
                            .filter(p -> p.hasPermissionLevel(2))
                            .forEach(p -> p.sendMessage(Text.literal(msg)));
                        return 1;
                    })))
        );

        dispatcher.register(literal("warnings")
            .requires(s -> s.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    ServerCommandSource src = ctx.getSource();
                    UUID uuid = resolveUuid(src, name);
                    if (uuid == null) {
                        src.sendFeedback(() -> Text.literal("§cPlayer not found: " + name), false);
                        return 0;
                    }
                    List<ModerationModule.WarnEntry> list = ModerationModule.getWarnings(uuid);
                    if (list.isEmpty()) {
                        src.sendFeedback(() -> Text.literal("§7[ServerOwner] No warnings for " + name + "."), false);
                    } else {
                        StringBuilder sb = new StringBuilder("§e[ServerOwner] §fWarnings for " + name + ":\n");
                        for (int i = 0; i < list.size(); i++) {
                            ModerationModule.WarnEntry w = list.get(i);
                            sb.append("  §7").append(i + 1).append(". §f").append(w.reason()).append(" §7(by ").append(w.by()).append(")\n");
                        }
                        src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                    }
                    return 1;
                }))
        );

        dispatcher.register(literal("clearwarnings")
            .requires(s -> s.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    ServerCommandSource src = ctx.getSource();
                    UUID uuid = resolveUuid(src, name);
                    if (uuid == null) {
                        src.sendFeedback(() -> Text.literal("§cPlayer not found: " + name), false);
                        return 0;
                    }
                    boolean cleared = ModerationModule.clearWarnings(uuid);
                    src.sendFeedback(() -> Text.literal(cleared
                        ? "§a[ServerOwner] Cleared all warnings for " + name + "."
                        : "§7[ServerOwner] " + name + " had no warnings."), false);
                    return 1;
                }))
        );
    }

    private static UUID resolveUuid(ServerCommandSource src, String name) {
        ServerPlayerEntity online = src.getServer().getPlayerManager().getPlayer(name);
        return online != null ? online.getUuid() : null;
    }
}
