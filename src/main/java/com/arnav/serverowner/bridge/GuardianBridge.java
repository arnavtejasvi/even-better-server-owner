package com.arnav.serverowner.bridge;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class GuardianBridge {
    private GuardianBridge() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("serverguard");
    }

    public static void logToWatchlist(UUID targetUuid, String event) {
        // No Guardian Fabric version exists; no-op
    }

    public static double getGuardianTps(MinecraftServer server) {
        return -1;
    }
}
