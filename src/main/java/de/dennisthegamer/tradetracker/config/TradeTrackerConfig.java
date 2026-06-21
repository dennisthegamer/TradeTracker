package de.dennisthegamer.tradetracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TradeTrackerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "tradetracker.json"
    );

    private static TradeTrackerConfig INSTANCE = null;

    // General
    public boolean enabled = true;

    // HUD Settings
    public String hudPosition = "TOP_LEFT";
    public boolean hudVisibleAlways = false;
    public float hudOpacity = 0.6f;
    public float hudScale = 1.0f;

    // Session Settings
    public boolean showSessionSummary = true;
    public boolean persistSessions = false;

    // Tracking Settings
    public boolean trackWanderingTrader = true;
    public boolean showVillagerNameplate = true;

    // Item Value Overrides (item ID -> emerald value)
    public Map<String, Float> itemValues = new HashMap<>();

    public TradeTrackerConfig() {
    }

    public static TradeTrackerConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static TradeTrackerConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                TradeTrackerConfig config = GSON.fromJson(reader, TradeTrackerConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load TradeTracker config: " + e.getMessage());
            }
        }
        TradeTrackerConfig config = new TradeTrackerConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().mkdirs() && !CONFIG_FILE.getParentFile().exists()) {
                System.err.println("Failed to create config directory");
                return;
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save TradeTracker config: " + e.getMessage());
        }
    }

    public HudPosition getHudPosition() {
        try {
            return HudPosition.valueOf(hudPosition);
        } catch (IllegalArgumentException e) {
            return HudPosition.TOP_LEFT;
        }
    }

    public enum HudPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
