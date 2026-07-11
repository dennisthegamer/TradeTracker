package de.dennisthegamer.tradetracker.tracker;

import de.dennisthegamer.tradetracker.TradeTrackerClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.dennisthegamer.tradetracker.platform.Platforms;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persists per-villager trade counts (UUID → count) to tradetracker_villagers.json. */
public class VillagerTradeStore {

    private static final VillagerTradeStore INSTANCE = new VillagerTradeStore();

    private final Map<UUID, Integer> tradeCounts = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORE_FILE = Platforms.get().getConfigDir()
            .resolve("tradetracker_villagers.json");

    private VillagerTradeStore() {}

    public static VillagerTradeStore getInstance() {
        return INSTANCE;
    }

    public void increment(UUID villagerUuid) {
        if (villagerUuid == null) return;
        tradeCounts.merge(villagerUuid, 1, Integer::sum);
        saveToDisk();
    }

    public int getCount(UUID villagerUuid) {
        if (villagerUuid == null) return 0;
        return tradeCounts.getOrDefault(villagerUuid, 0);
    }

    void saveToDisk() {
        try {
            Map<String, Integer> serializable = new LinkedHashMap<>();
            tradeCounts.forEach((uuid, count) -> serializable.put(uuid.toString(), count));
            Files.writeString(STORE_FILE, GSON.toJson(serializable));
        } catch (IOException e) {
            TradeTrackerClient.LOGGER.error("Failed to save villager trade store", e);
        }
    }

    public void loadFromDisk() {
        if (!Files.exists(STORE_FILE)) return;
        try {
            String json = Files.readString(STORE_FILE);
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> data = GSON.fromJson(json, type);
            if (data == null) return;
            tradeCounts.clear();
            data.forEach((key, count) -> {
                try {
                    tradeCounts.put(UUID.fromString(key), count);
                } catch (IllegalArgumentException e) {
                    TradeTrackerClient.LOGGER.warn("Skipping invalid UUID key in villager store: {}", key);
                }
            });
        } catch (Exception e) {
            TradeTrackerClient.LOGGER.error("Failed to load villager trade store", e);
        }
    }
}
