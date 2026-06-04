package com.arnav.serverowner.module;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PunishmentModule {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("serverowner");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public record PunishmentEntry(String type, String reason, String by, long timestamp) {}

    private static final Map<UUID, List<PunishmentEntry>> history = new HashMap<>();

    public static void init() {
        loadHistory();
    }

    public static void record(UUID uuid, String playerName, String type, String reason, String by) {
        List<PunishmentEntry> list = history.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.add(new PunishmentEntry(type, reason, by, System.currentTimeMillis()));
        saveHistory();
    }

    public static List<PunishmentEntry> getHistory(UUID uuid) {
        return history.getOrDefault(uuid, List.of());
    }

    public static String formatEntry(PunishmentEntry e) {
        return "[" + FMT.format(Instant.ofEpochMilli(e.timestamp())) + "] §e" + e.type().toUpperCase()
            + " §7by §f" + e.by() + " §7— §f" + e.reason();
    }

    private static void loadHistory() {
        Path file = DATA_DIR.resolve("history.json");
        if (!Files.exists(file)) return;
        try {
            JsonObject obj = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (obj == null) return;
            for (var entry : obj.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    List<PunishmentEntry> list = new ArrayList<>();
                    for (var el : entry.getValue().getAsJsonArray()) {
                        JsonObject p = el.getAsJsonObject();
                        list.add(new PunishmentEntry(
                            p.get("type").getAsString(),
                            p.get("reason").getAsString(),
                            p.get("by").getAsString(),
                            p.get("timestamp").getAsLong()
                        ));
                    }
                    history.put(uuid, list);
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private static void saveHistory() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            for (var entry : history.entrySet()) {
                JsonArray arr = new JsonArray();
                for (PunishmentEntry p : entry.getValue()) {
                    JsonObject po = new JsonObject();
                    po.addProperty("type", p.type());
                    po.addProperty("reason", p.reason());
                    po.addProperty("by", p.by());
                    po.addProperty("timestamp", p.timestamp());
                    arr.add(po);
                }
                obj.add(entry.getKey().toString(), arr);
            }
            Files.writeString(DATA_DIR.resolve("history.json"), GSON.toJson(obj));
        } catch (IOException ignored) {}
    }
}
