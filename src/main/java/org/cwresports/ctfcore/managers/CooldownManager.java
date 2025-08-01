package org.cwresports.ctfcore.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public void setCooldown(Player player, String key, long seconds) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            cooldowns.put(player.getUniqueId(), new HashMap<>());
        }
        cooldowns.get(player.getUniqueId()).put(key, System.currentTimeMillis() + (seconds * 1000));
    }

    public boolean isOnCooldown(Player player, String key) {
        if (!cooldowns.containsKey(player.getUniqueId()) || !cooldowns.get(player.getUniqueId()).containsKey(key)) {
            return false;
        }
        return System.currentTimeMillis() < cooldowns.get(player.getUniqueId()).get(key);
    }

    public long getRemainingCooldown(Player player, String key) {
        if (!isOnCooldown(player, key)) {
            return 0;
        }
        return (cooldowns.get(player.getUniqueId()).get(key) - System.currentTimeMillis()) / 1000;
    }
}
