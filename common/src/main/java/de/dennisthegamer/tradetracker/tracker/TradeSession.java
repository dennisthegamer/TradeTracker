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
import java.util.*;

/**
 * Holds all trade data for the current session.
 */
public class TradeSession {

    private static final TradeSession INSTANCE = new TradeSession();

    private final List<TradeEntry> trades = new ArrayList<>();
    private long accumulatedMillis;
    private long segmentStartTime;
    private boolean paused;
    private int totalProfit;
    private int totalLoss;

    private TradeSession() {
        reset();
    }

    public static TradeSession getInstance() {
        return INSTANCE;
    }

    public void reset() {
        trades.clear();
        accumulatedMillis = 0;
        segmentStartTime = 0;
        paused = true;
        totalProfit = 0;
        totalLoss = 0;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        if (!paused) {
            accumulatedMillis += System.currentTimeMillis() - segmentStartTime;
            paused = true;
        }
    }

    public void resume() {
        if (paused) {
            segmentStartTime = System.currentTimeMillis();
            paused = false;
        }
    }

    public void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public void addTrade(TradeEntry entry) {
        trades.add(entry);
        int balance = entry.getEmeraldBalance();
        if (balance > 0) {
            totalProfit += balance;
        } else if (balance < 0) {
            totalLoss += Math.abs(balance);
        }
    }

    public List<TradeEntry> getTrades() {
        return Collections.unmodifiableList(trades);
    }

    public int getTradeCount() {
        return trades.size();
    }

    public int getTotalProfit() {
        return totalProfit;
    }

    public int getTotalLoss() {
        return totalLoss;
    }

    public int getNetBalance() {
        return totalProfit - totalLoss;
    }

    public TradeEntry getBestTrade() {
        return trades.stream()
                .max(Comparator.comparingInt(TradeEntry::getEmeraldBalance))
                .orElse(null);
    }

    public TradeEntry getWorstTrade() {
        return trades.stream()
                .min(Comparator.comparingInt(TradeEntry::getEmeraldBalance))
                .orElse(null);
    }

    /**
     * Count trades that happened on the given in-game day.
     */
    public int getTradesForDay(int gameDay) {
        return (int) trades.stream()
                .filter(t -> t.getGameDay() == gameDay)
                .count();
    }

    /**
     * Get all unique professions with their trade counts.
     */
    public Map<String, Integer> getProfessionCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TradeEntry entry : trades) {
            String key = entry.isWanderingTrader() ? "Wandering Trader" : entry.getVillagerProfession();
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Get all unique professions with their total emerald balance.
     */
    public Map<String, Integer> getProfessionBalances() {
        Map<String, Integer> balances = new LinkedHashMap<>();
        for (TradeEntry entry : trades) {
            String key = entry.isWanderingTrader() ? "Wandering Trader" : entry.getVillagerProfession();
            balances.merge(key, entry.getEmeraldBalance(), Integer::sum);
        }
        return balances;
    }

    /**
     * Get trades filtered by profession. Null = all trades.
     */
    public List<TradeEntry> getTradesForProfession(String profession) {
        if (profession == null) return getTrades();
        return trades.stream()
                .filter(t -> {
                    String key = t.isWanderingTrader() ? "Wandering Trader" : t.getVillagerProfession();
                    return key.equals(profession);
                })
                .toList();
    }

    /**
     * Get the most traded profession name and count.
     */
    public Map.Entry<String, Integer> getMostTradedProfession() {
        return getProfessionCounts().entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElse(null);
    }

    public long getSessionDurationMillis() {
        if (paused) {
            return accumulatedMillis;
        }
        return accumulatedMillis + (System.currentTimeMillis() - segmentStartTime);
    }

    public String getFormattedDuration() {
        long totalSeconds = getSessionDurationMillis() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SESSION_FILE = Platforms.get().getConfigDir()
            .resolve("tradetracker_session.json");

    public void saveToDisk() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("durationMillis", getSessionDurationMillis());
            data.put("totalProfit", totalProfit);
            data.put("totalLoss", totalLoss);

            List<Map<String, Object>> tradeList = new ArrayList<>();
            for (TradeEntry entry : trades) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("profession", entry.getVillagerProfession());
                t.put("level", entry.getVillagerLevel());
                t.put("wandering", entry.isWanderingTrader());
                t.put("inputDesc", entry.getInputDescription());
                t.put("outputDesc", entry.getOutputDescription());
                t.put("balance", entry.getEmeraldBalance());
                t.put("gameTime", entry.getGameTime());
                t.put("realTime", entry.getRealTime());
                t.put("gameDay", entry.getGameDay());
                tradeList.add(t);
            }
            data.put("trades", tradeList);

            Files.writeString(SESSION_FILE, GSON.toJson(data));
        } catch (IOException e) {
            TradeTrackerClient.LOGGER.error("Failed to save session", e);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean loadFromDisk() {
        if (!Files.exists(SESSION_FILE)) return false;
        try {
            String json = Files.readString(SESSION_FILE);
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = GSON.fromJson(json, type);
            if (data == null) return false;

            Number duration = (Number) data.get("durationMillis");
            Number profit = (Number) data.get("totalProfit");
            Number loss = (Number) data.get("totalLoss");
            if (duration == null) return false;

            this.accumulatedMillis = duration.longValue();
            this.totalProfit = profit != null ? profit.intValue() : 0;
            this.totalLoss = loss != null ? loss.intValue() : 0;
            this.paused = true;

            List<Map<String, Object>> tradeList = (List<Map<String, Object>>) data.get("trades");
            if (tradeList != null) {
                for (Map<String, Object> t : tradeList) {
                    String profession = (String) t.get("profession");
                    int level = ((Number) t.get("level")).intValue();
                    boolean wandering = Boolean.TRUE.equals(t.get("wandering"));
                    String inputDesc = (String) t.get("inputDesc");
                    String outputDesc = (String) t.get("outputDesc");
                    int balance = ((Number) t.get("balance")).intValue();
                    long gameTime = ((Number) t.get("gameTime")).longValue();
                    long realTime = ((Number) t.get("realTime")).longValue();
                    int gameDay = ((Number) t.get("gameDay")).intValue();

                    trades.add(new TradeEntry(profession, level, wandering,
                            inputDesc, outputDesc, balance, gameTime, realTime, gameDay));
                }
            }

            return !trades.isEmpty();
        } catch (Exception e) {
            TradeTrackerClient.LOGGER.error("Failed to load session", e);
            return false;
        }
    }

    public void deleteSavedSession() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            TradeTrackerClient.LOGGER.error("Failed to delete saved session", e);
        }
    }

}
