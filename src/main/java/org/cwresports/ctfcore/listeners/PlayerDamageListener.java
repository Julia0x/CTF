package org.cwresports.ctfcore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.GameState;

/**
 * Handles player damage events for spawn protection and CTF rules
 */
public class PlayerDamageListener implements Listener {
    
    private final CTFCore plugin;
    
    public PlayerDamageListener(CTFCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        // Check spawn protection
        if (plugin.getGameManager().hasSpawnProtection(player)) {
            event.setCancelled(true);
            
            // Notify attacker if it's PvP damage
            if (event instanceof EntityDamageByEntityEvent damageByEntity) {
                if (damageByEntity.getDamager() instanceof Player attacker) {
                    attacker.sendMessage("§c⛨ That player has spawn protection!");
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
    
    @EventHandler
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
            attacker.sendMessage("§c⚠ You cannot damage your teammates!");
            return;
        }
        
        // Check spawn protection for victim
        if (plugin.getGameManager().hasSpawnProtection(victim)) {
            event.setCancelled(true);
            attacker.sendMessage("§c⛨ That player has spawn protection!");
            return;
        }
        
        // Check spawn protection for attacker (prevent attacking while protected)
        if (plugin.getGameManager().hasSpawnProtection(attacker)) {
            event.setCancelled(true);
            attacker.sendMessage("§c⛨ You cannot attack while you have spawn protection!");
            return;
        }
    }
}