package org.cwresports.ctfcore.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.cwresports.ctfcore.CTFCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a CTF Arena with all necessary configuration data
 */
public class Arena {
    
    private final String name;
    private boolean enabled;
    private String worldGuardRegion;
    private String worldName;
    private Location lobbySpawn;
    private final Map<TeamColor, Team> teams;
    private final List<Location> powerupSpawnPoints;
    private boolean inSetupMode;
    
    public enum TeamColor {
        RED("red", "&c"),
        BLUE("blue", "&9");
        
        private final String name;
        private final String colorCode;
        
        TeamColor(String name, String colorCode) {
            this.name = name;
            this.colorCode = colorCode;
        }
        
        public String getName() {
            return name;
        }
        
        public String getColorCode() {
            return colorCode;
        }
        
        public String getDisplayName() {
            return colorCode + name.toUpperCase();
        }
        
        public static TeamColor fromString(String name) {
            for (TeamColor team : values()) {
                if (team.name.equalsIgnoreCase(name)) {
                    return team;
                }
            }
            return null;
        }
    }
    
    public static class Team {
        private final Location[] spawnPoints;
        private Location flagLocation;
        private Location capturePoint;
        
        public Team() {
            this.spawnPoints = new Location[4]; // Exactly 4 spawn points per team
        }
        
        public Location[] getSpawnPoints() {
            return spawnPoints;
        }
        
        public void setSpawnPoint(int index, Location location) {
            if (index >= 0 && index < 4) {
                spawnPoints[index] = location;
            }
        }
        
        public Location getSpawnPoint(int index) {
            if (index >= 0 && index < 4) {
                return spawnPoints[index];
            }
            return null;
        }
        
        public boolean hasAllSpawnPoints() {
            for (Location spawn : spawnPoints) {
                if (spawn == null) return false;
            }
            return true;
        }
        
        public int getSpawnPointCount() {
            int count = 0;
            for (Location spawn : spawnPoints) {
                if (spawn != null) count++;
            }
            return count;
        }
        
        public Location getFlagLocation() {
            return flagLocation;
        }
        
        public void setFlagLocation(Location flagLocation) {
            this.flagLocation = flagLocation;
        }
        
        public Location getCapturePoint() {
            return capturePoint;
        }
        
        public void setCapturePoint(Location capturePoint) {
            this.capturePoint = capturePoint;
        }
        
        public boolean isFullyConfigured() {
            return hasAllSpawnPoints() && flagLocation != null && capturePoint != null;
        }
    }
    
    public Arena(String name) {
        this.name = name;
        this.enabled = false;
        this.teams = new HashMap<>();
        this.teams.put(TeamColor.RED, new Team());
        this.teams.put(TeamColor.BLUE, new Team());
        this.powerupSpawnPoints = new ArrayList<>();
        this.inSetupMode = false;
    }
    
    /**
     * Load arena from configuration section
     */
    public static Arena fromConfig(String name, ConfigurationSection section) {
        Arena arena = new Arena(name);
        
        arena.enabled = section.getBoolean("enabled", false);
        arena.worldGuardRegion = section.getString("worldguard_region");
        arena.worldName = section.getString("world_name");
        
        // Load lobby spawn
        String lobbyStr = section.getString("lobby_spawn");
        if (lobbyStr != null && !lobbyStr.isEmpty()) {
            arena.lobbySpawn = parseLocation(lobbyStr, arena.worldName);
        }
        
        // Load teams
        ConfigurationSection teamsSection = section.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (TeamColor teamColor : TeamColor.values()) {
                ConfigurationSection teamSection = teamsSection.getConfigurationSection(teamColor.getName());
                if (teamSection != null) {
                    Team team = arena.teams.get(teamColor);
                    
                    // Load spawn points
                    ConfigurationSection spawnsSection = teamSection.getConfigurationSection("spawn_points");
                    if (spawnsSection != null) {
                        for (int i = 1; i <= 4; i++) {
                            String spawnStr = spawnsSection.getString(String.valueOf(i));
                            if (spawnStr != null && !spawnStr.isEmpty()) {
                                team.setSpawnPoint(i - 1, parseLocation(spawnStr, arena.worldName));
                            }
                        }
                    }
                    
                    // Load flag location
                    String flagStr = teamSection.getString("flag_location");
                    if (flagStr != null && !flagStr.isEmpty()) {
                        team.setFlagLocation(parseLocation(flagStr, arena.worldName));
                    }
                    
                    // Load capture point
                    String captureStr = teamSection.getString("capture_point");
                    if (captureStr != null && !captureStr.isEmpty()) {
                        team.setCapturePoint(parseLocation(captureStr, arena.worldName));
                    }
                }
            }
        }
        
        // Load powerup spawn points
        List<String> powerupSpawns = section.getStringList("powerup_spawn_points");
        for (String spawnStr : powerupSpawns) {
            Location loc = parseLocation(spawnStr, arena.worldName);
            if (loc != null) {
                arena.addPowerupSpawnPoint(loc);
            }
        }

        return arena;
    }
    
    /**
     * Save arena to configuration section
     */
    public void saveToConfig(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("worldguard_region", worldGuardRegion);
        section.set("world_name", worldName);
        section.set("lobby_spawn", lobbySpawn != null ? locationToString(lobbySpawn) : null);
        
        // Save teams
        for (TeamColor teamColor : TeamColor.values()) {
            Team team = teams.get(teamColor);
            String teamPath = "teams." + teamColor.getName();
            
            // Save spawn points
            for (int i = 0; i < 4; i++) {
                Location spawn = team.getSpawnPoint(i);
                section.set(teamPath + ".spawn_points." + (i + 1), 
                    spawn != null ? locationToString(spawn) : null);
            }
            
            // Save flag location
            section.set(teamPath + ".flag_location", 
                team.getFlagLocation() != null ? locationToString(team.getFlagLocation()) : null);
            
            // Save capture point
            section.set(teamPath + ".capture_point", 
                team.getCapturePoint() != null ? locationToString(team.getCapturePoint()) : null);
        }

        // Save powerup spawn points
        List<String> powerupSpawns = new ArrayList<>();
        for (Location loc : powerupSpawnPoints) {
            powerupSpawns.add(locationToString(loc));
        }
        section.set("powerup_spawn_points", powerupSpawns);
    }
    
    /**
     * Check if arena is fully configured and ready to be enabled
     */
    public boolean isFullyConfigured() {
        // Check basic requirements
        if (worldGuardRegion == null || worldName == null || lobbySpawn == null) {
            return false;
        }
        
        // Check if world exists
        World world = CTFCore.getInstance().getServer().getWorld(worldName);
        if (world == null) {
            return false;
        }
        
        // Check teams
        for (Team team : teams.values()) {
            if (!team.isFullyConfigured()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get configuration status for admin display
     */
    public Map<String, Object> getConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("enabled", enabled);
        status.put("world", worldName);
        status.put("region", worldGuardRegion);
        status.put("lobby_complete", lobbySpawn != null);
        
        for (TeamColor teamColor : TeamColor.values()) {
            Team team = teams.get(teamColor);
            String teamKey = teamColor.getName();
            
            status.put(teamKey + "_spawns_count", team.getSpawnPointCount());
            status.put(teamKey + "_spawns_complete", team.hasAllSpawnPoints());
            status.put(teamKey + "_flag_complete", team.getFlagLocation() != null);
            status.put(teamKey + "_capture_complete", team.getCapturePoint() != null);
        }
        
        return status;
    }
    
    // Utility methods for location serialization
    private static Location parseLocation(String locationStr, String worldName) {
        if (locationStr == null || worldName == null) return null;
        
        try {
            String[] parts = locationStr.split(",");
            if (parts.length >= 3) {
                World world = CTFCore.getInstance().getServer().getWorld(worldName);
                if (world == null) return null;
                
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                float yaw = parts.length > 3 ? Float.parseFloat(parts[3]) : 0;
                float pitch = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
                
                return new Location(world, x, y, z, yaw, pitch);
            }
        } catch (NumberFormatException e) {
            CTFCore.getInstance().getLogger().warning("Invalid location format: " + locationStr);
        }
        
        return null;
    }
    
    private static String locationToString(Location location) {
        if (location == null) return null;
        
        return String.format("%.2f,%.2f,%.2f,%.2f,%.2f",
            location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch());
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getWorldGuardRegion() {
        return worldGuardRegion;
    }
    
    public void setWorldGuardRegion(String worldGuardRegion) {
        this.worldGuardRegion = worldGuardRegion;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public Location getLobbySpawn() {
        return lobbySpawn;
    }
    
    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }
    
    public Team getTeam(TeamColor teamColor) {
        return teams.get(teamColor);
    }
    
    public Map<TeamColor, Team> getTeams() {
        return teams;
    }
    
    public boolean isInSetupMode() {
        return inSetupMode;
    }
    
    public void setInSetupMode(boolean inSetupMode) {
        this.inSetupMode = inSetupMode;
    }

    public List<Location> getPowerupSpawnPoints() {
        return powerupSpawnPoints;
    }

    public void addPowerupSpawnPoint(Location location) {
        powerupSpawnPoints.add(location);
    }

    public void clearPowerupSpawnPoints() {
        powerupSpawnPoints.clear();
    }
    
    public World getWorld() {
        return worldName != null ? CTFCore.getInstance().getServer().getWorld(worldName) : null;
    }
}