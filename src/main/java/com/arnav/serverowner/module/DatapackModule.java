package com.arnav.serverowner.module;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DatapackModule {

    public static List<String> listEnabled(MinecraftServer server) {
        return server.getDataPackManager().getEnabledProfiles().stream()
            .map(ResourcePackProfile::getId)
            .collect(Collectors.toList());
    }

    public static List<String> listAvailable(MinecraftServer server) {
        return server.getDataPackManager().getProfiles().stream()
            .map(ResourcePackProfile::getId)
            .collect(Collectors.toList());
    }

    public static boolean enable(MinecraftServer server, String packId) {
        ResourcePackManager mgr = server.getDataPackManager();
        mgr.scanPacks();
        List<String> current = listEnabled(server);
        if (current.contains(packId)) return false;
        if (mgr.getProfiles().stream().noneMatch(p -> p.getId().equals(packId))) return false;
        LinkedHashSet<String> newEnabled = new LinkedHashSet<>(current);
        newEnabled.add(packId);
        reload(server, newEnabled);
        return true;
    }

    public static boolean disable(MinecraftServer server, String packId) {
        List<String> current = listEnabled(server);
        if (!current.contains(packId)) return false;
        LinkedHashSet<String> newEnabled = new LinkedHashSet<>(current);
        newEnabled.remove(packId);
        reload(server, newEnabled);
        return true;
    }

    public static void reload(MinecraftServer server) {
        reload(server, new LinkedHashSet<>(listEnabled(server)));
    }

    private static void reload(MinecraftServer server, Collection<String> enabledIds) {
        server.reloadResources(enabledIds).exceptionally(e -> {
            server.execute(() -> server.getPlayerManager().getPlayerList().forEach(p ->
                p.sendMessage(Text.literal("§c[DataManager] Reload failed: " + e.getMessage()))));
            return null;
        });
    }
}
