package com.arnav.serverowner.command;

import com.arnav.serverowner.module.DatapackModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DataManagerCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("datamanager")
            .requires(s -> s.hasPermissionLevel(2))

            .then(literal("list")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    List<String> enabled = DatapackModule.listEnabled(src.getServer());
                    List<String> available = DatapackModule.listAvailable(src.getServer());
                    StringBuilder sb = new StringBuilder("§e[DataManager] §fDatapacks:\n");
                    sb.append("§aEnabled (").append(enabled.size()).append("):\n");
                    enabled.forEach(p -> sb.append("  §7- §f").append(p).append("\n"));
                    List<String> disabled = available.stream().filter(p -> !enabled.contains(p)).toList();
                    if (!disabled.isEmpty()) {
                        sb.append("§7Disabled (").append(disabled.size()).append("):\n");
                        disabled.forEach(p -> sb.append("  §8- §7").append(p).append("\n"));
                    }
                    src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                    return 1;
                }))

            .then(literal("enable")
                .requires(s -> s.hasPermissionLevel(3))
                .then(argument("pack", StringArgumentType.word())
                    .executes(ctx -> {
                        String pack = StringArgumentType.getString(ctx, "pack");
                        ServerCommandSource src = ctx.getSource();
                        boolean ok = DatapackModule.enable(src.getServer(), pack);
                        src.sendFeedback(() -> Text.literal(ok
                            ? "§a[DataManager] Enabled datapack '" + pack + "' and reloading..."
                            : "§c[DataManager] Could not enable '" + pack + "' (not found or already enabled)."), false);
                        return ok ? 1 : 0;
                    })))

            .then(literal("disable")
                .requires(s -> s.hasPermissionLevel(3))
                .then(argument("pack", StringArgumentType.word())
                    .executes(ctx -> {
                        String pack = StringArgumentType.getString(ctx, "pack");
                        ServerCommandSource src = ctx.getSource();
                        boolean ok = DatapackModule.disable(src.getServer(), pack);
                        src.sendFeedback(() -> Text.literal(ok
                            ? "§a[DataManager] Disabled datapack '" + pack + "' and reloading..."
                            : "§c[DataManager] Could not disable '" + pack + "' (not found or already disabled)."), false);
                        return ok ? 1 : 0;
                    })))

            .then(literal("reload")
                .requires(s -> s.hasPermissionLevel(3))
                .executes(ctx -> {
                    DatapackModule.reload(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("§a[DataManager] Reloading datapacks..."), false);
                    return 1;
                }))
        );
    }
}
