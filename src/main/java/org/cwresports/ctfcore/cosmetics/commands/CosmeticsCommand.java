package org.cwresports.ctfcore.cosmetics.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.cosmetics.managers.AchievementManager;
import org.cwresports.ctfcore.cosmetics.managers.CosmeticsManager;

import java.util.*;

/**
 * Handles cosmetic-related commands
 */
public class CosmeticsCommand implements CommandExecutor {

    private final CTFCore plugin;
    private final CosmeticsManager cosmeticsManager;
    private final AchievementManager achievementManager;

    public CosmeticsCommand(CTFCore plugin) {
        this.plugin = plugin;
        this.cosmeticsManager = plugin.getCosmeticsManager();
        this.achievementManager = plugin.getAchievementManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open cosmetics GUI
            player.sendMessage("§eOpening cosmetics menu...");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "give":
                return handleGive(player, args);
            case "stats":
                return handleStats(player, args);
            case "list":
                return handleList(player, args);
            default:
                player.sendMessage("§cUnknown cosmetics command!");
                return true;
        }
    }

    private boolean handleGive(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /cosmetics give <cosmetic>");
            return true;
        }

        String cosmeticId = args[1];
        boolean success = cosmeticsManager.giveCosmetic(player, cosmeticId);

        if (success) {
            player.sendMessage("§aSuccessfully given cosmetic: " + cosmeticId);
        } else {
            player.sendMessage("§cFailed to give cosmetic: " + cosmeticId);
        }

        return true;
    }

    private boolean handleStats(Player player, String[] args) {
        Set<String> ownedCosmetics = cosmeticsManager.getOwnedCosmetics(player);
        Collection<?> allCosmetics = cosmeticsManager.getAllCosmetics();
        double completionPercentage = achievementManager.getCompletionPercentage(player);
        Set<String> unlockedAchievements = achievementManager.getUnlockedAchievements(player);
        Collection<?> allAchievements = achievementManager.getAllAchievements();

        player.sendMessage("§e=== Cosmetics Stats ===");
        player.sendMessage("§7Cosmetics: §e" + ownedCosmetics.size() + "/" + allCosmetics.size());
        player.sendMessage("§7Achievement Progress: §e" + String.format("%.1f", completionPercentage) + "%");
        player.sendMessage("§7Achievements: §e" + unlockedAchievements.size() + "/" + allAchievements.size());

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        player.sendMessage("§e=== Available Cosmetics ===");
        
        cosmeticsManager.getAllCosmetics().forEach(cosmetic -> {
            boolean owned = cosmeticsManager.ownsCosmetic(player, cosmetic.getId());
            String status = owned ? "§a✅" : "§c❌";
            player.sendMessage(status + " §e" + cosmetic.getName() + " §7(" + cosmetic.getType().getDisplayName() + ")");
        });

        return true;
    }
}