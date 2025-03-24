package com.dod.UnrealZaruba.Gamemodes.GamePhases;


public enum PhaseId {
    // Common game phases
    TEAM_SELECTION("TeamSelection"),
    GAME("Game"),

    PREPARATION("Preparation"),
    STRATEGY_TIME("StrategyTime"),
    BATTLE("Battle"),
    COMMANDER_VOTING("CommanderVoting"),
    GAME_OVER("GameOver"),
    
    // Add other specific phases as needed
    OBJECTIVE_COMPLETION("ObjectiveCompletion"),
    REINFORCEMENT("Reinforcement"),
    
    // Special phases
    UNDEFINED("Undefined");
    
    private final String phaseName;
    
    PhaseId(String phaseName) {
        this.phaseName = phaseName;
    }
    
    /**
     * Gets the display name of the phase.
     * 
     * @return The phase name as a string
     */
    public String getPhaseName() {
        return phaseName;
    }
    
    /**
     * Get a PhaseId from its string name.
     * 
     * @param name The phase name to look up
     * @return The corresponding PhaseId or UNDEFINED if not found
     */
    public static PhaseId fromString(String name) {
        for (PhaseId phase : values()) {
            if (phase.phaseName.equalsIgnoreCase(name)) {
                return phase;
            }
        }
        return UNDEFINED;
    }
} 