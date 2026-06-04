package com.arnav.serverowner.module;

import com.arnav.serverowner.ServerOwnerMod;
import com.arnav.serverowner.ServerOwnerUtils;
import com.arnav.serverowner.bridge.GuardianBridge;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModerationModule {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("serverowner");

    private static final Map<UUID, List<WarnEntry>> warnings = new HashMap<>();
    private static final Map<UUID, MuteEntry> mutes = new HashMap<>();
    private static final Map<UUID, Vec3d> frozen = new HashMap<>();
    private static final Set<UUID> vanished = new HashSet<>();

    public record WarnEntry(String reason, long timestamp, String by) {}
    public record MuteEntry(String reason, long expiry, String by) {}

    public static void init() {
        loadWarnings();
        loadMutes();

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            UUID uuid = sender.getUuid();
            MuteEntry mute = mutes.get(uuid);
            if (mute == null) return true;
            if (mute.expiry() != 0 && System.currentTimeMillis() > mute.expiry()) {
                mutes.remove(uuid);
                saveMutes();
                return true;
            }
            String remaining = mute.expiry() == 0 ? "permanently" : "until expiry";
            sender.sendMessage(Text.literal("§c[ServerOwner] You are muted " + remaining + ". Reason: " + mute.reason()));
            return false;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (frozen.isEmpty()) return;
            for (var player : server.getPlayerManager().getPlayerList()) {
                Vec3d pos = frozen.get(player.getUuid());
                if (pos == null) continue;
                if (player.getPos().squaredDistanceTo(pos) > 0.01) {
                    player.requestTeleport(pos.x, pos.y, pos.z);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (vanished.contains(player.getUuid())) {
                applyVanishEffect(player);
            }
        });
    }

    // --- Warn ---

    public static int addWarning(ServerPlayerEntity target, String reason, String by) {
        UUID uuid = target.getUuid();
        List<WarnEntry> list = warnings.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.add(new WarnEntry(reason, System.currentTimeMillis(), by));
        saveWarnings();
        PunishmentModule.record(uuid, target.getName().getString(), "warn", reason, by);
        GuardianBridge.logToWatchlist(uuid, "[ServerOwner] warned: " + reason);
        int count = list.size();
        int threshold = ServerOwnerMod.config.moderation.autoKickAfterWarnings;
        if (threshold > 0 && count >= threshold) {
            target.networkHandler.disconnect(Text.literal("§c[ServerOwner] You have been kicked: too many warnings (" + count + ")."));
        }
        return count;
    }

    public static List<WarnEntry> getWarnings(UUID uuid) {
        return warnings.getOrDefault(uuid, List.of());
    }

    public static boolean clearWarnings(UUID uuid) {
        boolean had = warnings.containsKey(uuid);
        warnings.remove(uuid);
        if (had) saveWarnings();
        return had;
    }

    // --- Mute ---

    public static void mute(ServerPlayerEntity target, long durationMs, String reason, String by) {
        UUID uuid = target.getUuid();
        long expiry = durationMs == 0 ? 0 : System.currentTimeMillis() + durationMs;
        mutes.put(uuid, new MuteEntry(reason, expiry, by));
        saveMutes();
        PunishmentModule.record(uuid, target.getName().getString(), "mute", reason + " (" + ServerOwnerUtils.formatDuration(durationMs) + ")", by);
        GuardianBridge.logToWatchlist(uuid, "[ServerOwner] muted: " + reason);
        target.sendMessage(Text.literal("§c[ServerOwner] You have been muted. Reason: " + reason));
    }

    public static boolean unmute(UUID uuid) {
        boolean had = mutes.containsKey(uuid);
        mutes.remove(uuid);
        if (had) saveMutes();
        return had;
    }

    public static boolean isMuted(UUID uuid) {
        MuteEntry m = mutes.get(uuid);
        if (m == null) return false;
        if (m.expiry() != 0 && System.currentTimeMillis() > m.expiry()) {
            mutes.remove(uuid);
            saveMutes();
            return false;
        }
        return true;
    }

    public static MuteEntry getMute(UUID uuid) {
        return mutes.get(uuid);
    }

    // --- Freeze ---

    public static boolean freeze(ServerPlayerEntity target) {
        if (frozen.containsKey(target.getUuid())) return false;
        frozen.put(target.getUuid(), target.getPos());
        target.sendMessage(Text.literal("§c[ServerOwner] You have been frozen."));
        return true;
    }

    public static boolean unfreeze(UUID uuid) {
        return frozen.remove(uuid) != null;
    }

    public static boolean isFrozen(UUID uuid) {
        return frozen.containsKey(uuid);
    }

    // --- Vanish ---

    public static boolean toggleVanish(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (vanished.contains(uuid)) {
            vanished.remove(uuid);
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
            return false;
        } else {
            vanished.add(uuid);
            applyVanishEffect(player);
            return true;
        }
    }

    private static void applyVanishEffect(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
    }

    // --- Persistence ---

    private static void loadWarnings() {
        Path file = DATA_DIR.resolve("warnings.json");
        if (!Files.exists(file)) return;
        try {
            JsonObject obj = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (obj == null) return;
            for (var entry : obj.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    List<WarnEntry> list = new ArrayList<>();
                    for (var el : entry.getValue().getAsJsonArray()) {
                        JsonObject w = el.getAsJsonObject();
                        list.add(new WarnEntry(
                            w.get("reason").getAsString(),
                            w.get("timestamp").getAsLong(),
                            w.get("by").getAsString()
                        ));
                    }
                    warnings.put(uuid, list);
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private static void saveWarnings() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            for (var entry : warnings.entrySet()) {
                JsonArray arr = new JsonArray();
                for (WarnEntry w : entry.getValue()) {
                    JsonObject wo = new JsonObject();
                    wo.addProperty("reason", w.reason());
                    wo.addProperty("timestamp", w.timestamp());
                    wo.addProperty("by", w.by());
                    arr.add(wo);
                }
                obj.add(entry.getKey().toString(), arr);
            }
            Files.writeString(DATA_DIR.resolve("warnings.json"), GSON.toJson(obj));
        } catch (IOException ignored) {}
    }

    private static void loadMutes() {
        Path file = DATA_DIR.resolve("mutes.json");
        if (!Files.exists(file)) return;
        try {
            JsonObject obj = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (obj == null) return;
            for (var entry : obj.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    JsonObject m = entry.getValue().getAsJsonObject();
                    MuteEntry mute = new MuteEntry(
                        m.get("reason").getAsString(),
                        m.get("expiry").getAsLong(),
                        m.get("by").getAsString()
                    );
                    if (mute.expiry() == 0 || mute.expiry() > System.currentTimeMillis()) {
                        mutes.put(uuid, mute);
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private static void saveMutes() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            for (var entry : mutes.entrySet()) {
                JsonObject mo = new JsonObject();
                mo.addProperty("reason", entry.getValue().reason());
                mo.addProperty("expiry", entry.getValue().expiry());
                mo.addProperty("by", entry.getValue().by());
                obj.add(entry.getKey().toString(), mo);
            }
            Files.writeString(DATA_DIR.resolve("mutes.json"), GSON.toJson(obj));
        } catch (IOException ignored) {}
    }
}
