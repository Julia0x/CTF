package org.cwresports.ctfcore.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.cwresports.ctfcore.CTFCore;

/**
 * Manages currency system using Vault API
 */
public class CurrencyManager {

    private final CTFCore plugin;
    private Economy economy;
    private boolean vaultEnabled;

    public CurrencyManager(CTFCore plugin) {
        this.plugin = plugin;
        this.vaultEnabled = setupEconomy();
    }

    /**
     * Setup Vault economy integration
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Currency features will be disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy plugin found! Currency features will be disabled.");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Vault economy integration enabled with: " + economy.getName());
        return true;
    }

    /**
     * Check if Vault economy is available
     */
    public boolean isEnabled() {
        return vaultEnabled && economy != null;
    }

    /**
     * Get player's balance
     */
    public double getBalance(Player player) {
        if (!isEnabled()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
    /**
     * Get player's coins (alias for getBalance)
     */
    public int getCoins(Player player) {
        return (int) getBalance(player);
    }

    /**
     * Add coins to player's account
     */
    public boolean addCoins(Player player, double amount) {
        if (!isEnabled() || amount <= 0) {
            return false;
        }

        economy.depositPlayer(player, amount);

        // Send notification to player
        player.sendMessage(plugin.getConfigManager().getMessage("currency-earned",
                java.util.Map.of("amount", String.valueOf((int) amount))));

        return true;
    }

    /**
     * Remove coins from player's account
     */
    public boolean removeCoins(Player player, double amount) {
        if (!isEnabled() || amount <= 0) {
            return false;
        }

        if (economy.getBalance(player) < amount) {
            return false; // Insufficient funds
        }

        economy.withdrawPlayer(player, amount);
        return true;
    }

    /**
     * Check if player has enough coins
     */
    public boolean hasEnough(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return economy.getBalance(player) >= amount;
    }

    /**
     * Format currency amount for display
     */
    public String formatAmount(double amount) {
        if (!isEnabled()) {
            return "0";
        }
        return economy.format(amount);
    }

    /**
     * Get currency name (singular)
     */
    public String getCurrencyName() {
        if (!isEnabled()) {
            return "Coin";
        }
        return economy.currencyNameSingular();
    }

    /**
     * Get currency name (plural)
     */
    public String getCurrencyNamePlural() {
        if (!isEnabled()) {
            return "Coins";
        }
        return economy.currencyNamePlural();
    }
}