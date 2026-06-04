package com.arnav.serverowner.module;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModInfoModule {

    public record ModEntry(String modId, String version, String displayName) {}

    public static List<ModEntry> getLoadedMods() {
        List<ModEntry> result = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            var meta = container.getMetadata();
            result.add(new ModEntry(
                meta.getId(),
                meta.getVersion().getFriendlyString(),
                meta.getName()
            ));
        }
        result.sort(Comparator.comparing(ModEntry::modId));
        return result;
    }

    public static Map<String, String> readServerProperties() {
        Path props = FabricLoader.getInstance().getGameDir().resolve("server.properties");
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.exists(props)) return map;
        try {
            for (String line : Files.readAllLines(props)) {
                if (line.startsWith("#") || line.isBlank()) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException ignored) {}
        return map;
    }

    public static boolean setServerProperty(String key, String value) {
        Path props = FabricLoader.getInstance().getGameDir().resolve("server.properties");
        if (!Files.exists(props)) return false;
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(props));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("#") || line.isBlank()) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                if (line.substring(0, eq).trim().equals(key)) {
                    lines.set(i, key + "=" + value);
                    found = true;
                    break;
                }
            }
            if (!found) lines.add(key + "=" + value);
            Files.writeString(props, String.join(System.lineSeparator(), lines));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
