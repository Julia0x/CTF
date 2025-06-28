package org.cwresports.ctfcore.models;

/**
 * Represents the current state of a CTF game
 */
public enum GameState {
    /**
     * Game is waiting for players to join
     */
    WAITING,
    
    /**
     * Game is starting (countdown phase)
     */
    STARTING,
    
    /**
     * Game is actively being played
     */
    PLAYING,
    
    /**
     * Game is ending (cleanup phase)
     */
    ENDING
}