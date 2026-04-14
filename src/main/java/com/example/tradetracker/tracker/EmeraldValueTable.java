package com.example.tradetracker.tracker;

import com.example.tradetracker.config.TradeTrackerConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps items to their Emerald equivalent value for trade balance calculations.
 */
public class EmeraldValueTable {

    private static final Map<String, Float> DEFAULT_VALUES = new HashMap<>();

    static {
        // Basic crops / materials (value per stack-amount used in typical trades)
        DEFAULT_VALUES.put("minecraft:wheat", 0.05f);           // 20 wheat = 1 emerald -> 1/20
        DEFAULT_VALUES.put("minecraft:paper", 0.042f);          // 24 paper = 1 emerald -> 1/24
        DEFAULT_VALUES.put("minecraft:book", 1.0f);             // 1 book ~ 1 emerald
        DEFAULT_VALUES.put("minecraft:bookshelf", 3.0f);
        DEFAULT_VALUES.put("minecraft:diamond", 3.5f);

        // Farmer trades
        DEFAULT_VALUES.put("minecraft:potato", 0.038f);         // 26 potatoes = 1 emerald
        DEFAULT_VALUES.put("minecraft:carrot", 0.045f);         // 22 carrots = 1 emerald
        DEFAULT_VALUES.put("minecraft:beetroot", 0.067f);       // 15 beetroot = 1 emerald
        DEFAULT_VALUES.put("minecraft:pumpkin", 0.167f);        // 6 pumpkins = 1 emerald
        DEFAULT_VALUES.put("minecraft:melon", 0.25f);           // 4 melons = 1 emerald
        DEFAULT_VALUES.put("minecraft:bread", 0.167f);
        DEFAULT_VALUES.put("minecraft:cookie", 0.056f);
        DEFAULT_VALUES.put("minecraft:cake", 1.0f);
        DEFAULT_VALUES.put("minecraft:golden_carrot", 0.33f);
        DEFAULT_VALUES.put("minecraft:glistering_melon_slice", 0.33f);

        // Butcher trades
        DEFAULT_VALUES.put("minecraft:chicken", 0.071f);        // 14 chicken = 1 emerald
        DEFAULT_VALUES.put("minecraft:porkchop", 0.143f);       // 7 porkchop = 1 emerald
        DEFAULT_VALUES.put("minecraft:beef", 0.1f);             // 10 beef = 1 emerald
        DEFAULT_VALUES.put("minecraft:mutton", 0.143f);
        DEFAULT_VALUES.put("minecraft:rabbit", 0.25f);
        DEFAULT_VALUES.put("minecraft:cooked_porkchop", 0.2f);
        DEFAULT_VALUES.put("minecraft:cooked_chicken", 0.125f);

        // Fisherman trades
        DEFAULT_VALUES.put("minecraft:string", 0.05f);          // 20 string = 1 emerald
        DEFAULT_VALUES.put("minecraft:cod", 0.067f);            // 15 cod = 1 emerald
        DEFAULT_VALUES.put("minecraft:salmon", 0.077f);         // 13 salmon = 1 emerald
        DEFAULT_VALUES.put("minecraft:tropical_fish", 0.167f);
        DEFAULT_VALUES.put("minecraft:pufferfish", 0.25f);

        // Shepherd trades
        DEFAULT_VALUES.put("minecraft:white_wool", 0.056f);     // 18 wool = 1 emerald
        DEFAULT_VALUES.put("minecraft:white_dye", 0.083f);      // 12 dye = 1 emerald

        // Librarian trades
        DEFAULT_VALUES.put("minecraft:ink_sac", 0.2f);          // 5 ink = 1 emerald
        DEFAULT_VALUES.put("minecraft:writable_book", 1.0f);
        DEFAULT_VALUES.put("minecraft:glass", 0.1f);
        DEFAULT_VALUES.put("minecraft:lantern", 1.0f);
        DEFAULT_VALUES.put("minecraft:name_tag", 5.0f);

        // Cleric trades
        DEFAULT_VALUES.put("minecraft:rotten_flesh", 0.031f);   // 32 = 1 emerald
        DEFAULT_VALUES.put("minecraft:gold_ingot", 0.33f);      // 3 = 1 emerald
        DEFAULT_VALUES.put("minecraft:rabbit_foot", 1.0f);
        DEFAULT_VALUES.put("minecraft:redstone", 0.25f);
        DEFAULT_VALUES.put("minecraft:lapis_lazuli", 1.0f);
        DEFAULT_VALUES.put("minecraft:ender_pearl", 5.0f);
        DEFAULT_VALUES.put("minecraft:glowstone", 4.0f);
        DEFAULT_VALUES.put("minecraft:experience_bottle", 0.33f);

        // Armorer / Toolsmith / Weaponsmith
        DEFAULT_VALUES.put("minecraft:coal", 0.067f);           // 15 coal = 1 emerald
        DEFAULT_VALUES.put("minecraft:iron_ingot", 0.25f);      // 4 iron = 1 emerald
        DEFAULT_VALUES.put("minecraft:flint", 0.042f);          // 24 flint = 1 emerald

        // Armor items (approximate sell-back values)
        DEFAULT_VALUES.put("minecraft:iron_helmet", 4.0f);
        DEFAULT_VALUES.put("minecraft:iron_chestplate", 9.0f);
        DEFAULT_VALUES.put("minecraft:iron_leggings", 7.0f);
        DEFAULT_VALUES.put("minecraft:iron_boots", 4.0f);
        DEFAULT_VALUES.put("minecraft:diamond_helmet", 13.0f);
        DEFAULT_VALUES.put("minecraft:diamond_chestplate", 21.0f);
        DEFAULT_VALUES.put("minecraft:diamond_leggings", 17.0f);
        DEFAULT_VALUES.put("minecraft:diamond_boots", 13.0f);

        // Tools
        DEFAULT_VALUES.put("minecraft:iron_sword", 4.0f);
        DEFAULT_VALUES.put("minecraft:iron_axe", 6.0f);
        DEFAULT_VALUES.put("minecraft:iron_pickaxe", 5.0f);
        DEFAULT_VALUES.put("minecraft:iron_shovel", 3.0f);
        DEFAULT_VALUES.put("minecraft:diamond_sword", 12.0f);
        DEFAULT_VALUES.put("minecraft:diamond_axe", 18.0f);
        DEFAULT_VALUES.put("minecraft:diamond_pickaxe", 15.0f);
        DEFAULT_VALUES.put("minecraft:diamond_shovel", 10.0f);

        // Cartographer trades
        DEFAULT_VALUES.put("minecraft:compass", 1.0f);
        DEFAULT_VALUES.put("minecraft:filled_map", 7.0f);

        // Leatherworker trades
        DEFAULT_VALUES.put("minecraft:leather", 0.167f);        // 6 leather = 1 emerald

        // Fletcher trades
        DEFAULT_VALUES.put("minecraft:stick", 0.031f);          // 32 sticks = 1 emerald
        DEFAULT_VALUES.put("minecraft:feather", 0.042f);        // 24 feathers = 1 emerald
        DEFAULT_VALUES.put("minecraft:arrow", 0.063f);          // 16 arrows = 1 emerald
        DEFAULT_VALUES.put("minecraft:bow", 2.0f);
        DEFAULT_VALUES.put("minecraft:crossbow", 3.0f);

        // Mason trades
        DEFAULT_VALUES.put("minecraft:clay_ball", 0.1f);        // 10 clay = 1 emerald
        DEFAULT_VALUES.put("minecraft:stone", 0.05f);           // 20 stone = 1 emerald
        DEFAULT_VALUES.put("minecraft:quartz", 0.083f);         // 12 quartz = 1 emerald

        // Emerald is always 1:1
        DEFAULT_VALUES.put("minecraft:emerald", 1.0f);
        DEFAULT_VALUES.put("minecraft:emerald_block", 9.0f);

        // Enchanted books (estimated common/rare/treasure)
        DEFAULT_VALUES.put("minecraft:enchanted_book", 10.0f);  // generic average

        // Wandering Trader specials
        DEFAULT_VALUES.put("minecraft:slime_ball", 4.0f);
        DEFAULT_VALUES.put("minecraft:nautilus_shell", 5.0f);
        DEFAULT_VALUES.put("minecraft:gunpowder", 1.0f);
        DEFAULT_VALUES.put("minecraft:packed_ice", 3.0f);
        DEFAULT_VALUES.put("minecraft:blue_ice", 6.0f);
    }

    /**
     * Get the emerald value of a single item. Checks config overrides first, then defaults.
     */
    public static float getItemValue(ItemStack stack) {
        if (stack.isEmpty()) return 0f;

        String itemId = stack.getItem().builtInRegistryHolder().key().identifier().toString();

        // Check config overrides first
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (config.itemValues.containsKey(itemId)) {
            return config.itemValues.get(itemId);
        }

        // Check defaults
        if (DEFAULT_VALUES.containsKey(itemId)) {
            return DEFAULT_VALUES.get(itemId);
        }

        // Unknown item - return 0
        return 0f;
    }

    /**
     * Get the total emerald value of a stack (value per item * count).
     */
    public static float getStackValue(ItemStack stack) {
        if (stack.isEmpty()) return 0f;

        String itemId = stack.getItem().builtInRegistryHolder().key().identifier().toString();

        // Emeralds are always worth their count
        if (itemId.equals("minecraft:emerald")) {
            return stack.getCount();
        }
        if (itemId.equals("minecraft:emerald_block")) {
            return stack.getCount() * 9f;
        }

        return getItemValue(stack) * stack.getCount();
    }

    /**
     * Calculate the emerald balance for a trade.
     * Tracks actual emerald flow: emeralds gained minus emeralds spent.
     * Positive = gained emeralds, Negative = spent emeralds.
     */
    public static int calculateBalance(ItemStack input1, ItemStack input2, ItemStack output) {
        int emeraldsSpent = getEmeraldCount(input1) + getEmeraldCount(input2);
        int emeraldsGained = getEmeraldCount(output);
        return emeraldsGained - emeraldsSpent;
    }

    /**
     * Get the number of emeralds represented by this stack.
     * Counts emeralds and emerald blocks (1 block = 9 emeralds).
     */
    private static int getEmeraldCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String itemId = stack.getItem().builtInRegistryHolder().key().identifier().toString();
        if (itemId.equals("minecraft:emerald")) return stack.getCount();
        if (itemId.equals("minecraft:emerald_block")) return stack.getCount() * 9;
        return 0;
    }

    public static Map<String, Float> getDefaultValues() {
        return new HashMap<>(DEFAULT_VALUES);
    }
}
