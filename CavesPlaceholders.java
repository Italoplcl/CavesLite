package dev.caveslite;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

/**
 * Placeholders for leaderboard/top plugins. Only registered if
 * PlaceholderAPI is actually installed (see CavesLite.onEnable).
 *
 * %dangerouscaves_kills% - total custom mob kills
 * %dangerouscaves_kills_<mob>% - kills of one specific mob (0 if that mob isn't tracked in config)
 * %dangerouscaves_next_threshold% - kills needed for the next achievement, or "-" if maxed
 * %dangerouscaves_last_achievement% - label of the highest achievement reached, or empty
 * %dangerouscaves_top_kills_<rank>% - total kills of whoever is #<rank> overall
 * %dangerouscaves_top_kills_<rank>_name% - name of whoever is #<rank> overall
 * %dangerouscaves_top_kills_<mob>_<rank>% - kills of whoever is #<rank> for that mob
 * %dangerouscaves_top_kills_<mob>_<rank>_name% - name of whoever is #<rank> for that mob
 */
public final class CavesPlaceholders extends PlaceholderExpansion {
    private final CavesLite plugin;
    private final MobAchievements achievements;
    private final KillLeaderboard leaderboard;

    public CavesPlaceholders(CavesLite plugin, MobAchievements achievements, KillLeaderboard leaderboard) {
        this.plugin = plugin;
        this.achievements = achievements;
        this.leaderboard = leaderboard;
    }

    @Override
    public String getIdentifier() {
        return "dangerouscaves";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.startsWith("top_kills_")) {
            return topKillsPlaceholder(lower.substring("top_kills_".length()));
        }

        if (player == null) return "";
        int kills = achievements.getKills(player);

        if (lower.startsWith("kills_")) {
            return String.valueOf(leaderboard.getMobKills(player, lower.substring("kills_".length())));
        }

        return switch (lower) {
            case "kills" -> String.valueOf(kills);
            case "next_threshold" -> {
                Integer next = achievements.nextThreshold(kills);
                yield next == null ? "-" : String.valueOf(next);
            }
            case "last_achievement" -> {
                String label = achievements.lastAchievementLabel(kills);
                yield label == null ? "" : label;
            }
            default -> null;
        };
    }

    /** rest is "<rank>", "<rank>_name", "<mob>_<rank>" or "<mob>_<rank>_name". */
    private String topKillsPlaceholder(String rest) {
        String[] parts = rest.split("_");
        boolean wantsName = parts.length > 0 && parts[parts.length - 1].equals("name");
        int rankIndex = wantsName ? parts.length - 2 : parts.length - 1;
        if (rankIndex < 0) return null;

        Integer rank = toInt(parts[rankIndex]);
        if (rank == null) return null;

        String category = rankIndex == 0 ? "total" : String.join("_", java.util.Arrays.copyOfRange(parts, 0, rankIndex));
        if (wantsName) {
            String name = leaderboard.nameAt(category, rank);
            return name == null ? "" : name;
        }
        Integer amount = leaderboard.amountAt(category, rank);
        return amount == null ? "" : String.valueOf(amount);
    }

    private Integer toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
