package org.cwresports.ctfcore.cosmetics.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.AchievementManager;
import org.cwresports.ctfcore.cosmetics.managers.CosmeticsManager;

/**
 * Handles cosmetic-related events
 */
public class CosmeticsListener implements Listener {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;
    private final AchievementManager achievementManager;

    public CosmeticsListener(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
        this.achievementManager = plugin.getAchievementManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cosmeticsManager.loadPlayerData(player);
        achievementManager.loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cosmeticsManager.unloadPlayerData(player);
        achievementManager.savePlayerData(player);
    }

    /**
     * Handle kill event for cosmetics and achievements
     */
    public void handleKill(Player killer, Player victim) {
        if (killer != null) {
            cosmeticsManager.playKillEffect(killer, victim);
            achievementManager.handleKill(killer);
        }
        if (victim != null) {
            cosmeticsManager.playDeathEffect(victim);
            achievementManager.handleDeath(victim);
        }
    }

    /**
     * Handle flag capture event
     */
    public void handleFlagCapture(Player player) {
        if (player != null) {
            achievementManager.handleFlagCapture(player);
        }
    }

    /**
     * Handle flag return event
     */
    public void handleFlagReturn(Player player) {
        if (player != null) {
            achievementManager.handleFlagReturn(player);
        }
    }

    /**
     * Handle game win event
     */
    public void handleGameWin(Player player) {
        if (player != null) {
            cosmeticsManager.playVictoryCelebration(player);
            achievementManager.handleGameWin(player);
        }
    }

    /**
     * Handle game play event
     */
    public void handleGamePlay(Player player) {
        if (player != null) {
            achievementManager.handleGamePlay(player);
        }
    }
}