package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.effects.ParticleEffect;
import org.cwresports.ctfcore.cosmetics.models.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages cosmetic effects and rewards for CTF players
 */
public class CosmeticsManager {

    private final CTFCore plugin;
    private final ParticleEffect particleEffect;
    private final CosmeticStorage storage;

    public CosmeticsManager(CTFCore plugin) {
        this.plugin = plugin;
        this.particleEffect = new ParticleEffect(plugin);
        this.storage = new CosmeticStorage();
    }

    /**
     * Get the cosmetic storage
     */
    public CosmeticStorage getStorage() {
        return storage;
    }

    /**
     * Give a cosmetic to a player
     */
    public void giveCosmetic(Player player, String cosmeticId) {
        if (player != null) {
            storage.giveCosmetic(player.getUniqueId(), cosmeticId);
        }
    }

    /**
     * Get all available cosmetics
     */
    public Collection<Cosmetic> getAllCosmetics() {
        return storage.getCosmetics().values();
    }

    /**
     * Get a specific cosmetic by ID
     */
    public Cosmetic getCosmetic(String cosmeticId) {
        return storage.getCosmetics().get(cosmeticId);
    }

    /**
     * Check if player owns a cosmetic
     */
    public boolean ownsCosmetic(Player player, String cosmeticId) {
        return player != null && storage.ownsCosmetic(player.getUniqueId(), cosmeticId);
    }

    /**
     * Get cosmetics by type
     */
    public List<Cosmetic> getCosmeticsByType(CosmeticType type) {
        return storage.getCosmetics().values().stream()
                .filter(cosmetic -> cosmetic.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Load player cosmetic data
     */
    public void loadPlayerData(Player player) {
        if (player != null) {
            // Initialize player with default cosmetics if they don't have any
            UUID playerId = player.getUniqueId();
            if (storage.getOwnedCosmetics(playerId).isEmpty()) {
                // Give default cosmetics
                storage.giveCosmetic(playerId, "default_kill");
                storage.giveCosmetic(playerId, "default_death");
                storage.giveCosmetic(playerId, "default_victory");
            }
        }
    }

    /**
     * Unload player cosmetic data
     */
    public void unloadPlayerData(Player player) {
        // Currently no action needed - data is persistent
    }

    /**
     * Get player's owned cosmetics
     */
    public Set<String> getOwnedCosmetics(Player player) {
        if (player != null) {
            return storage.getOwnedCosmetics(player.getUniqueId());
        }
        return Collections.emptySet();
    }

    /**
     * Get player's equipped cosmetic for a type
     */
    public String getEquippedCosmetic(Player player, CosmeticType type) {
        if (player != null) {
            return storage.getEquippedCosmetic(player.getUniqueId(), type);
        }
        return null;
    }

    /**
     * Unequip a cosmetic type for player
     */
    public void unequipCosmetic(Player player, CosmeticType type) {
        if (player != null) {
            storage.unequipCosmetic(player.getUniqueId(), type);
        }
    }

    /**
     * Equip a cosmetic for player
     */
    public void equipCosmetic(Player player, String cosmeticId) {
        if (player != null) {
            storage.equipCosmetic(player.getUniqueId(), cosmeticId);
        }
    }

    /**
     * Play victory celebration for a player
     */
    public void playVictoryCelebration(Player player) {
        String effectType = getEquippedCosmetic(player, CosmeticType.VICTORY_DANCE);
        if (effectType == null) effectType = "default";
        particleEffect.playVictoryCelebration(player, effectType);
    }

    /**
     * Play death effect for a player
     */
    public void playDeathEffect(Player player) {
        String effectType = getEquippedCosmetic(player, CosmeticType.DEATH_EFFECT);
        if (effectType == null) effectType = "default";
        particleEffect.playDeathEffect(player, effectType);
    }

    /**
     * Play kill effect
     */
    public void playKillEffect(Player killer, Player victim) {
        String effectType = getEquippedCosmetic(killer, CosmeticType.KILL_EFFECT);
        if (effectType == null) effectType = "default";
        particleEffect.playKillEffect(killer, victim, effectType);
    }

    /**
     * Play flag carrier effect
     */
    public void playFlagCarrierEffect(Player player) {
        String effectType = getEquippedCosmetic(player, CosmeticType.FLAG_CARRIER_EFFECT);
        if (effectType == null) effectType = "default";
        particleEffect.playFlagCarrierEffect(player, effectType);
    }

    /**
     * Start trail effect
     */
    public void startTrailEffect(Player player) {
        String effectType = getEquippedCosmetic(player, CosmeticType.TRAIL);
        if (effectType == null) effectType = "default";
        particleEffect.startTrailEffect(player, effectType);
    }

    /**
     * Remove player effects on quit
     */
    public void removePlayerEffects(Player player) {
        // Clean up any active effects
        unloadPlayerData(player);
    }
}