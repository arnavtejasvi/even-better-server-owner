package com.arnav.serverowner;

import com.arnav.serverowner.command.*;
import com.arnav.serverowner.module.ModerationModule;
import com.arnav.serverowner.module.PunishmentModule;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

public class ServerOwnerFabric implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("serverowner");

    @Override
    public void onInitialize() {
        ServerOwnerMod.config = ServerOwnerConfig.load();

        PunishmentModule.init();

        if (ServerOwnerMod.config.moderation.enabled) {
            ModerationModule.init();
            LOGGER.info("ServerOwner: Moderation enabled (auto-kick after {} warnings).",
                ServerOwnerMod.config.moderation.autoKickAfterWarnings);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var cfg = ServerOwnerMod.config;

            if (cfg.moderation.enabled) {
                WarnCommand.register(dispatcher);
                MuteCommand.register(dispatcher);
                FreezeCommand.register(dispatcher);
                VanishCommand.register(dispatcher);
                StaffCommand.register(dispatcher);
            }

            TempBanCommand.register(dispatcher);

            if (cfg.datapack.enabled) {
                DataManagerCommand.register(dispatcher);
            }

            if (cfg.modInfo.enabled) {
                ServerStatusCommand.register(dispatcher);
            }

            if (cfg.modrinth.enabled) {
                ModrinthCommand.register(dispatcher);
            }

            dispatcher.register(
                literal("serverowner")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(literal("reload")
                        .executes(ctx -> {
                            ServerOwnerMod.config = ServerOwnerConfig.load();
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§a[ServerOwner] Config reloaded."), false);
                            return 1;
                        }))
            );
        });

        LOGGER.info("ServerOwner initialized.");
    }
}
