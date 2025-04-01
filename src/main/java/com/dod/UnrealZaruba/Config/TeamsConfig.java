package com.dod.UnrealZaruba.Config;

import java.io.File;
import java.io.IOException;

import com.dod.UnrealZaruba.TeamLogic.TeamData;
import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;

public class TeamsConfig extends AbstractConfig<TeamData> {
    
    private static final String CONFIG_PATH = "unrealzaruba" + File.separator + "teams.json";
    private static TeamsConfig instance;
    
    public static TeamsConfig getInstance() {
        if (instance == null) {
            instance = new TeamsConfig();
        }
        return instance;
    }
    
    private TeamsConfig() {
        // Register this config with the manager
        ConfigManager.registerConfig(TeamData.class, this);
    }
    
    @Override
    protected String getFilePath() {
        return CONFIG_PATH;
    }
    
    @Override
    protected Class<TeamData> getConfigClass() {
        return TeamData.class;
    }
    
    @Override
    protected TeamData getDefaultConfig() {
        return new TeamData(); // Empty TeamData as default
    }
    
    public void saveTeamData(TeamData teamData) {
        try {
            save(teamData);
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved teams configuration");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Failed to save teams configuration");
            e.printStackTrace();
        }
    }
    
    public TeamData loadTeamData() {
        TeamData teamData = load();
        if (teamData == null) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] No team data found, using empty team data");
            return new TeamData();
        }
        return teamData;
    }
} 