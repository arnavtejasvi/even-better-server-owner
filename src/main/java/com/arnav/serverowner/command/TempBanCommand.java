package com.arnav.serverowner.command;

import com.arnav.serverowner.ServerOwnerUtils;
import com.arnav.serverowner.module.PunishmentModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Date;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TempBanCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tempban")
            .requires(s -> s.hasPermissionLevel(3))
            .then(argument("player", StringArgumentType.word())
                .then(argument("duration", StringArgumentType.word())
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> doBan(ctx.getSource(),
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "duration"),
                            StringArgumentType.getString(ctx, "reason"))))
                    .executes(ctx -> doBan(ctx.getSource(),
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "duration"),
                        "No reason given"))))
        );

        dispatcher.register(literal("history")
            .requires(s -> s.hasPermissionLevel(2))
            .then(argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "player");
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
                    if (target == null) {
                        src.sendFeedback(() -> Text.literal("§c" + name + " is not online."), false);
                        return 0;
                    }
                    List<PunishmentModule.PunishmentEntry> hist = PunishmentModule.getHistory(target.getUuid());
                    if (hist.isEmpty()) {
                        src.sendFeedback(() -> Text.literal("§7[ServerOwner] No punishment history for " + name + "."), false);
                    } else {
                        StringBuilder sb = new StringBuilder("§e[ServerOwner] §fHistory for " + name + ":\n");
                        int start = Math.max(0, hist.size() - 10);
                        for (int i = start; i < hist.size(); i++) {
                            sb.append("  §7").append(PunishmentModule.formatEntry(hist.get(i))).append("\n");
                        }
                        src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                    }
                    return 1;
                }))
        );
    }

    private static int doBan(ServerCommandSource src, String name, String duration, String reason) {
        ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
        if (target == null) {
            src.sendFeedback(() -> Text.literal("§c" + name + " is not online."), false);
            return 0;
        }
        long ms = ServerOwnerUtils.parseDurationMs(duration);
        Date expiry = ms == 0 ? null : new Date(System.currentTimeMillis() + ms);

        var banList = src.getServer().getPlayerManager().getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(target.getGameProfile(), null, src.getName(), expiry, reason);
        banList.add(entry);

        String label = ms == 0 ? "permanently" : "for " + ServerOwnerUtils.formatDuration(ms);
        target.networkHandler.disconnect(Text.literal("§c[ServerOwner] You have been banned " + label + ".\nReason: " + reason));

        PunishmentModule.record(target.getUuid(), name, "tempban",
            reason + " (" + (ms == 0 ? "permanent" : ServerOwnerUtils.formatDuration(ms)) + ")", src.getName());
        ServerOwnerUtils.notifyOps(src.getServer(), "§e[ServerOwner] §f" + name + " §7banned " + label + ": §f" + reason);
        return 1;
    }
}
