package com.dod.UnrealZaruba.Config;

import java.io.File;
import java.io.IOException;

import com.dod.UnrealZaruba.UnrealZaruba;
import com.dod.UnrealZaruba.ConfigurationManager.ConfigManager;
import net.minecraft.core.BlockPos;

public class MainConfig extends AbstractConfig<MainConfig.MainConfigData> {
    
    private static final String CONFIG_PATH = "unrealzaruba" + File.separator + "mainConfig.json";
    private static MainConfig instance;
    
    public enum Mode {
        DEV,    // Mode to change map in many ways
        GAME    // Mode to enable for game
    }

    public static class MainConfigData {
        private boolean isZarubaServer = false;
        private Mode mode = Mode.GAME;
        private BlockPos lobbySpawnPoint = new BlockPos(0, 65, 0);

        public boolean getIsZarubaServer() {
            return isZarubaServer;
        }

        public void setIsZarubaServer(boolean isZarubaServer) {
            this.isZarubaServer = isZarubaServer;
        }

        public Mode getMode() {
            return mode;
        }
        
        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public BlockPos getLobbySpawnPoint() {
            return lobbySpawnPoint;
        }

        public void setLobbySpawnPoint(BlockPos lobbySpawnPoint) {
            this.lobbySpawnPoint = lobbySpawnPoint;
        }
    }
    
    public static MainConfig getInstance() {
        if (instance == null) {
            instance = new MainConfig();
        }
        return instance;
    }
    
    private MainConfig() {
        ConfigManager.registerConfig(MainConfigData.class, this);
    }
    
    @Override
    protected String getFilePath() {
        return CONFIG_PATH;
    }
    
    @Override
    protected Class<MainConfigData> getConfigClass() {
        return MainConfigData.class;
    }
    
    @Override
    protected MainConfigData getDefaultConfig() {
        return new MainConfigData(); // Default to GAME mode
    }
    
    public void saveMainConfig(MainConfigData config) {
        try {
            save(config);
            UnrealZaruba.LOGGER.info("[UnrealZaruba] Saved main configuration");
        } catch (IOException e) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] Failed to save main configuration");
            e.printStackTrace();
        }
    }
    
    public MainConfigData loadMainConfig() {
        MainConfigData config = load();
        if (config == null) {
            UnrealZaruba.LOGGER.warn("[UnrealZaruba] No main configuration found, using defaults");
            return createDefaultIfNotExist();
        }
        return config;
    }
    
    public Mode getMode() {
        return loadMainConfig().getMode();
    }

    public boolean isZarubaServer() {
        return loadMainConfig().isZarubaServer;
    }
    
    public void setMode(Mode mode) {
        MainConfigData config = loadMainConfig();
        config.setMode(mode);
        saveMainConfig(config);
        UnrealZaruba.LOGGER.info("[UnrealZaruba] Zaruba mode set to: " + mode);
    }

    public BlockPos getLobbySpawnPoint() {
        return loadMainConfig().getLobbySpawnPoint();
    }
} 