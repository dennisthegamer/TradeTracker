package com.example.tradetracker.event;

import com.example.tradetracker.config.TradeTrackerConfig;
import com.example.tradetracker.render.TradeTrackerHud;
import com.example.tradetracker.tracker.EmeraldValueTable;
import com.example.tradetracker.tracker.TradeEntry;
import com.example.tradetracker.tracker.TradeSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class TradeEventHandler {

    // Current merchant context (set when MerchantScreen opens)
    private static String currentProfession = "Unknown";
    private static int currentLevel = 1;
    private static boolean currentIsWandering = false;

    /**
     * Called when a MerchantScreen is detected as open. Updates the merchant context.
     */
    public static void onMerchantScreenOpen(MerchantScreen screen) {
        String title = screen.getTitle().getString();
        currentProfession = title;

        // Check if this is a wandering trader
        currentIsWandering = title.equalsIgnoreCase("Wandering Trader")
                || title.equalsIgnoreCase("Fahrender Händler");

        // Get trader level from the menu
        currentLevel = screen.getMenu().getTraderLevel();
    }

    /**
     * Called from the mixin when a trade result is taken by the player.
     */
    public static void onTradeCompleted(Player player, MerchantOffer offer, Merchant merchant) {
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;

        // Skip wandering trader if disabled
        if (currentIsWandering && !config.trackWanderingTrader) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        ItemStack input1 = offer.getBaseCostA();
        ItemStack input2 = offer.getCostB();
        ItemStack output = offer.getResult();

        int balance = EmeraldValueTable.calculateBalance(input1, input2, output);

        long gameTime = client.level.getOverworldClockTime();
        int gameDay = (int) (gameTime / 24000L) + 1;

        TradeEntry entry = new TradeEntry(
                currentProfession,
                currentLevel,
                currentIsWandering,
                input1,
                input2,
                output,
                balance,
                gameTime,
                gameDay
        );

        TradeSession.getInstance().addTrade(entry);

        // Trigger HUD flash effects
        if (balance > 10) {
            TradeTrackerHud.triggerProfitFlash();
        } else if (balance < -5) {
            TradeTrackerHud.triggerLossFlash();
        }
    }

    /**
     * Reset merchant context when the screen closes.
     */
    public static void onMerchantScreenClose() {
        currentProfession = "Unknown";
        currentLevel = 1;
        currentIsWandering = false;
    }

    public static boolean isMerchantScreenOpen() {
        Minecraft client = Minecraft.getInstance();
        return client.screen instanceof MerchantScreen;
    }

    public static String getCurrentProfession() {
        return currentProfession;
    }

    public static boolean isCurrentWandering() {
        return currentIsWandering;
    }
}
