package com.arnav.serverowner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerOwnerConfig {
    public ModerationConfig moderation = new ModerationConfig();
    public MuteConfig mute = new MuteConfig();
    public FreezeConfig freeze = new FreezeConfig();
    public VanishConfig vanish = new VanishConfig();
    public DatapackConfig datapack = new DatapackConfig();
    public ModInfoConfig modInfo = new ModInfoConfig();
    public ModrinthConfig modrinth = new ModrinthConfig();
    public GuardianConfig guardian = new GuardianConfig();

    public static class ModerationConfig {
        public boolean enabled = true;
        public int autoKickAfterWarnings = 3;
    }
    public static class MuteConfig       { public boolean enabled = true; }
    public static class FreezeConfig     { public boolean enabled = true; }
    public static class VanishConfig     { public boolean enabled = true; }
    public static class DatapackConfig   { public boolean enabled = true; }
    public static class ModInfoConfig    { public boolean enabled = true; }
    public static class ModrinthConfig   { public boolean enabled = true; }
    public static class GuardianConfig   { public boolean logModerationToWatchlist = true; }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("serverowner.json");
    }

    public static ServerOwnerConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            ServerOwnerConfig defaults = new ServerOwnerConfig();
            save(defaults);
            return defaults;
        }
        try {
            ServerOwnerConfig cfg = GSON.fromJson(Files.readString(path), ServerOwnerConfig.class);
            return cfg != null ? cfg : new ServerOwnerConfig();
        } catch (IOException e) {
            return new ServerOwnerConfig();
        }
    }

    public static void save(ServerOwnerConfig config) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException ignored) {}
    }
}
