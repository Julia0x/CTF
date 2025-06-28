package org.cwresports.ctfcore.models;

import java.util.UUID;

/**
 * Represents a single entry in a leaderboard
 */
public class LeaderboardEntry {
    private final UUID playerId;
    private final String playerName;
    private final Object value;
    private final int rank;

    public LeaderboardEntry(UUID playerId, String playerName, Object value, int rank) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.value = value;
        this.rank = rank;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Object getValue() {
        return value;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Get formatted value for display
     */
    public String getFormattedValue() {
        if (value instanceof Double) {
            return String.format("%.2f", (Double) value);
        }
        return value.toString();
    }

    /**
     * Get rank color based on position
     */
    public String getRankColor() {
        switch (rank) {
            case 1: return "§6"; // Gold
            case 2: return "§e"; // Yellow
            case 3: return "§c"; // Red
            default: return "§7"; // Gray
        }
    }

    /**
     * Get value color (same as rank color for consistency)
     */
    public String getValueColor() {
        return getRankColor();
    }

    @Override
    public String toString() {
        return String.format("LeaderboardEntry{rank=%d, player=%s, value=%s}", 
                rank, playerName, getFormattedValue());
    }
}