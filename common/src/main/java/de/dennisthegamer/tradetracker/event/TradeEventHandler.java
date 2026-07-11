package de.dennisthegamer.tradetracker.event;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import de.dennisthegamer.tradetracker.tracker.EmeraldValueTable;
import de.dennisthegamer.tradetracker.tracker.TradeEntry;
import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import de.dennisthegamer.tradetracker.tracker.TradeSession;
import de.dennisthegamer.tradetracker.tracker.VillagerTradeStore;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TradeEventHandler {

    private static String currentProfession = "Unknown";
    private static int currentLevel = 1;
    private static boolean currentIsWandering = false;
    private static UUID currentVillagerUuid = null;
    private static boolean offersCaptured = false;

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
        AbstractVillager villager = null;
        if (mc.crosshairPickEntity instanceof AbstractVillager v) {
            villager = v;
        } else if (mc.player != null && mc.level != null) {
            AABB searchBox = mc.player.getBoundingBox().inflate(5.0);
            villager = mc.level.getEntitiesOfClass(AbstractVillager.class, searchBox)
                    .stream()
                    .min(Comparator.comparingDouble(v -> v.distanceTo(mc.player)))
                    .orElse(null);
        }
        if (villager != null) {
            currentVillagerUuid = villager.getUUID();
        }

        // TradeMemory: register the villager, regardless of whether the player trades.
        offersCaptured = false;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (villager != null && mc.level != null && config.enabled
                && (!currentIsWandering || config.trackWanderingTrader)) {
            TradeMemoryStore store = TradeMemoryStore.getInstance();
            BlockPos pos = villager.blockPosition();
            String dimension = mc.level.dimension().identifier().toString();
            store.registerOrUpdate(currentVillagerUuid, currentProfession,
                    pos.getX(), pos.getY(), pos.getZ(), dimension);
            store.saveToDisk();
        }
    }

    /**
     * Called every tick while a merchant screen is open. The offer list arrives from the
     * server shortly AFTER the screen opens, so the prices are captured here as soon as
     * the list is populated — even if the player never trades.
     */
    public static void onMerchantScreenTick(MerchantScreen screen) {
        if (offersCaptured || currentVillagerUuid == null) return;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;
        if (currentIsWandering && !config.trackWanderingTrader) return;
        if (screen.getMenu().getOffers().isEmpty()) return;

        TradeMemoryStore store = TradeMemoryStore.getInstance();
        for (MerchantOffer offer : screen.getMenu().getOffers()) {
            OfferInfo info = describeOffer(offer);
            if (info != null) {
                store.recordOffer(currentVillagerUuid, info.itemName, info.price, info.basePrice,
                        info.sellTrade, info.itemCount, info.baseItemCount);
            }
        }
        store.saveToDisk();
        offersCaptured = true;
    }

    /**
     * Item name, emerald price and direction of an offer — null if not priceable in emeralds.
     * {@code price} is the ACTUAL current price including demand/discount adjustments
     * (Hero of the Village, gossip, demand); {@code basePrice} is the unmodified price.
     * For buy offers the adjustments apply to the ITEM COUNT the villager demands, not the
     * emerald payout — {@code itemCount} is the adjusted count, {@code baseItemCount} the base.
     */
    private record OfferInfo(String itemName, int price, int basePrice, boolean sellTrade,
                             int itemCount, int baseItemCount) {}

    private static OfferInfo describeOffer(MerchantOffer offer) {
        ItemStack actualCostA = offer.getCostA();   // adjusted by demand + special price diff
        ItemStack baseCostA = offer.getBaseCostA(); // unmodified base cost
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        int actualCost = emeraldCount(actualCostA) + emeraldCount(costB);
        int baseCost = emeraldCount(baseCostA) + emeraldCount(costB);
        if (actualCost > 0) {
            // Villager sells the result item for emeralds
            return new OfferInfo(describeItem(result), actualCost, baseCost, true,
                    result.getCount(), result.getCount());
        }
        if (result.is(Items.EMERALD)) {
            // Villager buys the cost item and pays emeralds; demand/discounts change the
            // demanded item count (e.g. 6 → 10 leather for 1 emerald), never the payout
            return new OfferInfo(describeItem(baseCostA), result.getCount(), result.getCount(), false,
                    actualCostA.getCount(), baseCostA.getCount());
        }
        return null;
    }

    private static int emeraldCount(ItemStack stack) {
        return stack.is(Items.EMERALD) ? stack.getCount() : 0;
    }

    /**
     * Display name of an item; enchanted books include their stored enchantments so that
     * price comparison distinguishes e.g. "Enchanted Book (Mending)" from "Enchanted Book (Sharpness V)".
     */
    public static String describeItem(ItemStack stack) {
        String name = stack.getHoverName().getString();
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
                parts.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            }
            name += " (" + String.join(", ", parts) + ")";
        }
        return name;
    }

    public static void onTradeCompleted(MerchantOffer offer, UUID villagerUuid) {
        // Prefer UUID from mixin; fall back to UUID captured at screen open
        UUID effectiveUuid = villagerUuid != null ? villagerUuid : currentVillagerUuid;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;
        if (currentIsWandering && !config.trackWanderingTrader) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        // getCostA() = actual cost incl. demand/discount adjustments (what the player really pays)
        ItemStack input1 = offer.getCostA();
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

        // TradeMemory: persist the completed trade and refresh the price history
        if (effectiveUuid != null) {
            TradeMemoryStore store = TradeMemoryStore.getInstance();
            OfferInfo info = describeOffer(offer);
            if (info != null) {
                store.recordTrade(effectiveUuid, info.itemName, info.price, info.basePrice,
                        info.sellTrade, info.itemCount, info.baseItemCount);
                store.recordOffer(effectiveUuid, info.itemName, info.price, info.basePrice,
                        info.sellTrade, info.itemCount, info.baseItemCount);
            } else {
                // Not expressible in emeralds (barter trade) — record with price 0
                store.recordTrade(effectiveUuid, describeItem(output), 0, 0, true, 0, 0);
            }
            store.saveToDisk();
        }

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
        offersCaptured = false;
    }

    public static boolean isMerchantScreenClosed() {
        Minecraft client = Minecraft.getInstance();
        return !(client.screen instanceof MerchantScreen);
    }
}
