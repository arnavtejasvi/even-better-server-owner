package com.arnav.serverowner.command;

import com.arnav.serverowner.ServerOwnerUtils;
import com.arnav.serverowner.module.ModerationModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MuteCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mute")
            .requires(s -> s.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .then(argument("duration", StringArgumentType.word())
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> doMute(ctx.getSource(),
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "duration"),
                            StringArgumentType.getString(ctx, "reason"))))
                    .executes(ctx -> doMute(ctx.getSource(),
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "duration"),
                        "No reason given")))
                .executes(ctx -> doMute(ctx.getSource(),
                    StringArgumentType.getString(ctx, "player"),
                    "perm", "No reason given")))
        );

        dispatcher.register(literal("unmute")
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
                    boolean unmuted = ModerationModule.unmute(uuid);
                    src.sendFeedback(() -> Text.literal(unmuted
                        ? "§a[ServerOwner] Unmuted " + name + "."
                        : "§7[ServerOwner] " + name + " was not muted."), false);
                    ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
                    if (unmuted && target != null) {
                        target.sendMessage(Text.literal("§a[ServerOwner] You have been unmuted."));
                    }
                    return 1;
                }))
        );
    }

    private static int doMute(ServerCommandSource src, String name, String duration, String reason) {
        ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
        if (target == null) {
            src.sendFeedback(() -> Text.literal("§c" + name + " is not online."), false);
            return 0;
        }
        long ms = ServerOwnerUtils.parseDurationMs(duration);
        ModerationModule.mute(target, ms, reason, src.getName());
        String label = ms == 0 ? "permanently" : "for " + ServerOwnerUtils.formatDuration(ms);
        ServerOwnerUtils.notifyOps(src.getServer(), "§e[ServerOwner] §f" + name + " §7muted " + label + ": §f" + reason);
        return 1;
    }

    private static UUID resolveUuid(ServerCommandSource src, String name) {
        ServerPlayerEntity online = src.getServer().getPlayerManager().getPlayer(name);
        return online != null ? online.getUuid() : null;
    }
}
