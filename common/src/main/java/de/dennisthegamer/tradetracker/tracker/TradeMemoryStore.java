package de.dennisthegamer.tradetracker.tracker;

import de.dennisthegamer.tradetracker.TradeTrackerClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dennisthegamer.tradetracker.platform.Platforms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * TradeMemory: persistent registry of all villagers the player has interacted with,
 * their trades, and an item price history for cross-villager price comparison.
 * Stored as JSON in config/tradetracker_memory.json (consistent with the other stores).
 */
public class TradeMemoryStore {

    /** A villager the player has interacted with. */
    public static class VillagerRecord {
        public String uuid;
        public String profession;
        public long firstSeen;
        public long lastSeen;
        public int x, y, z;
        public String dimension;
        public String customTag;              // nullable free-text marker ("Elite", "Meide", ...)
        public boolean markedForTracking;     // glow + direction arrow active
        /**
         * Identity of the world this villager was seen in ("local:&lt;world dir&gt;" or
         * "server:&lt;address&gt;"). Empty for records written before this field existed;
         * those are never treated as belonging to any world.
         */
        public String worldId = "";
    }

    /** A completed trade with a specific villager. */
    public static class TradeRecord {
        public String itemName;
        public int priceInEmeralds;           // actual paid price; 0 = not expressible in emeralds
        public int basePriceInEmeralds;       // unmodified base price (before demand/discounts)
        public String villagerUuid;
        public long timestamp;
        public boolean sellTrade;             // true = villager sells to the player
        public int itemCount;                 // buy offers: demanded item count incl. demand/discounts; 0 = unknown (legacy)
        public int baseItemCount;             // buy offers: unmodified demanded item count

        public int basePrice() {
            return basePriceInEmeralds > 0 ? basePriceInEmeralds : priceInEmeralds;
        }
    }

    /** Latest known offer price per (villager, item) — basis for price comparison. */
    public static class PriceRecord {
        public String itemName;
        public String villagerUuid;
        public int priceInEmeralds;           // actual current price (incl. demand/discounts)
        public int basePriceInEmeralds;       // unmodified base price
        public long timestamp;
        public boolean sellTrade;
        public int itemCount;                 // buy offers: demanded item count incl. demand/discounts; 0 = unknown (legacy)
        public int baseItemCount;             // buy offers: unmodified demanded item count

        public int basePrice() {
            return basePriceInEmeralds > 0 ? basePriceInEmeralds : priceInEmeralds;
        }
    }

    /** Serialized root object. */
    private static class MemoryData {
        Map<String, VillagerRecord> villagers = new HashMap<>();
        List<TradeRecord> trades = new ArrayList<>();
        List<PriceRecord> priceHistory = new ArrayList<>();
    }

    private static final TradeMemoryStore INSTANCE = new TradeMemoryStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STORE_FILE = Platforms.get().getConfigDir()
            .resolve("tradetracker_memory.json");

    private MemoryData data = new MemoryData();
    /** Fast lookup for the per-frame glow check. */
    private final Set<UUID> markedUuids = new HashSet<>();
    private boolean dirty = false;
    /** Identity of the currently joined world; empty while no world is joined. */
    private String worldId = "";

    private TradeMemoryStore() {}

    public static TradeMemoryStore getInstance() {
        return INSTANCE;
    }

    // === Registration & sightings ===

    /** Registers a villager on GUI open, or refreshes profession/position/lastSeen. */
    public void registerOrUpdate(UUID uuid, String profession, int x, int y, int z, String dimension) {
        if (uuid == null) return;
        String key = uuid.toString();
        VillagerRecord rec = data.villagers.get(key);
        long now = System.currentTimeMillis();
        if (rec == null) {
            rec = new VillagerRecord();
            rec.uuid = key;
            rec.firstSeen = now;
            data.villagers.put(key, rec);
        }
        rec.profession = profession;
        rec.lastSeen = now;
        rec.x = x;
        rec.y = y;
        rec.z = z;
        rec.dimension = dimension;
        rec.worldId = worldId;
        dirty = true;
    }

    /** Updates lastSeen + position of an already known villager (called from the client tick). */
    public void updateSighting(UUID uuid, int x, int y, int z, String dimension) {
        if (uuid == null) return;
        VillagerRecord rec = data.villagers.get(uuid.toString());
        if (rec == null) return;
        rec.lastSeen = System.currentTimeMillis();
        rec.x = x;
        rec.y = y;
        rec.z = z;
        rec.dimension = dimension;
        rec.worldId = worldId;
        dirty = true;
    }

    // === Trades & prices ===

    public void recordTrade(UUID villagerUuid, String itemName, int priceInEmeralds,
                            int basePriceInEmeralds, boolean sellTrade,
                            int itemCount, int baseItemCount) {
        if (villagerUuid == null || itemName == null || itemName.isEmpty()) return;
        TradeRecord rec = new TradeRecord();
        rec.itemName = itemName;
        rec.priceInEmeralds = priceInEmeralds;
        rec.basePriceInEmeralds = basePriceInEmeralds;
        rec.villagerUuid = villagerUuid.toString();
        rec.timestamp = System.currentTimeMillis();
        rec.sellTrade = sellTrade;
        rec.itemCount = itemCount;
        rec.baseItemCount = baseItemCount;
        data.trades.add(rec);
        dirty = true;
    }

    /** Upserts the latest offer price for (villager, item). */
    public void recordOffer(UUID villagerUuid, String itemName, int priceInEmeralds,
                            int basePriceInEmeralds, boolean sellTrade,
                            int itemCount, int baseItemCount) {
        if (villagerUuid == null || itemName == null || itemName.isEmpty() || priceInEmeralds <= 0) return;
        String key = villagerUuid.toString();
        for (PriceRecord rec : data.priceHistory) {
            if (rec.villagerUuid.equals(key) && rec.itemName.equals(itemName) && rec.sellTrade == sellTrade) {
                rec.priceInEmeralds = priceInEmeralds;
                rec.basePriceInEmeralds = basePriceInEmeralds;
                rec.timestamp = System.currentTimeMillis();
                rec.itemCount = itemCount;
                rec.baseItemCount = baseItemCount;
                dirty = true;
                return;
            }
        }
        PriceRecord rec = new PriceRecord();
        rec.itemName = itemName;
        rec.villagerUuid = key;
        rec.priceInEmeralds = priceInEmeralds;
        rec.basePriceInEmeralds = basePriceInEmeralds;
        rec.timestamp = System.currentTimeMillis();
        rec.sellTrade = sellTrade;
        rec.itemCount = itemCount;
        rec.baseItemCount = baseItemCount;
        data.priceHistory.add(rec);
        dirty = true;
    }

    // === Queries ===

    public VillagerRecord getVillager(String uuid) {
        return data.villagers.get(uuid);
    }

    /** All known villagers, most recently seen first. */
    public List<VillagerRecord> getAllVillagers() {
        List<VillagerRecord> list = new ArrayList<>(data.villagers.values());
        list.sort(Comparator.comparingLong((VillagerRecord r) -> r.lastSeen).reversed());
        return list;
    }

    /**
     * A record belongs to the current world only if it carries a world identity and that
     * identity matches. The dimension id alone is not enough: "minecraft:overworld" is the
     * same string on every server and in every singleplayer world, so comparing only the
     * dimension made villagers marked on one server show up on all others.
     */
    public boolean belongsToCurrentWorld(VillagerRecord rec) {
        return rec != null && !worldId.isEmpty() && worldId.equals(rec.worldId);
    }

    /** Marked villagers of the world currently joined - the basis for arrow and glow. */
    public List<VillagerRecord> getMarkedVillagersInCurrentWorld() {
        return data.villagers.values().stream()
                .filter(r -> r.markedForTracking && belongsToCurrentWorld(r)).toList();
    }

    public List<VillagerRecord> getMarkedVillagers() {
        return data.villagers.values().stream().filter(r -> r.markedForTracking).toList();
    }

    public boolean isMarked(UUID uuid) {
        return uuid != null && markedUuids.contains(uuid);
    }

    public List<TradeRecord> getTradesFor(String uuid) {
        List<TradeRecord> list = new ArrayList<>();
        for (TradeRecord rec : data.trades) {
            if (rec.villagerUuid.equals(uuid)) list.add(rec);
        }
        list.sort(Comparator.comparingLong((TradeRecord r) -> r.timestamp).reversed());
        return list;
    }

    /** Current known sell offers of one villager, cheapest first. */
    public List<PriceRecord> getOffersFor(String uuid) {
        List<PriceRecord> list = new ArrayList<>();
        for (PriceRecord rec : data.priceHistory) {
            if (rec.villagerUuid.equals(uuid)) list.add(rec);
        }
        list.sort(Comparator.comparingInt(r -> r.priceInEmeralds));
        return list;
    }

    /** Cheapest sell offer for the same item from any OTHER villager, or null. */
    public PriceRecord findBetterPrice(String itemName, String excludeUuid, int currentPrice) {
        PriceRecord best = null;
        for (PriceRecord rec : data.priceHistory) {
            if (!rec.sellTrade) continue;
            if (rec.villagerUuid.equals(excludeUuid)) continue;
            if (!rec.itemName.equals(itemName)) continue;
            if (!data.villagers.containsKey(rec.villagerUuid)) continue;
            if (rec.priceInEmeralds >= currentPrice) continue;
            if (best == null || rec.priceInEmeralds < best.priceInEmeralds) best = rec;
        }
        return best;
    }

    /** All sell offers whose item name contains the query (case-insensitive), cheapest first. */
    public List<PriceRecord> searchOffers(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<PriceRecord> list = new ArrayList<>();
        for (PriceRecord rec : data.priceHistory) {
            if (!rec.sellTrade) continue;
            if (!data.villagers.containsKey(rec.villagerUuid)) continue;
            if (rec.itemName.toLowerCase(Locale.ROOT).contains(q)) list.add(rec);
        }
        list.sort(Comparator.comparingInt(r -> r.priceInEmeralds));
        return list;
    }

    public int getTradeCountFor(String uuid) {
        int count = 0;
        for (TradeRecord rec : data.trades) {
            if (rec.villagerUuid.equals(uuid)) count++;
        }
        return count;
    }

    /** How often a specific offer (item + direction) was traded with this villager. */
    public int getTradeCountFor(String uuid, String itemName, boolean sellTrade) {
        int count = 0;
        for (TradeRecord rec : data.trades) {
            if (rec.villagerUuid.equals(uuid) && rec.sellTrade == sellTrade
                    && rec.itemName.equals(itemName)) count++;
        }
        return count;
    }

    // === World binding ===

    /** Called on world join, after loadFromDisk: rebinds the store to the joined world. */
    public void setWorld(String worldId) {
        this.worldId = worldId == null ? "" : worldId;
        rebuildMarkedCache();
    }

    /** Called on world leave: nothing is marked while no world is joined. */
    public void clearWorld() {
        worldId = "";
        markedUuids.clear();
    }

    // === Mutations from the GUI ===

    public void setMarkedForTracking(String uuid, boolean marked) {
        VillagerRecord rec = data.villagers.get(uuid);
        if (rec == null) return;
        rec.markedForTracking = marked;
        rebuildMarkedCache();
        dirty = true;
        saveToDisk();
    }

    public void setCustomTag(String uuid, String tag) {
        VillagerRecord rec = data.villagers.get(uuid);
        if (rec == null) return;
        rec.customTag = (tag == null || tag.isBlank()) ? null : tag.trim();
        dirty = true;
        saveToDisk();
    }

    public void removeVillager(String uuid) {
        data.villagers.remove(uuid);
        data.trades.removeIf(rec -> rec.villagerUuid.equals(uuid));
        data.priceHistory.removeIf(rec -> rec.villagerUuid.equals(uuid));
        rebuildMarkedCache();
        dirty = true;
        saveToDisk();
    }

    // === Persistence ===

    private void rebuildMarkedCache() {
        markedUuids.clear();
        for (VillagerRecord rec : data.villagers.values()) {
            if (!rec.markedForTracking || !belongsToCurrentWorld(rec)) continue;
            try {
                markedUuids.add(UUID.fromString(rec.uuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveToDisk() {
        try {
            Files.writeString(STORE_FILE, GSON.toJson(data));
            dirty = false;
        } catch (IOException e) {
            TradeTrackerClient.LOGGER.error("Failed to save trade memory store", e);
        }
    }

    public void saveIfDirty() {
        if (dirty) saveToDisk();
    }

    public void loadFromDisk() {
        if (!Files.exists(STORE_FILE)) return;
        try {
            String json = Files.readString(STORE_FILE);
            MemoryData loaded = GSON.fromJson(json, MemoryData.class);
            if (loaded == null) return;
            if (loaded.villagers == null) loaded.villagers = new HashMap<>();
            if (loaded.trades == null) loaded.trades = new ArrayList<>();
            if (loaded.priceHistory == null) loaded.priceHistory = new ArrayList<>();
            // Gson allocates without running field initialisers, so records written before
            // worldId existed come back as null rather than "".
            for (VillagerRecord rec : loaded.villagers.values()) {
                if (rec != null && rec.worldId == null) rec.worldId = "";
            }
            data = loaded;
            rebuildMarkedCache();
            dirty = false;
        } catch (Exception e) {
            TradeTrackerClient.LOGGER.error("Failed to load trade memory store", e);
        }
    }
}
