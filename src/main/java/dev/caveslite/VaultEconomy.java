package dev.caveslite;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * Thin wrapper around Vault's Economy service. Works with whatever economy
 * plugin the server has registered through Vault (CMI, EssentialsX, or any
 * other) - this plugin never talks to CMI or EssentialsX directly. If Vault
 * isn't installed, every call here is a safe no-op.
 */
public final class VaultEconomy {
    private final Plugin plugin;
    private Economy economy;
    private boolean warned;

    public VaultEconomy(Plugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)looks up the Vault economy provider. Safe to call again, e.g. on /dcaves reload. */
    public void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
    }

    public boolean isHooked() {
        return economy != null;
    }

    /** No-ops (and logs once) if Vault or an economy provider isn't available. */
    public void deposit(Player player, double amount) {
        if (amount <= 0) return;
        if (economy == null) {
            if (!warned) {
                plugin.getLogger().log(Level.WARNING,
                        "No economy plugin found behind Vault - money rewards are disabled.");
                warned = true;
            }
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
