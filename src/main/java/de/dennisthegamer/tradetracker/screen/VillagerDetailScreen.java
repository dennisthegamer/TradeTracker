package de.dennisthegamer.tradetracker.screen;

import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * TradeMemory detail view for one villager: header info, action buttons
 * (mark for tracking, custom tag, remove), known offers with cross-villager
 * price comparison, and the trade history with this villager.
 */
public class VillagerDetailScreen extends Screen {

    private static final int BG_COLOR = 0xCC000000;
    private static final int HEADER_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int MUTED_COLOR = 0xFFAAAAAA;
    private static final int MARKED_COLOR = 0xFF55FFFF;
    private static final int PRICE_COLOR = 0xFF55FF55;
    private static final int BADGE_COLOR = 0xFFFFAA00;
    private static final int BEST_COLOR = 0xFF55FF55;
    private static final int REMOVE_COLOR = 0xFFFF6666;
    private static final int BUTTON_BG = 0x66333333;
    private static final int BUTTON_BG_HOVER = 0x88555555;
    private static final int DIVIDER_COLOR = 0x44FFFFFF;

    private static final int HEADER_HEIGHT = 30;
    private static final int PADDING = 6;

    private final TradeMemoryStore store = TradeMemoryStore.getInstance();
    private final String villagerUuid;
    private final Screen parent;

    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;

    private boolean editingTag = false;
    private String tagBuffer = "";

    // Button hitboxes, recomputed every frame: {x1, y1, x2, y2}
    private final int[] markButton = new int[4];
    private final int[] tagButton = new int[4];
    private final int[] removeButton = new int[4];

    public VillagerDetailScreen(String villagerUuid, Screen parent) {
        super(Component.translatable("tradetracker.detail.title"));
        this.villagerUuid = villagerUuid;
        this.parent = parent;
    }

    private TradeMemoryStore.VillagerRecord record() {
        return store.getVillager(villagerUuid);
    }

    private int infoBlockHeight() {
        return (font.lineHeight + 4) * 4 + PADDING;
    }

    private int buttonRowY() {
        return HEADER_HEIGHT + PADDING + infoBlockHeight();
    }

    private int contentStartY() {
        return buttonRowY() + font.lineHeight + PADDING * 3 + 4;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int screenW = this.width;
        int screenH = this.height;
        int lineHeight = font.lineHeight + 4;

        graphics.fill(0, 0, screenW, screenH, BG_COLOR);

        TradeMemoryStore.VillagerRecord rec = record();
        if (rec == null) {
            // Removed while open — go back
            String gone = I18n.get("tradetracker.detail.removed");
            graphics.text(font, gone, (screenW - font.width(gone)) / 2, screenH / 2, MUTED_COLOR, true);
            return;
        }

        // === Header ===
        graphics.fill(0, 0, screenW, HEADER_HEIGHT, 0xDD000000);
        String title = I18n.get("tradetracker.detail.title");
        graphics.text(font, title, (screenW - font.width(title)) / 2, 10, HEADER_COLOR, true);
        String back = I18n.get("tradetracker.detail.back");
        graphics.text(font, back, PADDING, 10, MUTED_COLOR, true);

        // === Info block ===
        int y = HEADER_HEIGHT + PADDING;
        String profession = I18n.get("tradetracker.detail.profession", rec.profession);
        if (rec.markedForTracking) profession += "  ★";
        graphics.text(font, profession, PADDING, y, rec.markedForTracking ? MARKED_COLOR : TEXT_COLOR, true);
        String tag = I18n.get("tradetracker.detail.tag",
                rec.customTag != null ? rec.customTag : I18n.get("tradetracker.detail.no_tag"));
        graphics.text(font, tag, screenW / 2, y, TEXT_COLOR, true);
        y += lineHeight;

        graphics.text(font, I18n.get("tradetracker.detail.position",
                rec.x, rec.y, rec.z, TradeMemoryFormat.dimensionName(rec.dimension)), PADDING, y, MUTED_COLOR, true);
        String dist = TradeMemoryFormat.distanceOrDimension(rec);
        graphics.text(font, dist, screenW - font.width(dist) - PADDING, y, MUTED_COLOR, true);
        y += lineHeight;

        graphics.text(font, I18n.get("tradetracker.detail.first_seen", TradeMemoryFormat.date(rec.firstSeen)),
                PADDING, y, MUTED_COLOR, true);
        graphics.text(font, I18n.get("tradetracker.detail.last_seen", TradeMemoryFormat.timeAgo(rec.lastSeen)),
                screenW / 2, y, MUTED_COLOR, true);
        y += lineHeight;

        graphics.text(font, I18n.get("tradetracker.detail.uuid", rec.uuid.substring(0, 8) + "..."),
                PADDING, y, 0xFF666666, true);
        graphics.text(font, I18n.get("tradetracker.detail.trades", store.getTradeCountFor(rec.uuid)),
                screenW / 2, y, TEXT_COLOR, true);

        // === Buttons / tag editor ===
        int btnY = buttonRowY();
        if (editingTag) {
            String editor = I18n.get("tradetracker.detail.tag_edit", tagBuffer + "_");
            graphics.text(font, editor, PADDING, btnY + PADDING, HEADER_COLOR, true);
            String hint = I18n.get("tradetracker.detail.tag_edit_hint");
            graphics.text(font, hint, screenW - font.width(hint) - PADDING, btnY + PADDING, MUTED_COLOR, true);
            markButton[2] = markButton[0];
            tagButton[2] = tagButton[0];
            removeButton[2] = removeButton[0];
        } else {
            String markLabel = I18n.get(rec.markedForTracking
                    ? "tradetracker.detail.unmark" : "tradetracker.detail.mark");
            String tagLabel = I18n.get("tradetracker.detail.set_tag");
            String removeLabel = I18n.get("tradetracker.detail.remove");

            int x = PADDING;
            x = drawButton(graphics, font, markButton, x, btnY, markLabel,
                    rec.markedForTracking ? MARKED_COLOR : TEXT_COLOR, mouseX, mouseY);
            x = drawButton(graphics, font, tagButton, x, btnY, tagLabel, TEXT_COLOR, mouseX, mouseY);
            drawButton(graphics, font, removeButton, x, btnY, removeLabel, REMOVE_COLOR, mouseX, mouseY);
        }

        // === Content panels ===
        int contentY = contentStartY();
        int leftPanelWidth = screenW / 2;
        int rightPanelX = leftPanelWidth + 1;
        graphics.fill(0, contentY - PADDING, screenW, contentY - PADDING + 1, DIVIDER_COLOR);
        graphics.fill(leftPanelWidth, contentY - PADDING, leftPanelWidth + 1, screenH, DIVIDER_COLOR);

        renderOffers(graphics, font, rec, contentY, leftPanelWidth, screenH, lineHeight);
        renderTrades(graphics, font, rec, contentY, rightPanelX, screenW, screenH, lineHeight);
    }

    private int drawButton(GuiGraphicsExtractor graphics, Font font, int[] bounds,
                           int x, int y, String label, int color, int mouseX, int mouseY) {
        int w = font.width(label) + PADDING * 2;
        int h = font.lineHeight + PADDING * 2;
        bounds[0] = x;
        bounds[1] = y;
        bounds[2] = x + w;
        bounds[3] = y + h;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        graphics.fill(x, y, x + w, y + h, hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        graphics.text(font, label, x + PADDING, y + PADDING, color, true);
        return x + w + PADDING;
    }

    private void renderOffers(GuiGraphicsExtractor graphics, Font font, TradeMemoryStore.VillagerRecord rec,
                              int contentY, int panelWidth, int screenH, int lineHeight) {
        String header = I18n.get("tradetracker.detail.offers");
        graphics.text(font, header, PADDING, contentY, HEADER_COLOR, true);

        List<TradeMemoryStore.PriceRecord> offers = store.getOffersFor(rec.uuid);
        if (offers.isEmpty()) {
            graphics.text(font, I18n.get("tradetracker.detail.no_offers"),
                    PADDING, contentY + lineHeight + 2, MUTED_COLOR, true);
            return;
        }

        int entryHeight = lineHeight * 2 + 2;
        int listY = contentY + lineHeight + 2;
        for (int i = leftScrollOffset; i < offers.size(); i++) {
            int entryY = listY + (i - leftScrollOffset) * entryHeight;
            if (entryY + entryHeight > screenH) break;

            TradeMemoryStore.PriceRecord offer = offers.get(i);

            // Line 1: item ..... price (actual incl. demand/discount, base in parentheses);
            // buy offers show the demanded item count, where the adjustments apply
            String price;
            int priceColor;
            if (offer.sellTrade) {
                price = TradeMemoryFormat.priceLabel(offer.priceInEmeralds, offer.basePrice());
                priceColor = TradeMemoryFormat.priceColor(offer.priceInEmeralds, offer.basePrice(), PRICE_COLOR);
            } else {
                price = TradeMemoryFormat.buyPriceLabel(offer.itemCount, offer.baseItemCount, offer.priceInEmeralds);
                priceColor = TradeMemoryFormat.buyPriceColor(offer.itemCount, offer.baseItemCount, MUTED_COLOR);
            }
            String itemName = TradeMemoryFormat.truncate(font, offer.itemName,
                    panelWidth - font.width(price) - PADDING * 3);
            graphics.text(font, itemName, PADDING, entryY, TEXT_COLOR, true);
            graphics.text(font, price, panelWidth - font.width(price) - PADDING, entryY, priceColor, true);

            // Line 2: price comparison badge (sell offers) / offer direction, plus how
            // often this offer was traded with this villager
            int traded = store.getTradeCountFor(rec.uuid, offer.itemName, offer.sellTrade);
            String tradedSuffix = traded > 0
                    ? " · " + I18n.get("tradetracker.detail.times_traded", traded) : "";
            if (offer.sellTrade) {
                TradeMemoryStore.PriceRecord better =
                        store.findBetterPrice(offer.itemName, rec.uuid, offer.priceInEmeralds);
                if (better != null) {
                    TradeMemoryStore.VillagerRecord other = store.getVillager(better.villagerUuid);
                    String badge = I18n.get("tradetracker.detail.better_price",
                            better.priceInEmeralds,
                            TradeMemoryFormat.displayName(other),
                            TradeMemoryFormat.distanceOrDimension(other)) + tradedSuffix;
                    graphics.text(font, TradeMemoryFormat.truncate(font, badge, panelWidth - PADDING * 2),
                            PADDING + 8, entryY + lineHeight, BADGE_COLOR, true);
                } else {
                    graphics.text(font, I18n.get("tradetracker.detail.best_price") + tradedSuffix,
                            PADDING + 8, entryY + lineHeight, BEST_COLOR, true);
                }
            } else {
                graphics.text(font, I18n.get("tradetracker.detail.buy_offer") + tradedSuffix,
                        PADDING + 8, entryY + lineHeight, 0xFF888888, true);
            }
        }
    }

    private void renderTrades(GuiGraphicsExtractor graphics, Font font, TradeMemoryStore.VillagerRecord rec,
                              int contentY, int panelX, int screenW, int screenH, int lineHeight) {
        String header = I18n.get("tradetracker.detail.trade_history");
        graphics.text(font, header, panelX + PADDING, contentY, HEADER_COLOR, true);

        List<TradeMemoryStore.TradeRecord> trades = store.getTradesFor(rec.uuid);
        if (trades.isEmpty()) {
            graphics.text(font, I18n.get("tradetracker.detail.no_trades"),
                    panelX + PADDING, contentY + lineHeight + 2, MUTED_COLOR, true);
            return;
        }

        int entryHeight = lineHeight * 2 + 2;
        int listY = contentY + lineHeight + 2;
        int panelWidth = screenW - panelX;
        for (int i = rightScrollOffset; i < trades.size(); i++) {
            int entryY = listY + (i - rightScrollOffset) * entryHeight;
            if (entryY + entryHeight > screenH) break;

            TradeMemoryStore.TradeRecord trade = trades.get(i);

            // Line 1: direction + item ..... actually paid price (base in parentheses);
            // buy trades show the item count actually handed over
            String price;
            int priceColor;
            if (trade.priceInEmeralds <= 0) {
                price = "?";
                priceColor = MUTED_COLOR;
            } else if (trade.sellTrade) {
                price = TradeMemoryFormat.priceLabel(trade.priceInEmeralds, trade.basePrice());
                priceColor = TradeMemoryFormat.priceColor(trade.priceInEmeralds, trade.basePrice(), MUTED_COLOR);
            } else {
                price = TradeMemoryFormat.buyPriceLabel(trade.itemCount, trade.baseItemCount, trade.priceInEmeralds);
                priceColor = TradeMemoryFormat.buyPriceColor(trade.itemCount, trade.baseItemCount, PRICE_COLOR);
            }
            String direction = trade.sellTrade ? "→" : "←";
            String itemName = TradeMemoryFormat.truncate(font, direction + " " + trade.itemName,
                    panelWidth - font.width(price) - PADDING * 3);
            graphics.text(font, itemName, panelX + PADDING, entryY, TEXT_COLOR, true);
            graphics.text(font, price, screenW - font.width(price) - PADDING, entryY, priceColor, true);

            // Line 2: date
            graphics.text(font, TradeMemoryFormat.date(trade.timestamp),
                    panelX + PADDING + 8, entryY + lineHeight, 0xFF888888, true);
        }
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (editingTag && event.isAllowedChatCharacter() && tagBuffer.length() < 24) {
            tagBuffer += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        int keyCode = event.key();

        if (editingTag) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!tagBuffer.isEmpty()) tagBuffer = tagBuffer.substring(0, tagBuffer.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                store.setCustomTag(villagerUuid, tagBuffer);
                editingTag = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingTag = false;
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            goBack();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean consumed) {
        if (consumed) return false;
        if (event.button() != 0) return super.mouseClicked(event, consumed);

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        // Back (header, left)
        if (mouseY < HEADER_HEIGHT && mouseX < this.width / 4) {
            goBack();
            return true;
        }

        TradeMemoryStore.VillagerRecord rec = record();
        if (rec == null || editingTag) return super.mouseClicked(event, consumed);

        if (hit(markButton, mouseX, mouseY)) {
            store.setMarkedForTracking(villagerUuid, !rec.markedForTracking);
            return true;
        }
        if (hit(tagButton, mouseX, mouseY)) {
            editingTag = true;
            tagBuffer = rec.customTag != null ? rec.customTag : "";
            return true;
        }
        if (hit(removeButton, mouseX, mouseY)) {
            store.removeVillager(villagerUuid);
            goBack();
            return true;
        }

        return super.mouseClicked(event, consumed);
    }

    private boolean hit(int[] bounds, int mouseX, int mouseY) {
        return mouseX >= bounds[0] && mouseX < bounds[2] && mouseY >= bounds[1] && mouseY < bounds[3];
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < this.width / 2.0) {
            int size = store.getOffersFor(villagerUuid).size();
            leftScrollOffset = Math.max(0, Math.min(leftScrollOffset - (int) scrollY, Math.max(0, size - 3)));
        } else {
            int size = store.getTradesFor(villagerUuid).size();
            rightScrollOffset = Math.max(0, Math.min(rightScrollOffset - (int) scrollY, Math.max(0, size - 3)));
        }
        return true;
    }

    private void goBack() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
