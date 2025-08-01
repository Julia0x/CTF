package org.cwresports.ctfcore.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;

/**
 * Manages server lobby functionality
 */
public class ServerLobbyManager {
    
    private final CTFCore plugin;
    private Location serverLobbySpawn;
    
    public ServerLobbyManager(CTFCore plugin) {
        this.plugin = plugin;
        loadServerLobby();
    }
    
    /**
     * Load server lobby spawn from configuration
     */
    private void loadServerLobby() {
        String lobbyStr = plugin.getConfigManager().getMainConfig().getString("server.lobby-spawn");
        if (lobbyStr != null && !lobbyStr.equals("null") && !lobbyStr.isEmpty()) {
            this.serverLobbySpawn = parseLocation(lobbyStr);
        }
    }
    
    /**
     * Set server lobby spawn point
     */
    public boolean setServerLobbySpawn(Location location) {
        this.serverLobbySpawn = location;
        
        String locationStr = locationToString(location);
        plugin.getConfigManager().getMainConfig().set("server.lobby-spawn", locationStr);
        plugin.getConfigManager().saveConfig("config.yml");
        
        plugin.getLogger().info("Server lobby spawn set at: " + locationStr);
        return true;
    }
    
    /**
     * Teleport player to server lobby
     */
    public boolean teleportToServerLobby(Player player) {
        if (serverLobbySpawn == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("server-lobby-not-set"));
            return false;
        }
        
        player.teleport(serverLobbySpawn);
        player.sendMessage(plugin.getConfigManager().getMessage("teleported-to-server-lobby"));
        return true;
    }
    
    /**
     * Check if server lobby is configured
     */
    public boolean isServerLobbyConfigured() {
        return serverLobbySpawn != null;
    }
    
    /**
     * Get server lobby spawn location
     */
    public Location getServerLobbySpawn() {
        return serverLobbySpawn;
    }
    
    /**
     * Parse location from string
     */
    private Location parseLocation(String locationStr) {
        if (locationStr == null) return null;
        
        try {
            String[] parts = locationStr.split(",");
            if (parts.length >= 4) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
                float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
                
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null) return null;
                
                return new Location(world, x, y, z, yaw, pitch);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid server lobby location format: " + locationStr);
        }
        
        return null;
    }
    
    /**
     * Convert location to string
     */
    private String locationToString(Location location) {
        if (location == null) return null;
        
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
            location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch());
    }
}