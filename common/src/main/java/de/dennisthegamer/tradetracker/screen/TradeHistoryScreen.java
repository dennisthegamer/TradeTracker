package de.dennisthegamer.tradetracker.screen;

import de.dennisthegamer.tradetracker.tracker.TradeEntry;
import de.dennisthegamer.tradetracker.tracker.TradeSession;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TradeHistoryScreen extends TradeTrackerTabScreen {

    private static final int BG_COLOR = 0xCC000000;
    private static final int HEADER_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PROFIT_COLOR = 0xFF55FF55;
    private static final int LOSS_COLOR = 0xFFFF5555;
    private static final int SELECTED_COLOR = 0x44FFFFFF;
    private static final int HOVER_COLOR = 0x22FFFFFF;
    private static final int DIVIDER_COLOR = 0x44FFFFFF;

    private final TradeSession session;
    private final List<String> professions;
    private int selectedProfessionIndex = -1; // -1 = all
    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;
    private FilterMode filterMode = FilterMode.ALL;

    // Status message for reset feedback
    private String statusMessage = null;
    private int statusMessageTicks = 0;

    public TradeHistoryScreen() {
        super(Component.translatable("tradetracker.history.title"), Tab.HISTORY);
        this.session = TradeSession.getInstance();

        // Build profession list
        this.professions = new ArrayList<>();
        this.professions.add(null); // "All" entry
        this.professions.addAll(session.getProfessionCounts().keySet());
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Font font = this.font;
        int screenW = this.width;
        int screenH = this.height;
        int contentX = contentX();
        int leftPanelX = contentX;
        int leftPanelWidth = (screenW - contentX) / 3;
        int rightPanelX = contentX + leftPanelWidth + 1;
        int rightPanelWidth = screenW - rightPanelX;
        int headerHeight = 30;
        int lineHeight = font.lineHeight + 4;
        int padding = 6;

        // Full screen dark background
        graphics.fill(0, 0, screenW, screenH, BG_COLOR);

        // === Header ===
        graphics.fill(0, 0, screenW, headerHeight, 0xDD000000);
        String title = I18n.get("tradetracker.history.title");
        graphics.text(font, title, (screenW - font.width(title)) / 2, 10, HEADER_COLOR, true);

        // Filter mode indicator at top-right
        String filterText = "[F] " + getFilterLabel();
        graphics.text(font, filterText, screenW - font.width(filterText) - padding, 10, 0xFFAAAAAA, true);

        // Reset button at top-left
        String resetText = I18n.get("tradetracker.history.reset");
        graphics.text(font, resetText, padding, 10, 0xFFFF6666, true);

        // Status message
        if (statusMessage != null && statusMessageTicks > 0) {
            graphics.text(font, statusMessage,
                    (screenW - font.width(statusMessage)) / 2, headerHeight + 2, 0xFF55FF55, true);
        }

        // === Sidebar (tab navigation) ===
        renderSidebar(graphics, mouseX, mouseY);

        // === Vertical divider ===
        graphics.fill(leftPanelX + leftPanelWidth, headerHeight, leftPanelX + leftPanelWidth + 1, screenH, DIVIDER_COLOR);

        // === Left Panel: Professions ===
        int contentStartY = headerHeight + padding + (statusMessage != null && statusMessageTicks > 0 ? font.lineHeight + 2 : 0);

        String leftHeader = I18n.get("tradetracker.history.professions");
        graphics.text(font, leftHeader, leftPanelX + padding, contentStartY, HEADER_COLOR, true);
        int profY = contentStartY + lineHeight + 2;

        Map<String, Integer> profBalances = session.getProfessionBalances();
        Map<String, Integer> profCounts = session.getProfessionCounts();

        for (int i = 0; i < professions.size(); i++) {
            int entryY = profY + (i - leftScrollOffset) * lineHeight;
            if (entryY < contentStartY + lineHeight || entryY > screenH - lineHeight) continue;

            boolean selected = (i == selectedProfessionIndex + 1);
            boolean hovered = mouseX >= leftPanelX && mouseX < leftPanelX + leftPanelWidth
                    && mouseY >= entryY && mouseY < entryY + lineHeight;

            if (selected) {
                graphics.fill(leftPanelX, entryY, leftPanelX + leftPanelWidth, entryY + lineHeight, SELECTED_COLOR);
            } else if (hovered) {
                graphics.fill(leftPanelX, entryY, leftPanelX + leftPanelWidth, entryY + lineHeight, HOVER_COLOR);
            }

            String profName;
            String balanceStr;
            int nameColor = TEXT_COLOR;

            if (professions.get(i) == null) {
                profName = I18n.get("tradetracker.history.all");
                int totalBalance = session.getNetBalance();
                balanceStr = String.format("(%d) %+d◆", session.getTradeCount(), totalBalance);
                nameColor = selected ? HEADER_COLOR : TEXT_COLOR;
            } else {
                String prof = professions.get(i);
                profName = prof;
                int count = profCounts.getOrDefault(prof, 0);
                int balance = profBalances.getOrDefault(prof, 0);
                balanceStr = String.format("(%d) %+d◆", count, balance);
            }

            graphics.text(font, profName, leftPanelX + padding, entryY + 2, nameColor, true);
            int balColor = balanceStr.contains("+") ? PROFIT_COLOR : LOSS_COLOR;
            if (balanceStr.contains("+0")) balColor = TEXT_COLOR;
            graphics.text(font, balanceStr,
                    leftPanelX + leftPanelWidth - font.width(balanceStr) - padding, entryY + 2, balColor, true);
        }

        // === Right Panel: Trade List ===
        String rightHeader = I18n.get("tradetracker.history.trades");
        graphics.text(font, rightHeader, rightPanelX + padding, contentStartY, HEADER_COLOR, true);

        List<TradeEntry> filteredTrades = getFilteredTrades();

        if (filteredTrades.isEmpty()) {
            String noTrades = I18n.get("tradetracker.history.no_trades");
            graphics.text(font, noTrades,
                    rightPanelX + (rightPanelWidth - font.width(noTrades)) / 2,
                    screenH / 2, 0xFFAAAAAA, true);
        } else {
            int tradeY = contentStartY + lineHeight + 2;
            int entryHeight = lineHeight * 2 + 2; // Two lines per entry

            for (int i = rightScrollOffset; i < filteredTrades.size(); i++) {
                int entryY = tradeY + (i - rightScrollOffset) * entryHeight;
                if (entryY + entryHeight > screenH) break;

                TradeEntry entry = filteredTrades.get(i);
                int balance = entry.getEmeraldBalance();

                // Background tint based on profit/loss
                int entryBg = balance > 0 ? 0x1A55FF55 : (balance < 0 ? 0x1AFF5555 : 0x0A888888);
                graphics.fill(rightPanelX + 2, entryY, screenW - 2, entryY + entryHeight - 1, entryBg);

                // Line 1: Profession (Level) | Balance | Timestamp
                String profLabel = entry.isWanderingTrader() ? "Wandering Trader"
                        : entry.getVillagerProfession() + " (" + entry.getLevelName() + ")";
                graphics.text(font, profLabel,
                        rightPanelX + padding, entryY + 2, 0xFFBBBBBB, true);

                String balanceLabel = String.format("%+d◆", balance);
                int balColor = balance > 0 ? PROFIT_COLOR : (balance < 0 ? LOSS_COLOR : TEXT_COLOR);
                graphics.text(font, balanceLabel,
                        screenW - font.width(balanceLabel) - padding, entryY + 2, balColor, true);

                // Line 2: Input -> Output [Time]
                String tradeDesc = entry.getInputDescription() + " → " + entry.getOutputDescription();
                String timeStr = "[" + entry.getFormattedTime() + "]";

                // Truncate trade description if needed
                int maxTradeWidth = rightPanelWidth - font.width(timeStr) - padding * 3;
                if (font.width(tradeDesc) > maxTradeWidth) {
                    while (tradeDesc.length() > 1 && font.width(tradeDesc + "...") > maxTradeWidth) {
                        tradeDesc = tradeDesc.substring(0, tradeDesc.length() - 1);
                    }
                    tradeDesc += "...";
                }

                graphics.text(font, tradeDesc,
                        rightPanelX + padding, entryY + lineHeight + 2, TEXT_COLOR, true);
                graphics.text(font, timeStr,
                        screenW - font.width(timeStr) - padding, entryY + lineHeight + 2, 0xFF888888, true);
            }
        }

        // Tick status message
        if (statusMessageTicks > 0) {
            statusMessageTicks--;
            if (statusMessageTicks == 0) statusMessage = null;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        // R = Reset Session
        if (keyCode == GLFW.GLFW_KEY_R) {
            session.deleteSavedSession();
            session.reset();
            professions.clear();
            professions.add(null);
            selectedProfessionIndex = -1;
            rightScrollOffset = 0;
            statusMessage = I18n.get("tradetracker.history.reset_confirm");
            statusMessageTicks = 60;
            return true;
        }

        // F = Cycle Filter
        if (keyCode == GLFW.GLFW_KEY_F) {
            FilterMode[] modes = FilterMode.values();
            filterMode = modes[(filterMode.ordinal() + 1) % modes.length];
            rightScrollOffset = 0;
            return true;
        }

        // Escape = close
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean consumed) {
        if (consumed) return false;

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (handleSidebarClick(event)) return true;

        if (button == 0) {
            int leftPanelX = contentX();
            int leftPanelWidth = (this.width - leftPanelX) / 3;
            int headerHeight = 30;
            int lineHeight = font.lineHeight + 4;
            int padding = 6;
            int contentStartY = headerHeight + padding
                    + (statusMessage != null && statusMessageTicks > 0 ? font.lineHeight + 2 : 0);
            int profY = contentStartY + lineHeight + 2;

            if (mouseX >= leftPanelX && mouseX < leftPanelX + leftPanelWidth && mouseY > profY) {
                int clickedIndex = (int) ((mouseY - profY) / lineHeight) + leftScrollOffset;
                if (clickedIndex >= 0 && clickedIndex < professions.size()) {
                    selectedProfessionIndex = clickedIndex - 1; // -1 because index 0 = "All"
                    rightScrollOffset = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int leftPanelEnd = contentX() + (this.width - contentX()) / 3;

        if (mouseX < leftPanelEnd) {
            // Scroll left panel
            leftScrollOffset = Math.max(0, leftScrollOffset - (int) scrollY);
            leftScrollOffset = Math.min(leftScrollOffset, Math.max(0, professions.size() - 5));
        } else {
            // Scroll right panel
            rightScrollOffset = Math.max(0, rightScrollOffset - (int) scrollY);
            List<TradeEntry> filtered = getFilteredTrades();
            rightScrollOffset = Math.min(rightScrollOffset, Math.max(0, filtered.size() - 5));
        }

        return true;
    }

    private List<TradeEntry> getFilteredTrades() {
        String selectedProf = selectedProfessionIndex >= 0 && selectedProfessionIndex < professions.size() - 1
                ? professions.get(selectedProfessionIndex + 1)
                : null;

        List<TradeEntry> trades = session.getTradesForProfession(selectedProf);

        trades = switch (filterMode) {
            case ALL -> trades;
            case PROFIT_ONLY -> trades.stream().filter(t -> t.getEmeraldBalance() > 0).toList();
            case LOSS_ONLY -> trades.stream().filter(t -> t.getEmeraldBalance() < 0).toList();
            case LAST_10 -> trades.size() > 10
                    ? trades.subList(trades.size() - 10, trades.size())
                    : trades;
        };

        return trades;
    }

    private String getFilterLabel() {
        return switch (filterMode) {
            case ALL -> I18n.get("tradetracker.history.filter.all");
            case PROFIT_ONLY -> I18n.get("tradetracker.history.filter.profit");
            case LOSS_ONLY -> I18n.get("tradetracker.history.filter.loss");
            case LAST_10 -> I18n.get("tradetracker.history.filter.last10");
        };
    }

    private enum FilterMode {
        ALL, PROFIT_ONLY, LOSS_ONLY, LAST_10
    }
}
