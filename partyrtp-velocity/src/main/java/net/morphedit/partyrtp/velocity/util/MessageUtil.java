package net.morphedit.partyrtp.velocity.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {
    private MessageUtil() {}

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacySection();

    public static Component colorize(String text) {
        return SERIALIZER.deserialize(text);
    }

    public static String replace(String text, String placeholder, String value) {
        return text.replace(placeholder, value);
    }
}