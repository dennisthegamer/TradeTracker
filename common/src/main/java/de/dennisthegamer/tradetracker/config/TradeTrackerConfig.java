package de.dennisthegamer.tradetracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dennisthegamer.tradetracker.platform.Platforms;
import de.dennisthegamer.hudlib.position.HudPlacement;
import de.dennisthegamer.hudlib.position.HudPositionMigration;
import de.dennisthegamer.hudlib.position.HudPreset;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeTrackerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            Platforms.get().getConfigDir().toFile(),
            "tradetracker.json"
    );

    private static TradeTrackerConfig INSTANCE = null;

    // General
    public boolean enabled = true;

    // HUD Settings
    public String hudPosition = "TOP_LEFT";
    /** Freie HUD-Position (Anker + Offset). Nach {@link #load()} immer non-null. */
    public HudPlacement hudPlacement = null;
    /** Vom Nutzer gespeicherte Positions-Slots. */
    public List<HudPreset> hudSlots = new ArrayList<>();
    public boolean hudVisibleAlways = false;
    public float hudOpacity = 0.6f;
    public float hudScale = 1.0f;

    // Session Settings
    public boolean showSessionSummary = true;
    public boolean persistSessions = false;

    // Tracking Settings
    public boolean trackWanderingTrader = true;
    public boolean showVillagerNameplate = true;

    // TradeMemory Settings
    public boolean glowMarkedVillagers = true;
    public boolean showDirectionArrow = true;
    public String arrowPosition = "TOP_CENTER";
    public int glowColor = 0x55FFFF;

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
                    config.migrateHudPosition();
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to load TradeTracker config: " + e.getMessage());
            }
        }
        TradeTrackerConfig config = new TradeTrackerConfig();
        config.migrateHudPosition();
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

    /**
     * Einmalige Migration: befüllt {@link #hudPlacement} aus dem Legacy-{@link #hudPosition}
     * (4-Ecken-Enum als String; Feld-Default TOP_LEFT bleibt so optisch erhalten) und stoppt
     * das Persistieren des Legacy-Feldes (Gson lässt null-Felder weg).
     */
    public void migrateHudPosition() {
        if (hudPlacement == null) {
            hudPlacement = HudPositionMigration.fromLegacy(hudPosition);
        }
        hudPosition = null;
        if (hudSlots == null) {
            hudSlots = new ArrayList<>();
        }
    }

    /** Non-null-Zugriff für Renderer/Editor (defensiv, falls die JSON von Hand geleert wurde). */
    public HudPlacement getHudPlacement() {
        if (hudPlacement == null) {
            migrateHudPosition();
        }
        return hudPlacement;
    }

    public ArrowPosition getArrowPosition() {
        try {
            return ArrowPosition.valueOf(arrowPosition);
        } catch (IllegalArgumentException e) {
            return ArrowPosition.TOP_CENTER;
        }
    }

    public enum ArrowPosition {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, CENTER, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }
}
