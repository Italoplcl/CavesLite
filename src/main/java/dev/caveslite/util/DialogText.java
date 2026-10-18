package dev.caveslite.util;

import net.kyori.adventure.text.Component;

/** Presentation-only palette for administration dialogs, including existing custom files. */
public final class DialogText {
    private DialogText() {}

    public static String palette(String text) {
        if (text == null) return "";
        // Collapse legacy RGB sequences before processing their individual color digits.
        return text.replaceAll("(?i)[&§]x(?:[&§][0-9a-f]){6}", "&f")
                .replaceAll("(?i)[&§]#[0-9a-f]{6}", "&f")
                .replaceAll("(?i)[&§][1-6a-e]", "&f")
                .replaceAll("(?i)[&§]0", "&8")
                .replace('§', '&');
    }

    public static Component component(String text) {
        return Text.legacy(palette(text));
    }

    public static Component status(String name, boolean enabled) {
        // Strip name formatting so an embedded reset cannot undo the inactive marker.
        String plain = name.replaceAll("(?i)[&§]x(?:[&§][0-9a-f]){6}", "")
                .replaceAll("(?i)[&§]#[0-9a-f]{6}", "")
                .replaceAll("(?i)[&§][0-9a-fk-or]", "");
        return component((enabled ? "&f" : "&7&m") + plain);
    }
}
