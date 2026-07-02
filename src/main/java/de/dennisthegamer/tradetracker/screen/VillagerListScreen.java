package de.dennisthegamer.tradetracker.screen;

import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * TradeMemory main list: all known villagers (profession, position, last interaction,
 * custom tag). Typing filters by item name and switches to a cheapest-first offer list.
 */
public class VillagerListScreen extends TradeTrackerTabScreen {

    private static final int BG_COLOR = 0xCC000000;
    private static final int HEADER_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int HOVER_COLOR = 0x22FFFFFF;
    private static final int MARKED_COLOR = 0xFF55FFFF;
    private static final int PRICE_COLOR = 0xFF55FF55;

    private static final int PADDING = 6;

    private final TradeMemoryStore store = TradeMemoryStore.getInstance();

    private String search = "";
    private int scrollOffset = 0;
    private SortMode sortMode = SortMode.LAST_SEEN;

    public VillagerListScreen() {
        super(Component.translatable("tradetracker.villagers.title"), Tab.VILLAGERS);
    }

    private int entryHeight() {
        return (font.lineHeight + 4) * 2 + 2;
    }

    private int listStartY() {
        return HEADER_HEIGHT + font.lineHeight + PADDING * 2;
    }

    private List<TradeMemoryStore.VillagerRecord> sortedVillagers() {
        List<TradeMemoryStore.VillagerRecord> list = store.getAllVillagers();
        if (sortMode == SortMode.PROFESSION) {
            list.sort(Comparator.comparing((TradeMemoryStore.VillagerRecord r) -> r.profession)
                    .thenComparing(r -> -r.lastSeen));
        }
        return list;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int screenW = this.width;
        int screenH = this.height;
        int lineHeight = font.lineHeight + 4;

        graphics.fill(0, 0, screenW, screenH, BG_COLOR);

        // === Header ===
        graphics.fill(0, 0, screenW, HEADER_HEIGHT, 0xDD000000);
        String title = I18n.get("tradetracker.villagers.title");
        graphics.text(font, title, (screenW - font.width(title)) / 2, 10, HEADER_COLOR, true);

        String sortHint = I18n.get("tradetracker.villagers.sort_hint", getSortLabel());
        graphics.text(font, sortHint, screenW - font.width(sortHint) - PADDING, 10, MUTED_COLOR, true);

        // === Sidebar (tab navigation) ===
        renderSidebar(graphics, mouseX, mouseY);

        // === Search bar ===
        String searchText = I18n.get("tradetracker.villagers.search", search + "_");
        graphics.text(font, searchText, contentX() + PADDING, HEADER_HEIGHT + PADDING, TEXT_COLOR, true);

        int listY = listStartY();
        int entryHeight = entryHeight();

        if (search.isEmpty()) {
            renderVillagerList(graphics, font, screenW, screenH, listY, entryHeight, lineHeight, mouseX, mouseY);
        } else {
            renderSearchResults(graphics, font, screenW, screenH, listY, entryHeight, lineHeight, mouseX, mouseY);
        }
    }

    private void renderVillagerList(GuiGraphicsExtractor graphics, Font font, int screenW, int screenH,
                                    int listY, int entryHeight, int lineHeight, int mouseX, int mouseY) {
        List<TradeMemoryStore.VillagerRecord> villagers = sortedVillagers();
        int contentX = contentX();

        if (villagers.isEmpty()) {
            String empty = I18n.get("tradetracker.villagers.empty");
            graphics.text(font, empty, contentX + (screenW - contentX - font.width(empty)) / 2,
                    screenH / 2, MUTED_COLOR, true);
            return;
        }

        for (int i = scrollOffset; i < villagers.size(); i++) {
            int entryY = listY + (i - scrollOffset) * entryHeight;
            if (entryY + entryHeight > screenH) break;

            TradeMemoryStore.VillagerRecord rec = villagers.get(i);
            boolean hovered = mouseX >= contentX && mouseY >= entryY && mouseY < entryY + entryHeight;
            if (hovered) {
                graphics.fill(contentX, entryY, screenW, entryY + entryHeight, HOVER_COLOR);
            }
            graphics.fill(contentX + PADDING, entryY, screenW - PADDING, entryY + 1, 0x22FFFFFF);

            // Line 1: [★] Profession [Tag] ..... N Trades
            StringBuilder name = new StringBuilder();
            if (rec.markedForTracking) name.append("★ ");
            name.append(rec.profession);
            if (rec.customTag != null) name.append("  [").append(rec.customTag).append("]");
            int nameColor = rec.markedForTracking ? MARKED_COLOR : TEXT_COLOR;
            graphics.text(font, name.toString(), contentX + PADDING, entryY + 3, nameColor, true);

            String trades = I18n.get("tradetracker.villagers.trades_count", store.getTradeCountFor(rec.uuid));
            graphics.text(font, trades, screenW - font.width(trades) - PADDING, entryY + 3, MUTED_COLOR, true);

            // Line 2: Position · Dimension ..... last seen
            graphics.text(font, TradeMemoryFormat.position(rec), contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String ago = TradeMemoryFormat.timeAgo(rec.lastSeen);
            graphics.text(font, ago, screenW - font.width(ago) - PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
        }
    }

    private void renderSearchResults(GuiGraphicsExtractor graphics, Font font, int screenW, int screenH,
                                     int listY, int entryHeight, int lineHeight, int mouseX, int mouseY) {
        List<TradeMemoryStore.PriceRecord> offers = store.searchOffers(search);
        int contentX = contentX();

        if (offers.isEmpty()) {
            String empty = I18n.get("tradetracker.villagers.no_results", search);
            graphics.text(font, empty, contentX + (screenW - contentX - font.width(empty)) / 2,
                    screenH / 2, MUTED_COLOR, true);
            return;
        }

        for (int i = scrollOffset; i < offers.size(); i++) {
            int entryY = listY + (i - scrollOffset) * entryHeight;
            if (entryY + entryHeight > screenH) break;

            TradeMemoryStore.PriceRecord offer = offers.get(i);
            TradeMemoryStore.VillagerRecord rec = store.getVillager(offer.villagerUuid);

            boolean hovered = mouseX >= contentX && mouseY >= entryY && mouseY < entryY + entryHeight;
            if (hovered) {
                graphics.fill(contentX, entryY, screenW, entryY + entryHeight, HOVER_COLOR);
            }
            graphics.fill(contentX + PADDING, entryY, screenW - PADDING, entryY + 1, 0x22FFFFFF);

            // Line 1: Item ..... price (cheapest first → first entry gold)
            String price = TradeMemoryFormat.priceLabel(offer.priceInEmeralds, offer.basePrice());
            int priceColor = (i == 0) ? HEADER_COLOR
                    : TradeMemoryFormat.priceColor(offer.priceInEmeralds, offer.basePrice(), PRICE_COLOR);
            String itemName = TradeMemoryFormat.truncate(font, offer.itemName,
                    screenW - contentX - font.width(price) - PADDING * 3);
            graphics.text(font, itemName, contentX + PADDING, entryY + 3, TEXT_COLOR, true);
            graphics.text(font, price, screenW - font.width(price) - PADDING, entryY + 3, priceColor, true);

            // Line 2: Villager (tag/profession) + position ..... distance
            String who = TradeMemoryFormat.displayName(rec) + " · " + (rec != null ? TradeMemoryFormat.position(rec) : "?");
            graphics.text(font, who, contentX + PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
            String dist = TradeMemoryFormat.distanceOrDimension(rec);
            graphics.text(font, dist, screenW - font.width(dist) - PADDING, entryY + 3 + lineHeight, MUTED_COLOR, true);
        }
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (event.isAllowedChatCharacter()) {
            search += event.codepointAsString();
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        int keyCode = event.key();

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scrollOffset = 0;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            sortMode = sortMode == SortMode.LAST_SEEN ? SortMode.PROFESSION : SortMode.LAST_SEEN;
            scrollOffset = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!search.isEmpty()) {
                search = "";
                scrollOffset = 0;
            } else {
                this.onClose();
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean consumed) {
        if (consumed) return false;
        if (handleSidebarClick(event)) return true;
        if (event.button() != 0) return super.mouseClicked(event, consumed);

        int listY = listStartY();
        int entryHeight = entryHeight();
        if (event.y() >= listY && event.x() >= contentX()) {
            int index = (int) ((event.y() - listY) / entryHeight) + scrollOffset;
            String uuid = null;
            if (search.isEmpty()) {
                List<TradeMemoryStore.VillagerRecord> villagers = sortedVillagers();
                if (index >= 0 && index < villagers.size()) uuid = villagers.get(index).uuid;
            } else {
                List<TradeMemoryStore.PriceRecord> offers = store.searchOffers(search);
                if (index >= 0 && index < offers.size()) uuid = offers.get(index).villagerUuid;
            }
            if (uuid != null) {
                Minecraft.getInstance().setScreen(new VillagerDetailScreen(uuid, this));
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int size = search.isEmpty() ? sortedVillagers().size() : store.searchOffers(search).size();
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) scrollY, Math.max(0, size - 3)));
        return true;
    }

    private String getSortLabel() {
        return switch (sortMode) {
            case LAST_SEEN -> I18n.get("tradetracker.villagers.sort.last_seen");
            case PROFESSION -> I18n.get("tradetracker.villagers.sort.profession");
        };
    }

    private enum SortMode {
        LAST_SEEN, PROFESSION
    }
}
