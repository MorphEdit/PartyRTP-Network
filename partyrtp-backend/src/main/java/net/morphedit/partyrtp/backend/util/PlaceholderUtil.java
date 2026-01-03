package net.morphedit.partyrtp.backend.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PlaceholderUtil {
    private PlaceholderUtil() {}

    public static String apply(String template, Player player) {
        if (template == null || template.isBlank()) return "";
        if (player == null) return template;

        Location loc = player.getLocation();
        String worldName = "world";
        if (loc.getWorld() != null) {
            worldName = loc.getWorld().getName();
        }

        Map<String, String> vars = Map.of(
                "%player%", player.getName(),
                "%uuid%", player.getUniqueId().toString(),
                "%world%", worldName,
                "%x%", String.valueOf(loc.getBlockX()),
                "%y%", String.valueOf(loc.getBlockY()),
                "%z%", String.valueOf(loc.getBlockZ())
        );

        String out = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }
}