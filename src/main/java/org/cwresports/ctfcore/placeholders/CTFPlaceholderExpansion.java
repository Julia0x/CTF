package org.cwresports.ctfcore.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.cwresports.ctfcore.CTFCore;
import org.cwresports.ctfcore.models.CTFPlayer;
import org.cwresports.ctfcore.models.CTFGame;

import java.util.Map;

/**
 * PlaceholderAPI expansion for CTF-Core
 * Provides placeholders for other plugins to use
 */
public class CTFPlaceholderExpansion extends PlaceholderExpansion {

    private final CTFCore plugin;

    public CTFPlaceholderExpansion(CTFCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ctf";
    }

    @Override
    public String getAuthor() {
        return "CWReSports";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        CTFPlayer ctfPlayer = plugin.getGameManager().getCTFPlayer(player);

        // If player is not in CTF system, load their data
        if (ctfPlayer == null) {
            Map<String, Object> playerData = plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId());
            ctfPlayer = new CTFPlayer(player, playerData);
        }

        switch (params.toLowerCase()) {
            // Level system placeholders
            case "level":
                return String.valueOf(ctfPlayer.getLevel());
            case "experience":
            case "xp":
                return String.valueOf(ctfPlayer.getExperience());
            case "xp_for_next_level":
            case "xp_required":
                return String.valueOf(ctfPlayer.getXPForNextLevel());
            case "xp_progress":
                return String.format("%.1f", ctfPlayer.getXPProgress() * 100);

            // Session statistics
            case "session_kills":
            case "kills":
                return String.valueOf(ctfPlayer.getKills());
            case "session_deaths":
            case "deaths":
                return String.valueOf(ctfPlayer.getDeaths());
            case "session_captures":
            case "captures":
                return String.valueOf(ctfPlayer.getCaptures());
            case "session_returns":
            case "returns":
                return String.valueOf(ctfPlayer.getFlagReturns());
            case "session_kd":
                return String.format("%.2f", ctfPlayer.getKDRatio());

            // Total statistics
            case "total_kills":
                return String.valueOf(ctfPlayer.getTotalKills());
            case "total_deaths":
                return String.valueOf(ctfPlayer.getTotalDeaths());
            case "total_captures":
                return String.valueOf(ctfPlayer.getTotalCaptures());
            case "total_returns":
                return String.valueOf(ctfPlayer.getTotalFlagReturns());
            case "total_kd":
                return String.format("%.2f", ctfPlayer.getKDRatio());
            case "games_played":
                return String.valueOf(ctfPlayer.getGamesPlayed());
            case "games_won":
                return String.valueOf(ctfPlayer.getGamesWon());
            case "win_rate":
                return String.format("%.1f", ctfPlayer.getWinRate() * 100);

            // Game status
            case "in_game":
                return ctfPlayer.isInGame() ? "true" : "false";
            case "team":
                return ctfPlayer.getTeam() != null ? ctfPlayer.getTeam().getName() : "none";
            case "team_color":
                return ctfPlayer.getTeam() != null ? ctfPlayer.getTeam().getColorCode() : "§f";
            case "has_flag":
                return ctfPlayer.hasFlag() ? "true" : "false";
            case "carrying_flag":
                return ctfPlayer.hasFlag() ? ctfPlayer.getCarryingFlag().getTeam().getName() : "none";

            // Arena information
            case "arena":
                if (ctfPlayer.isInGame()) {
                    return ctfPlayer.getGame().getArena().getName();
                }
                return "none";
            case "arena_state":
                if (ctfPlayer.isInGame()) {
                    return ctfPlayer.getGame().getState().toString().toLowerCase();
                }
                return "none";
            case "arena_players":
                if (ctfPlayer.isInGame()) {
                    return String.valueOf(ctfPlayer.getGame().getPlayers().size());
                }
                return "0";

            // Game scores
            case "red_score":
                if (ctfPlayer.isInGame()) {
                    return String.valueOf(ctfPlayer.getGame().getScore(org.cwresports.ctfcore.models.Arena.TeamColor.RED));
                }
                return "0";
            case "blue_score":
                if (ctfPlayer.isInGame()) {
                    return String.valueOf(ctfPlayer.getGame().getScore(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE));
                }
                return "0";
            case "red_kills":
                if (ctfPlayer.isInGame()) {
                    return String.valueOf(ctfPlayer.getGame().getTeamKills().getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.RED, 0));
                }
                return "0";
            case "blue_kills":
                if (ctfPlayer.isInGame()) {
                    return String.valueOf(ctfPlayer.getGame().getTeamKills().getOrDefault(org.cwresports.ctfcore.models.Arena.TeamColor.BLUE, 0));
                }
                return "0";

            // Time information
            case "time_left":
                if (ctfPlayer.isInGame()) {
                    return ctfPlayer.getGame().getFormattedTimeLeft();
                }
                return "00:00";

            default:
                return null;
        }
    }
}