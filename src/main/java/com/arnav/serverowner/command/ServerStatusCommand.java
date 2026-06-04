package com.arnav.serverowner.command;

import com.arnav.serverowner.bridge.GuardianBridge;
import com.arnav.serverowner.module.ModInfoModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerStatusCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("serverstatus")
            .requires(s -> s.hasPermissionLevel(2))

            .then(literal("mods")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    List<ModInfoModule.ModEntry> mods = ModInfoModule.getLoadedMods();
                    StringBuilder sb = new StringBuilder("§e[ServerStatus] §fLoaded mods (" + mods.size() + "):\n");
                    for (ModInfoModule.ModEntry mod : mods) {
                        sb.append("  §7").append(mod.modId()).append(" §8@ §f").append(mod.version())
                          .append(" §7(").append(mod.displayName()).append(")\n");
                    }
                    src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                    return 1;
                }))

            .then(literal("server")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    var server = src.getServer();
                    int online = server.getPlayerManager().getCurrentPlayerCount();
                    int max = server.getPlayerManager().getMaxPlayerCount();
                    String mcVersion = server.getVersion();
                    double tps = GuardianBridge.getGuardianTps(server);
                    String tpsStr = tps < 0 ? "§7N/A §8(install Even Better Server Guardian)" : String.format("§f%.1f", tps);
                    String msg = "§e[ServerStatus] §fServer Info:\n"
                        + "  §7MC Version: §f" + mcVersion + "\n"
                        + "  §7Players: §f" + online + "/" + max + "\n"
                        + "  §7TPS: " + tpsStr;
                    src.sendFeedback(() -> Text.literal(msg), false);
                    return 1;
                }))
        );

        dispatcher.register(literal("serverconfig")
            .requires(s -> s.hasPermissionLevel(3))

            .then(literal("list")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    Map<String, String> props = ModInfoModule.readServerProperties();
                    if (props.isEmpty()) {
                        src.sendFeedback(() -> Text.literal("§7[ServerConfig] server.properties not found."), false);
                        return 0;
                    }
                    StringBuilder sb = new StringBuilder("§e[ServerConfig] §fserver.properties:\n");
                    props.forEach((k, v) -> sb.append("  §7").append(k).append("=§f").append(v).append("\n"));
                    src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                    return 1;
                }))

            .then(literal("get")
                .then(argument("key", StringArgumentType.word())
                    .executes(ctx -> {
                        String key = StringArgumentType.getString(ctx, "key");
                        ServerCommandSource src = ctx.getSource();
                        String value = ModInfoModule.readServerProperties().get(key);
                        if (value == null) {
                            src.sendFeedback(() -> Text.literal("§cKey not found: " + key), false);
                            return 0;
                        }
                        src.sendFeedback(() -> Text.literal("§e[ServerConfig] §7" + key + "=§f" + value), false);
                        return 1;
                    })))

            .then(literal("set")
                .then(argument("key", StringArgumentType.word())
                    .then(argument("value", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String key = StringArgumentType.getString(ctx, "key");
                            String value = StringArgumentType.getString(ctx, "value");
                            ServerCommandSource src = ctx.getSource();
                            boolean ok = ModInfoModule.setServerProperty(key, value);
                            src.sendFeedback(() -> Text.literal(ok
                                ? "§a[ServerConfig] Set §f" + key + "=§a" + value + "§7. Restart required for most settings."
                                : "§c[ServerConfig] Could not write server.properties."), false);
                            return ok ? 1 : 0;
                        }))))
        );
    }
}
