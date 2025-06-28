package org.cwresports.ctfcore.cosmetics.models;

/**
 * Enum representing different types of cosmetics
 */
public enum CosmeticType {
    KILL_EFFECT("Kill Effect"),
    DEATH_EFFECT("Death Effect"),
    VICTORY_DANCE("Victory Dance"),
    TRAIL("Trail"),
    FLAG_CARRIER_EFFECT("Flag Carrier Effect"),
    PARTICLE_TRAIL("Particle Trail"),
    BANNER("Banner"),
    TAUNT("Taunt");

    private final String displayName;

    CosmeticType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}