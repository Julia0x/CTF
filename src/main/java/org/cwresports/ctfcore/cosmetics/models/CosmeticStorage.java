package org.cwresports.ctfcore.cosmetics.models;

import java.util.*;

/**
 * Storage class for cosmetic data
 */
public class CosmeticStorage {
    
    private final Map<String, Cosmetic> cosmetics;
    private final Map<UUID, Set<String>> playerOwnedCosmetics;
    private final Map<UUID, Map<CosmeticType, String>> playerEquippedCosmetics;

    public CosmeticStorage() {
        this.cosmetics = new HashMap<>();
        this.playerOwnedCosmetics = new HashMap<>();
        this.playerEquippedCosmetics = new HashMap<>();
        initializeDefaultCosmetics();
    }

    private void initializeDefaultCosmetics() {
        // Add some default cosmetics
        cosmetics.put("default_kill", new Cosmetic("default_kill", "Default Kill Effect", "Basic kill particles", CosmeticType.KILL_EFFECT, 0, "Common"));
        cosmetics.put("fire_kill", new Cosmetic("fire_kill", "Fire Kill Effect", "Fiery kill particles", CosmeticType.KILL_EFFECT, 100, "Rare"));
        cosmetics.put("default_death", new Cosmetic("default_death", "Default Death Effect", "Basic death particles", CosmeticType.DEATH_EFFECT, 0, "Common"));
        cosmetics.put("explosion_death", new Cosmetic("explosion_death", "Explosion Death Effect", "Explosive death particles", CosmeticType.DEATH_EFFECT, 150, "Epic"));
        cosmetics.put("default_victory", new Cosmetic("default_victory", "Default Victory Dance", "Basic victory celebration", CosmeticType.VICTORY_DANCE, 0, "Common"));
        cosmetics.put("firework_victory", new Cosmetic("firework_victory", "Firework Victory", "Firework celebration", CosmeticType.VICTORY_DANCE, 200, "Epic"));
    }

    public Map<String, Cosmetic> getCosmetics() {
        return cosmetics;
    }

    public Map<UUID, Set<String>> getPlayerOwnedCosmetics() {
        return playerOwnedCosmetics;
    }

    public Map<UUID, Map<CosmeticType, String>> getPlayerEquippedCosmetics() {
        return playerEquippedCosmetics;
    }

    public void giveCosmetic(UUID playerId, String cosmeticId) {
        playerOwnedCosmetics.computeIfAbsent(playerId, k -> new HashSet<>()).add(cosmeticId);
    }

    public boolean ownsCosmetic(UUID playerId, String cosmeticId) {
        return playerOwnedCosmetics.getOrDefault(playerId, Collections.emptySet()).contains(cosmeticId);
    }

    public void equipCosmetic(UUID playerId, String cosmeticId) {
        Cosmetic cosmetic = cosmetics.get(cosmeticId);
        if (cosmetic != null && ownsCosmetic(playerId, cosmeticId)) {
            playerEquippedCosmetics.computeIfAbsent(playerId, k -> new HashMap<>()).put(cosmetic.getType(), cosmeticId);
        }
    }

    public void unequipCosmetic(UUID playerId, CosmeticType type) {
        Map<CosmeticType, String> equipped = playerEquippedCosmetics.get(playerId);
        if (equipped != null) {
            equipped.remove(type);
        }
    }

    public String getEquippedCosmetic(UUID playerId, CosmeticType type) {
        Map<CosmeticType, String> equipped = playerEquippedCosmetics.get(playerId);
        return equipped != null ? equipped.get(type) : null;
    }

    public Set<String> getOwnedCosmetics(UUID playerId) {
        return playerOwnedCosmetics.getOrDefault(playerId, Collections.emptySet());
    }
}