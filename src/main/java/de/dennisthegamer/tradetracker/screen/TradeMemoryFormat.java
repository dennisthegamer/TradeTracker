package de.dennisthegamer.tradetracker.screen;

import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared formatting helpers for the TradeMemory screens. */
final class TradeMemoryFormat {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private TradeMemoryFormat() {}

    static String timeAgo(long timestamp) {
        long diffMinutes = (System.currentTimeMillis() - timestamp) / 60000L;
        if (diffMinutes < 1) return I18n.get("tradetracker.time.just_now");
        if (diffMinutes < 60) return I18n.get("tradetracker.time.minutes", diffMinutes);
        long hours = diffMinutes / 60;
        if (hours < 24) return I18n.get("tradetracker.time.hours", hours);
        return I18n.get("tradetracker.time.days", hours / 24);
    }

    static String date(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /** "minecraft:the_nether" → "The Nether" */
    static String dimensionName(String dimension) {
        if (dimension == null) return "?";
        int idx = dimension.indexOf(':');
        String path = idx >= 0 ? dimension.substring(idx + 1) : dimension;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /** Display name of a villager: custom tag if set, otherwise its profession. */
    static String displayName(TradeMemoryStore.VillagerRecord rec) {
        if (rec == null) return "?";
        return rec.customTag != null ? rec.customTag : rec.profession;
    }

    /**
     * Distance from the player to a stored villager position, e.g. "42m" — or the
     * dimension name if the villager is in a different dimension.
     */
    static String distanceOrDimension(TradeMemoryStore.VillagerRecord rec) {
        Minecraft mc = Minecraft.getInstance();
        if (rec == null || mc.player == null || mc.level == null) return "?";
        String currentDim = mc.level.dimension().identifier().toString();
        if (!currentDim.equals(rec.dimension)) return dimensionName(rec.dimension);
        double dx = rec.x + 0.5 - mc.player.getX();
        double dy = rec.y + 0.5 - mc.player.getY();
        double dz = rec.z + 0.5 - mc.player.getZ();
        return (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
    }

    static String position(TradeMemoryStore.VillagerRecord rec) {
        return rec.x + ", " + rec.y + ", " + rec.z + " · " + dimensionName(rec.dimension);
    }

    /**
     * Price label including dynamic price adjustments: shows the actual current price and,
     * if the villager raised or lowered it, the base price in parentheses —
     * e.g. "▼6◆ (7◆)" (discount) or "▲8◆ (7◆)" (markup).
     */
    static String priceLabel(int price, int basePrice) {
        if (basePrice > 0 && basePrice != price) {
            return (price < basePrice ? "▼" : "▲") + price + "◆ (" + basePrice + "◆)";
        }
        return price + "◆";
    }

    /** Green for discounted, red for raised prices, otherwise the default color. */
    static int priceColor(int price, int basePrice, int defaultColor) {
        if (basePrice > 0 && price < basePrice) return 0xFF55FF55;
        if (basePrice > 0 && price > basePrice) return 0xFFFF5555;
        return defaultColor;
    }

    /**
     * Label for a buy offer (villager pays emeralds): demand/discounts change the demanded
     * ITEM COUNT, not the payout — e.g. "×6 → 1◆", or "▲×10 (×6) → 1◆" when the villager
     * currently demands more items for the same emeralds. Falls back to the plain emerald
     * price for legacy records without counts.
     */
    static String buyPriceLabel(int itemCount, int baseItemCount, int price) {
        if (itemCount > 0 && baseItemCount > 0 && itemCount != baseItemCount) {
            return (itemCount > baseItemCount ? "▲" : "▼")
                    + "×" + itemCount + " (×" + baseItemCount + ") → " + price + "◆";
        }
        if (itemCount > 0) {
            return "×" + itemCount + " → " + price + "◆";
        }
        return price + "◆";
    }

    /** Red when the villager demands more items than base (worse), green when fewer (better). */
    static int buyPriceColor(int itemCount, int baseItemCount, int defaultColor) {
        if (itemCount > 0 && baseItemCount > 0 && itemCount > baseItemCount) return 0xFFFF5555;
        if (itemCount > 0 && baseItemCount > 0 && itemCount < baseItemCount) return 0xFF55FF55;
        return defaultColor;
    }

    static String truncate(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
}
