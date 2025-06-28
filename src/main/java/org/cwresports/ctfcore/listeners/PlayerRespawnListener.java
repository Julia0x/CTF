package org.cwresports.ctfcore.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles player respawn events in CTF games with smart spawn selection
 */
public class PlayerRespawnListener implements Listener {
    
    private final CTFCore plugin;
    private final Random random;
    
    public PlayerRespawnListener(CTFCore plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);
        
        if (ctfPlayer == null || !ctfPlayer.isInGame()) {
            return;
        }
        
        CTFGame game = ctfPlayer.getGame();
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        
        // Check if this player is dead and in our respawn countdown system
        // If so, we'll handle respawn manually - just set a safe location
        if (!ctfPlayer.isAlive()) {
            Arena arena = game.getArena();
            // Set respawn location to arena lobby (we'll teleport them properly in our countdown)
            event.setRespawnLocation(arena.getLobbySpawn());
            return;
        }
        
        Arena arena = game.getArena();
        Arena.TeamColor team = ctfPlayer.getTeam();
        
        if (team == null) {
            // No team assigned, respawn at lobby
            event.setRespawnLocation(arena.getLobbySpawn());
            return;
        }
        
        // Get team spawn points and find the best available one
        Arena.Team teamData = arena.getTeam(team);
        Location bestSpawn = findBestSpawnPoint(teamData, game);
        
        if (bestSpawn != null) {
            event.setRespawnLocation(bestSpawn);
        } else {
            // Fallback to lobby if no spawns available
            event.setRespawnLocation(arena.getLobbySpawn());
        }
        
        // Mark player as alive again
        ctfPlayer.respawn();
        
        // Schedule loadout and armor application for next tick (after respawn completes)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Re-apply basic loadout
                plugin.getGameManager().applyBasicLoadoutToPlayer(player);
                
                // Re-apply team colored armor after loadout application
                if (ctfPlayer.getTeam() != null) {
                    applyTeamColoredArmor(player, ctfPlayer.getTeam());
                }
                
                // Re-apply team kill enhancements to sword
                if (game != null && ctfPlayer.getTeam() != null) {
                    plugin.getGameManager().applyTeamKillEnhancements(player, game, ctfPlayer.getTeam());
                }
                
                // Apply spawn protection on respawn
                plugin.getGameManager().applySpawnProtection(player);
            }
        }, 1L);
    }
    
    /**
     * Find the best spawn point that's not occupied by other players
     */
    private Location findBestSpawnPoint(Arena.Team teamData, CTFGame game) {
        Location[] spawnPoints = teamData.getSpawnPoints();
        List<Location> availableSpawns = new ArrayList<>();
        
        // First, collect all valid spawn points
        for (Location spawn : spawnPoints) {
            if (spawn != null) {
                availableSpawns.add(spawn);
            }
        }
        
        if (availableSpawns.isEmpty()) {
            return null;
        }
        
        // Find spawns that aren't occupied by other players
        List<Location> unoccupiedSpawns = new ArrayList<>();
        
        for (Location spawn : availableSpawns) {
            boolean isOccupied = false;
            
            // Check if any other player is near this spawn point
            for (CTFPlayer otherPlayer : game.getPlayers()) {
                Player other = otherPlayer.getPlayer();
                if (other != null && other.isOnline() && other.getWorld().equals(spawn.getWorld())) {
                    double distance = other.getLocation().distance(spawn);
                    if (distance <= 2.0) { // Consider occupied if within 2 blocks
                        isOccupied = true;
                        break;
                    }
                }
            }
            
            if (!isOccupied) {
                unoccupiedSpawns.add(spawn);
            }
        }
        
        // If we have unoccupied spawns, use one randomly
        if (!unoccupiedSpawns.isEmpty()) {
            return unoccupiedSpawns.get(random.nextInt(unoccupiedSpawns.size()));
        }
        
        // If all spawns are occupied, just pick a random one (better than nothing)
        return availableSpawns.get(random.nextInt(availableSpawns.size()));
    }
    
    /**
     * Apply team colored armor to player (copied from GameManager for consistency)
     */
    private void applyTeamColoredArmor(Player player, Arena.TeamColor teamColor) {
        org.bukkit.Color armorColor;
        if (teamColor == Arena.TeamColor.RED) {
            armorColor = org.bukkit.Color.RED;
        } else {
            armorColor = org.bukkit.Color.BLUE;
        }

        // Create dyed leather armor
        org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET);
        org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
        org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS);
        org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS);

        // Dye the armor
        dyeLeatherArmor(helmet, armorColor);
        dyeLeatherArmor(chestplate, armorColor);
        dyeLeatherArmor(leggings, armorColor);
        dyeLeatherArmor(boots, armorColor);

        // Set armor
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }
    
    /**
     * Dye leather armor with specified color
     */
    private void dyeLeatherArmor(org.bukkit.inventory.ItemStack armor, org.bukkit.Color color) {
        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            meta.setColor(color);
            armor.setItemMeta(meta);
        }
    }
}