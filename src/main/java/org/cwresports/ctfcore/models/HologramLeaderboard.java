package org.cwresports.ctfcore.models;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.cwresports.ctfcore.CTFCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single hologram leaderboard instance
 */
public class HologramLeaderboard {
    private final String id;
    private final LeaderboardType type;
    private final int size;
    private Location location;
    private boolean enabled;
    private Hologram hologram;

    public HologramLeaderboard(String id, LeaderboardType type, Location location, int size) {
        this.id = id;
        this.type = type;
        this.location = location.clone();
        this.size = size;
        this.enabled = true;
    }

    /**
     * Create the actual hologram in the world
     */
    public void createHologram() {
        if (hologram != null) {
            deleteHologram();
        }

        try {
            hologram = DHAPI.createHologram(id, location);
            updateContent();
            CTFCore.getInstance().getLogger().info("Created hologram leaderboard: " + id);
        } catch (Exception e) {
            CTFCore.getInstance().getLogger().severe("Failed to create hologram " + id + ": " + e.getMessage());
        }
    }

    /**
     * Update hologram content with current leaderboard data
     */
    public void updateContent() {
        if (hologram == null) {
            return;
        }

        try {
            List<LeaderboardEntry> entries = CTFCore.getInstance().getHologramLeaderboardManager().getTopPlayers(type, size);
            List<String> lines = generateHologramLines(entries);
            
            DHAPI.setHologramLines(hologram, lines);
        } catch (Exception e) {
            CTFCore.getInstance().getLogger().warning("Failed to update hologram content for " + id + ": " + e.getMessage());
        }
    }

    /**
     * Move hologram to new location
     */
    public void moveHologram(Location newLocation) {
        this.location = newLocation.clone();
        
        if (hologram != null) {
            try {
                DHAPI.moveHologram(hologram, newLocation);
                CTFCore.getInstance().getLogger().info("Moved hologram leaderboard " + id + " to new location");
            } catch (Exception e) {
                CTFCore.getInstance().getLogger().warning("Failed to move hologram " + id + ": " + e.getMessage());
            }
        }
    }

    /**
     * Delete the hologram from the world
     */
    public void deleteHologram() {
        if (hologram != null) {
            try {
                DHAPI.removeHologram(hologram.getName());
                hologram = null;
                CTFCore.getInstance().getLogger().info("Deleted hologram leaderboard: " + id);
            } catch (Exception e) {
                CTFCore.getInstance().getLogger().warning("Failed to delete hologram " + id + ": " + e.getMessage());
            }
        }
    }

    /**
     * Generate hologram lines from leaderboard entries
     */
    private List<String> generateHologramLines(List<LeaderboardEntry> entries) {
        List<String> lines = new ArrayList<>();

        // Header
        lines.add("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lines.add("§e§l🏆 TOP " + size + " " + type.getDisplayName().toUpperCase() + " 🏆");
        lines.add("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lines.add(""); // Empty line for spacing

        // Entries
        for (LeaderboardEntry entry : entries) {
            String line = String.format("%s#%d §f%s §7- %s%s",
                    entry.getRankColor(),
                    entry.getRank(),
                    entry.getPlayerName(),
                    entry.getValueColor(),
                    entry.getFormattedValue());
            lines.add(line);
        }

        // Fill empty slots if needed
        int remainingSlots = size - entries.size();
        for (int i = 0; i < remainingSlots; i++) {
            int rank = entries.size() + i + 1;
            lines.add(String.format("§7#%d §f--- §7- §70", rank));
        }

        // Footer
        lines.add(""); // Empty line for spacing
        lines.add("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return lines;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public LeaderboardType getType() {
        return type;
    }

    public Location getLocation() {
        return location.clone();
    }

    public int getSize() {
        return size;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isActive() {
        return hologram != null;
    }

    @Override
    public String toString() {
        return String.format("HologramLeaderboard{id=%s, type=%s, size=%d, enabled=%s, active=%s}",
                id, type, size, enabled, isActive());
    }
}