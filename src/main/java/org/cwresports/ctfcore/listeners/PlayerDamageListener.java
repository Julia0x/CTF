package org.cwresports.ctfcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

/**
 * Enhanced player damage listener with improved spawn protection logic
 * Handles spawn protection removal on attack attempts and better PvP management
 */
public class PlayerDamageListener implements Listener {
    
    private final CTFCore plugin;
    
    public PlayerDamageListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        // Check spawn protection for the victim
        if (plugin.getGameManager().hasSpawnProtection(player)) {
            event.setCancelled(true);
            
            // Notify attacker if it's PvP damage
            if (event instanceof EntityDamageByEntityEvent damageByEntity) {
                if (damageByEntity.getDamager() instanceof Player attacker) {
                    String message = plugin.getConfigManager().getMessage("spawn-protection-active");
                    attacker.sendMessage(message);
                }
            }
            return;
        }
        
        // Check if players are in lobby (prevent damage in lobby)
        if (ctfPlayer.getGame().getState() != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        
        CTFPlayer victimCtfPlayer = plugin.getGameManager().getCTFPlayer(victim);
        CTFPlayer attackerCtfPlayer = plugin.getGameManager().getCTFPlayer(attacker);
        
        if (victimCtfPlayer == null || attackerCtfPlayer == null || 
            !victimCtfPlayer.isInGame() || !attackerCtfPlayer.isInGame()) {
            return;
        }
        
        // Check if they're in the same game
        if (!victimCtfPlayer.getGame().equals(attackerCtfPlayer.getGame())) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent friendly fire (same team damage)
        if (victimCtfPlayer.getTeam() != null && attackerCtfPlayer.getTeam() != null &&
            victimCtfPlayer.getTeam().equals(attackerCtfPlayer.getTeam())) {
            event.setCancelled(true);
            String message = plugin.getConfigManager().getMessage("teammate-damage");
            attacker.sendMessage(message);
            return;
        }
        
        // Check spawn protection for victim
        if (plugin.getGameManager().hasSpawnProtection(victim)) {
            event.setCancelled(true);
            String message = plugin.getConfigManager().getMessage("spawn-protection-active");
            attacker.sendMessage(message);
            return;
        }
        
        // **ENHANCED FEATURE: Remove spawn protection from attacker when they try to attack**
        if (plugin.getGameManager().hasSpawnProtection(attacker)) {
            // Remove spawn protection from attacker immediately
            plugin.getGameManager().removeSpawnProtection(attacker);
            
            // Notify attacker that their protection was removed
            String message = plugin.getConfigManager().getMessage("spawn-protection-removed");
            attacker.sendMessage(message);
            
            // Remove the spawn protection boss bar
            plugin.getMessageManager().removeSpawnProtectionBossBar(attacker);
            
            // Allow the attack to continue (don't cancel the event)
            plugin.getLogger().info("Removed spawn protection from " + attacker.getName() + " for attacking " + victim.getName());
        }
    }
}