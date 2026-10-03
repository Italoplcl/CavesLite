package dev.caveslite.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class Materials {
    public static final List<Material> HELMETS;
    public static final List<Material> CHESTPLATES;
    public static final List<Material> LEGGINGS;
    public static final List<Material> BOOTS;

    static {
        List<Material> helmets = new ArrayList<>();
        List<Material> chestplates = new ArrayList<>();
        List<Material> leggings = new ArrayList<>();
        List<Material> boots = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isLegacy() || !mat.isItem()) continue;
            String name = mat.name();
            if (name.endsWith("_HELMET")) helmets.add(mat);
            else if (name.endsWith("_CHESTPLATE")) chestplates.add(mat);
            else if (name.endsWith("_LEGGINGS")) leggings.add(mat);
            else if (name.endsWith("_BOOTS")) boots.add(mat);
        }
        helmets.add(Material.CARVED_PUMPKIN);
        HELMETS = Collections.unmodifiableList(helmets);
        CHESTPLATES = Collections.unmodifiableList(chestplates);
        LEGGINGS = Collections.unmodifiableList(leggings);
        BOOTS = Collections.unmodifiableList(boots);
    }

    // Written as names so a renamed/removed block can't break compilation.
    private static final Set<Material> CAVE = getSet(List.of(
            "ANDESITE", "DIORITE", "GRANITE", "CALCITE", "TUFF", "DRIPSTONE_BLOCK",
            "STONE", "BONE_BLOCK", "OBSIDIAN", "BEDROCK", "SMOOTH_BASALT",
            "DIAMOND_ORE", "EMERALD_ORE", "IRON_ORE", "GOLD_ORE",
            "LAPIS_ORE", "REDSTONE_ORE", "COAL_ORE", "COPPER_ORE",
            "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE", "DEEPSLATE_IRON_ORE", "DEEPSLATE_GOLD_ORE",
            "DEEPSLATE_LAPIS_ORE", "DEEPSLATE_REDSTONE_ORE", "DEEPSLATE_COAL_ORE", "DEEPSLATE_COPPER_ORE",
            "COBBLESTONE", "MOSSY_COBBLESTONE",
            "DIRT", "GRAVEL", "MOSS_BLOCK", "ROOTED_DIRT",
            "SOUL_SAND", "NETHERRACK", "GLOWSTONE",
            "TORCH", "CHEST", "OAK_PLANKS", "RAIL",
            "SPAWNER", "END_STONE", "NETHER_QUARTZ_ORE",
            "SOUL_SOIL", "BLACKSTONE", "BASALT",
            "NETHER_GOLD_ORE", "GILDED_BLACKSTONE",
            "DEEPSLATE"
    ));

    private Materials() {}

    public static boolean isCave(Material type) {
        return CAVE.contains(type);
    }

    /** Materials by config name; unknown names are skipped. */
    public static Set<Material> getSet(Collection<String> names) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material type = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
            if (type != null) materials.add(type);
        }
        return materials;
    }

    public static void rotate(Block block, BlockFace face) {
        BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(face);
            block.setBlockData(data, false);
        }
    }

    public static ItemStack coloredLeather(Material leatherPiece, int r, int g, int b) {
        ItemStack item = new ItemStack(leatherPiece);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(r, g, b));
        item.setItemMeta(meta);
        return item;
    }

    /** Player head from a minecraft-heads.com "Value" (base64 texture). */
    public static ItemStack head(String textureValue) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(
                UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8)), null);
        profile.setProperty(new ProfileProperty("textures", textureValue));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }
}
