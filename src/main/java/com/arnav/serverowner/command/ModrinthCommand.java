package com.arnav.serverowner.command;

import com.arnav.serverowner.module.ModrinthModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ModrinthCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("modrinth")
            .requires(s -> s.hasPermissionLevel(3))

            .then(literal("search")
                .then(argument("query", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String query = StringArgumentType.getString(ctx, "query");
                        ServerCommandSource src = ctx.getSource();
                        src.sendFeedback(() -> Text.literal("§7[Modrinth] Searching for \"" + query + "\"..."), false);
                        ModrinthModule.search(query, 5).thenAccept(results -> {
                            src.getServer().execute(() -> {
                                if (results.isEmpty()) {
                                    src.sendFeedback(() -> Text.literal("§c[Modrinth] No results found."), false);
                                    return;
                                }
                                StringBuilder sb = new StringBuilder("§e[Modrinth] §fResults for \"" + query + "\":\n");
                                for (var r : results) {
                                    sb.append("  §f").append(r.title()).append(" §8(").append(r.slug()).append(")")
                                      .append(" §7— ").append(r.downloads()).append(" downloads\n")
                                      .append("    §8").append(truncate(r.description(), 80)).append("\n");
                                }
                                sb.append("§7Use §f/modrinth info <slug> §7for details.");
                                src.sendFeedback(() -> Text.literal(sb.toString().stripTrailing()), false);
                            });
                        }).exceptionally(e -> {
                            src.getServer().execute(() ->
                                src.sendFeedback(() -> Text.literal("§c[Modrinth] Search failed: " + e.getMessage()), false));
                            return null;
                        });
                        return 1;
                    })))

            .then(literal("info")
                .then(argument("slug", StringArgumentType.word())
                    .executes(ctx -> {
                        String slug = StringArgumentType.getString(ctx, "slug");
                        ServerCommandSource src = ctx.getSource();
                        src.sendFeedback(() -> Text.literal("§7[Modrinth] Fetching info for \"" + slug + "\"..."), false);
                        ModrinthModule.getProjectInfo(slug).thenAccept(info -> {
                            src.getServer().execute(() -> {
                                if (info == null) {
                                    src.sendFeedback(() -> Text.literal("§c[Modrinth] Project not found: " + slug), false);
                                    return;
                                }
                                src.sendFeedback(() -> Text.literal("§e[Modrinth]\n" + info), false);
                            });
                        }).exceptionally(e -> {
                            src.getServer().execute(() ->
                                src.sendFeedback(() -> Text.literal("§c[Modrinth] Failed: " + e.getMessage()), false));
                            return null;
                        });
                        return 1;
                    })))

        );
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
