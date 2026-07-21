package de.dennisthegamer.tradetracker.tracker;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A single recorded trade event.
 */
public class TradeEntry {

    private final String villagerProfession;
    private final int villagerLevel;
    private final boolean wanderingTrader;
    private final ItemStack input1;
    private final ItemStack input2;
    private final ItemStack output;
    private final int emeraldBalance;
    private final long gameTime;
    private final long realTime;
    private final int gameDay;

    // Cached descriptions for persistence (populated lazily or from deserialization)
    private String cachedInputDesc;
    private String cachedOutputDesc;

    public TradeEntry(String villagerProfession, int villagerLevel, boolean wanderingTrader,
                      ItemStack input1, ItemStack input2, ItemStack output,
                      int emeraldBalance, long gameTime, int gameDay) {
        this.villagerProfession = villagerProfession;
        this.villagerLevel = villagerLevel;
        this.wanderingTrader = wanderingTrader;
        this.input1 = input1.copy();
        this.input2 = input2.isEmpty() ? ItemStack.EMPTY : input2.copy();
        this.output = output.copy();
        this.emeraldBalance = emeraldBalance;
        this.gameTime = gameTime;
        this.realTime = System.currentTimeMillis();
        this.gameDay = gameDay;
    }

    /**
     * Constructor for deserialized entries (restored from disk).
     * Uses pre-computed string descriptions instead of ItemStacks.
     */
    public TradeEntry(String villagerProfession, int villagerLevel, boolean wanderingTrader,
                      String inputDesc, String outputDesc,
                      int emeraldBalance, long gameTime, long realTime, int gameDay) {
        this.villagerProfession = villagerProfession;
        this.villagerLevel = villagerLevel;
        this.wanderingTrader = wanderingTrader;
        this.input1 = ItemStack.EMPTY;
        this.input2 = ItemStack.EMPTY;
        this.output = ItemStack.EMPTY;
        this.emeraldBalance = emeraldBalance;
        this.gameTime = gameTime;
        this.realTime = realTime;
        this.gameDay = gameDay;
        this.cachedInputDesc = inputDesc;
        this.cachedOutputDesc = outputDesc;
    }

    public String getVillagerProfession() {
        return villagerProfession;
    }

    public int getVillagerLevel() {
        return villagerLevel;
    }

    public boolean isWanderingTrader() {
        return wanderingTrader;
    }

    public int getEmeraldBalance() {
        return emeraldBalance;
    }

    public long getGameTime() {
        return gameTime;
    }

    public long getRealTime() {
        return realTime;
    }

    public int getGameDay() {
        return gameDay;
    }

    public String getLevelName() {
        // Ueber die Sprachdateien statt hart englisch: die Keys
        // tradetracker.level.1-5 gab es samt deutscher Uebersetzung laengst,
        // sie wurden nur nie gelesen.
        String key = (villagerLevel >= 1 && villagerLevel <= 5)
                ? "tradetracker.level." + villagerLevel
                : "tradetracker.level.unknown";
        return Component.translatable(key).getString();
    }

    public String getInputDescription() {
        if (cachedInputDesc != null) return cachedInputDesc;
        StringBuilder sb = new StringBuilder();
        sb.append(input1.getCount()).append("x ").append(getItemName(input1));
        if (!input2.isEmpty()) {
            sb.append(" + ").append(input2.getCount()).append("x ").append(getItemName(input2));
        }
        cachedInputDesc = sb.toString();
        return cachedInputDesc;
    }

    public String getOutputDescription() {
        if (cachedOutputDesc != null) return cachedOutputDesc;
        cachedOutputDesc = output.getCount() + "x " + getItemName(output);
        return cachedOutputDesc;
    }

    private String getItemName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    public String getFormattedTime() {
        long ticks = gameTime % 24000;
        int hours = (int) ((ticks / 1000 + 6) % 24);
        int minutes = (int) ((ticks % 1000) * 60 / 1000);
        return String.format("Day %d %02d:%02d", gameDay, hours, minutes);
    }

}
