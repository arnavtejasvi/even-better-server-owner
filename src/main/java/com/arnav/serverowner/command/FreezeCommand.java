package com.arnav.serverowner.command;

import com.arnav.serverowner.ServerOwnerUtils;
import com.arnav.serverowner.module.ModerationModule;
import com.arnav.serverowner.module.PunishmentModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FreezeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("freeze")
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
                    boolean froze = ModerationModule.freeze(target);
                    if (!froze) {
                        src.sendFeedback(() -> Text.literal("§c" + name + " is already frozen."), false);
                        return 0;
                    }
                    PunishmentModule.record(target.getUuid(), name, "freeze", "frozen in place", src.getName());
                    ServerOwnerUtils.notifyOps(src.getServer(), "§e[ServerOwner] §f" + name + " §7has been frozen.");
                    return 1;
                }))
        );

        dispatcher.register(literal("unfreeze")
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
                    boolean unfroze = ModerationModule.unfreeze(target.getUuid());
                    if (!unfroze) {
                        src.sendFeedback(() -> Text.literal("§c" + name + " is not frozen."), false);
                        return 0;
                    }
                    target.sendMessage(Text.literal("§a[ServerOwner] You have been unfrozen."));
                    ServerOwnerUtils.notifyOps(src.getServer(), "§e[ServerOwner] §f" + name + " §7has been unfrozen.");
                    return 1;
                }))
        );
    }
}
