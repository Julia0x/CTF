package org.cwresports.ctfcore.models;

/**
 * Enum representing different types of leaderboards
 */
public enum LeaderboardType {
    KILLS("Top Kills", "total_kills"),
    CAPTURES("Top Captures", "total_captures"),
    LEVEL("Top Levels", "level"),
    GAMES_WON("Top Games Won", "games_won");

    private final String displayName;
    private final String dataField;

    LeaderboardType(String displayName, String dataField) {
        this.displayName = displayName;
        this.dataField = dataField;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDataField() {
        return dataField;
    }

    public static LeaderboardType fromString(String type) {
        if (type == null) return null;
        
        try {
            return LeaderboardType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}