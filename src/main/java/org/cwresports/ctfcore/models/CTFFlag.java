package org.cwresports.ctfcore.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * Represents a CTF flag with its state and location
 */
public class CTFFlag {

    private final Arena.TeamColor team;
    private final Location baseLocation;
    private Location currentLocation;
    private CTFPlayer carrier;
    private FlagState state;
    private Block flagBlock;

    public enum FlagState {
        AT_BASE,
        CARRIED,
        DROPPED
    }

    public CTFFlag(Arena.TeamColor team, Location baseLocation) {
        this.team = team;
        this.baseLocation = baseLocation.clone();
        this.currentLocation = baseLocation.clone();
        this.state = FlagState.AT_BASE;
    }

    /**
     * Spawn the flag at its base location
     */
    public void spawn() {
        if (baseLocation.getWorld() == null) {
            return;
        }

        flagBlock = baseLocation.getBlock();

        // Set banner material based on team color
        Material bannerMaterial = switch (team) {
            case RED -> Material.RED_BANNER;
            case BLUE -> Material.BLUE_BANNER;
        };

        flagBlock.setType(bannerMaterial);

        // Banner is placed correctly - no additional block data needed for standing banners
        state = FlagState.AT_BASE;
        currentLocation = baseLocation.clone();
    }

    /**
     * Remove the flag from the world (when taken)
     */
    public void remove() {
        if (flagBlock != null && flagBlock.getType().name().contains("BANNER")) {
            flagBlock.setType(Material.AIR);
        }
    }

    /**
     * Set the flag carrier
     */
    public void setCarrier(CTFPlayer carrier) {
        this.carrier = carrier;
        this.state = FlagState.CARRIED;
        remove(); // Remove from world when carried

        if (carrier != null && carrier.getPlayer() != null) {
            currentLocation = carrier.getPlayer().getLocation().clone();
        }
    }

    /**
     * Drop the flag at a specific location
     */
    public void dropAt(Location location) {
        this.carrier = null;
        this.state = FlagState.DROPPED;
        this.currentLocation = location.clone();

        // Place flag block at drop location
        if (location.getWorld() != null) {
            Block dropBlock = location.getBlock();

            Material bannerMaterial = switch (team) {
                case RED -> Material.RED_BANNER;
                case BLUE -> Material.BLUE_BANNER;
            };

            dropBlock.setType(bannerMaterial);
            flagBlock = dropBlock;
        }
    }

    /**
     * Return the flag to its base
     */
    public void returnToBase() {
        this.carrier = null;
        this.state = FlagState.AT_BASE;
        this.currentLocation = baseLocation.clone();

        // Remove from dropped location if applicable
        if (flagBlock != null && state == FlagState.DROPPED) {
            flagBlock.setType(Material.AIR);
        }

        // Respawn at base
        spawn();
    }

    /**
     * Check if flag is at its base location
     */
    public boolean isAtBase() {
        return state == FlagState.AT_BASE;
    }

    /**
     * Check if flag is currently carried
     */
    public boolean isCarried() {
        return state == FlagState.CARRIED && carrier != null;
    }

    /**
     * Check if flag is dropped
     */
    public boolean isDropped() {
        return state == FlagState.DROPPED;
    }

    /**
     * Get distance from a location to the flag
     */
    public double getDistanceFrom(Location location) {
        if (currentLocation == null || location.getWorld() == null ||
                currentLocation.getWorld() == null ||
                !location.getWorld().equals(currentLocation.getWorld())) {
            return Double.MAX_VALUE;
        }

        return location.distance(currentLocation);
    }

    /**
     * Check if a location is near the flag (within interaction distance)
     */
    public boolean isNear(Location location, double distance) {
        return getDistanceFrom(location) <= distance;
    }

    /**
     * Update current location if flag is carried
     */
    public void updateLocation() {
        if (isCarried() && carrier != null && carrier.getPlayer() != null) {
            currentLocation = carrier.getPlayer().getLocation().clone();
        }
    }

    /**
     * Get the flag's display name for messages
     */
    public String getDisplayName() {
        return team.getColorCode() + team.getName().toUpperCase() + " FLAG";
    }

    /**
     * Get a status string for debugging/admin purposes
     */
    public String getStatusString() {
        String stateStr = switch (state) {
            case AT_BASE -> "At Base";
            case CARRIED -> "Carried by " + (carrier != null && carrier.getPlayer() != null ?
                    carrier.getPlayer().getName() : "Unknown");
            case DROPPED -> "Dropped";
        };

        return String.format("%s - %s", getDisplayName(), stateStr);
    }

    // Getters

    public Arena.TeamColor getTeam() {
        return team;
    }

    public Location getBaseLocation() {
        return baseLocation.clone();
    }

    public Location getCurrentLocation() {
        return currentLocation != null ? currentLocation.clone() : null;
    }

    public CTFPlayer getCarrier() {
        return carrier;
    }

    public FlagState getState() {
        return state;
    }

    public Block getFlagBlock() {
        return flagBlock;
    }

    @Override
    public String toString() {
        return String.format("CTFFlag{team=%s, state=%s, location=%s}",
                team.getName(), state,
                currentLocation != null ?
                        String.format("%.1f,%.1f,%.1f", currentLocation.getX(), currentLocation.getY(), currentLocation.getZ()) :
                        "null");
    }
}