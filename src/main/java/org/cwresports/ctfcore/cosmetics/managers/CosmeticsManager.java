package org.cwresports.ctfcore.cosmetics.managers;

import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.effects.ParticleEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cosmetic effects and rewards for CTF players
 */
public class CosmeticsManager {

    private final CTFCore plugin;
    private final ParticleEffect particleEffect;
    private final Map<UUID, String> playerEffects;

    public CosmeticsManager(CTFCore plugin) {
        this.plugin = plugin;
        this.particleEffect = new ParticleEffect(plugin);
        this.playerEffects = new HashMap<>();
    }

    /**
     * Play victory celebration for a player
     */
    public void playVictoryCelebration(Player player) {
        String effectType = playerEffects.getOrDefault(player.getUniqueId(), "default");
        particleEffect.playVictoryCelebration(player, effectType);
    }

    /**
     * Play death effect for a player
     */
    public void playDeathEffect(Player player) {
        String effectType = playerEffects.getOrDefault(player.getUniqueId(), "default");
        particleEffect.playDeathEffect(player, effectType);
    }

    /**
     * Play kill effect
     */
    public void playKillEffect(Player killer, Player victim) {
        String effectType = playerEffects.getOrDefault(killer.getUniqueId(), "default");
        particleEffect.playKillEffect(killer, victim, effectType);
    }

    /**
     * Play flag carrier effect
     */
    public void playFlagCarrierEffect(Player player) {
        String effectType = playerEffects.getOrDefault(player.getUniqueId(), "default");
        particleEffect.playFlagCarrierEffect(player, effectType);
    }

    /**
     * Start trail effect
     */
    public void startTrailEffect(Player player) {
        String effectType = playerEffects.getOrDefault(player.getUniqueId(), "default");
        particleEffect.startTrailEffect(player, effectType);
    }

    /**
     * Set cosmetic effect for a player
     */
    public void setPlayerEffect(Player player, String effectType) {
        playerEffects.put(player.getUniqueId(), effectType);
    }

    /**
     * Remove player effects on quit
     */
    public void removePlayerEffects(Player player) {
        playerEffects.remove(player.getUniqueId());
    }
}