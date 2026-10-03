package dev.caveslite.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/** Helpers for "&"-style colour codes, which the config files use. */
public final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {}

    public static Component legacy(String s) {
        return LEGACY.deserialize(s);
    }

    public static void send(CommandSender sender, String legacyMessage) {
        sender.sendMessage(legacy(legacyMessage));
    }
}
