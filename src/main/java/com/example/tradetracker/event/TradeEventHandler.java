package com.example.tradetracker.event;

import com.example.tradetracker.config.TradeTrackerConfig;
import com.example.tradetracker.render.TradeTrackerHud;
import com.example.tradetracker.tracker.EmeraldValueTable;
import com.example.tradetracker.tracker.TradeEntry;
import com.example.tradetracker.tracker.TradeSession;
import com.example.tradetracker.tracker.VillagerTradeStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.UUID;

public class TradeEventHandler {

    private static String currentProfession = "Unknown";
    private static int currentLevel = 1;
    private static boolean currentIsWandering = false;
    private static UUID currentVillagerUuid = null;

    public static void onMerchantScreenOpen(MerchantScreen screen) {
        String title = screen.getTitle().getString();
        currentProfession = title;
        currentIsWandering = title.equalsIgnoreCase("Wandering Trader")
                || title.equalsIgnoreCase("Fahrender Händler");
        currentLevel = screen.getMenu().getTraderLevel();

        // Capture UUID: MerchantContainer holds a ClientSideMerchant on the client,
        // so we get the UUID from crosshairPickEntity or the nearest AbstractVillager instead.
        currentVillagerUuid = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof AbstractVillager v) {
            currentVillagerUuid = v.getUUID();
        } else if (mc.player != null && mc.level != null) {
            AABB searchBox = mc.player.getBoundingBox().inflate(5.0);
            mc.level.getEntitiesOfClass(AbstractVillager.class, searchBox)
                    .stream()
                    .min(Comparator.comparingDouble(v -> v.distanceTo(mc.player)))
                    .ifPresent(v -> currentVillagerUuid = v.getUUID());
        }
    }

    public static void onTradeCompleted(MerchantOffer offer, UUID villagerUuid) {
        // Prefer UUID from mixin; fall back to UUID captured at screen open
        UUID effectiveUuid = villagerUuid != null ? villagerUuid : currentVillagerUuid;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;
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
        VillagerTradeStore.getInstance().increment(effectiveUuid);

        if (balance > 10) {
            TradeTrackerHud.triggerProfitFlash();
        } else if (balance < -5) {
            TradeTrackerHud.triggerLossFlash();
        }
    }

    public static void onMerchantScreenClose() {
        currentProfession = "Unknown";
        currentLevel = 1;
        currentIsWandering = false;
        currentVillagerUuid = null;
    }

    public static boolean isMerchantScreenClosed() {
        Minecraft client = Minecraft.getInstance();
        return !(client.screen instanceof MerchantScreen);
    }
}
