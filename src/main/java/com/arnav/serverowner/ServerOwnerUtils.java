package com.arnav.serverowner;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.concurrent.TimeUnit;

public final class ServerOwnerUtils {
    private ServerOwnerUtils() {}

    public static void notifyOps(MinecraftServer server, String message) {
        server.getPlayerManager().getPlayerList().stream()
            .filter(p -> p.hasPermissionLevel(2))
            .forEach(p -> p.sendMessage(Text.literal(message)));
    }

    public static long parseDurationMs(String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent")) {
            return 0;
        }
        char unit = Character.toLowerCase(input.charAt(input.length() - 1));
        try {
            long amount = Long.parseLong(input.substring(0, input.length() - 1));
            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(amount);
                case 'm' -> TimeUnit.MINUTES.toMillis(amount);
                case 'h' -> TimeUnit.HOURS.toMillis(amount);
                case 'd' -> TimeUnit.DAYS.toMillis(amount);
                default  -> 0;
            };
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatDuration(long ms) {
        if (ms == 0) return "permanent";
        long secs = ms / 1000;
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        if (mins < 60) return mins + "m";
        long hours = mins / 60;
        if (hours < 24) return hours + "h";
        return (hours / 24) + "d";
    }
}
