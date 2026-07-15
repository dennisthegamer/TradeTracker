package de.dennisthegamer.tradetracker.render;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.event.TradeEventHandler;
import de.dennisthegamer.tradetracker.tracker.TradeEntry;
import de.dennisthegamer.tradetracker.tracker.TradeSession;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import org.joml.Matrix3x2fStack;

public class TradeTrackerHud {

    private static boolean compactMode = false;

    // Flash effect state
    private static int flashTicksRemaining = 0;
    private static int flashColor = 0xFFD700; // gold for profit, red for loss
    private static final int FLASH_DURATION = 15; // 0.75 seconds

    private static final int PADDING = 6;
    private static final int MARGIN = 10;

    public static void toggleCompactMode() {
        compactMode = !compactMode;
    }

    public static void triggerProfitFlash() {
        flashTicksRemaining = FLASH_DURATION;
        flashColor = 0xFFD700; // gold
    }

    public static void triggerLossFlash() {
        flashTicksRemaining = FLASH_DURATION;
        flashColor = 0xFF5555; // red
    }

    public static void tick() {
        if (flashTicksRemaining > 0) flashTicksRemaining--;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;

        // Visibility check: only show when merchant screen is open (unless always visible)
        if (!config.hudVisibleAlways && TradeEventHandler.isMerchantScreenClosed()) return;

        TradeSession session = TradeSession.getInstance();
        if (session.getTradeCount() == 0 && TradeEventHandler.isMerchantScreenClosed()) return;

        float scale = config.hudScale;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        // Adjust screen dimensions for the scaled coordinate space
        int scaledWidth = (int) (client.getWindow().getGuiScaledWidth() / scale);
        int scaledHeight = (int) (client.getWindow().getGuiScaledHeight() / scale);

        if (compactMode) {
            renderCompact(graphics, client, session, config, scaledWidth, scaledHeight);
        } else {
            renderFull(graphics, client, session, config, scaledWidth, scaledHeight);
        }

        pose.popMatrix();
    }

    private static void renderCompact(GuiGraphicsExtractor graphics, Minecraft client,
                                       TradeSession session, TradeTrackerConfig config,
                                       int screenWidth, int screenHeight) {
        Font font = client.font;

        String text;
        if (session.getTradeCount() == 0) {
            text = I18n.get("tradetracker.hud.no_trades");
        } else {
            text = I18n.get("tradetracker.hud.compact", session.getNetBalance(), session.getTradeCount());
        }
        if (session.isPaused()) text = text + " " + (char) 0x23F8; // pause glyph (no time - TradeTracker is always-on)

        int textWidth = font.width(text);
        int hudWidth = textWidth + PADDING * 2;
        int hudHeight = font.lineHeight + PADDING * 2;

        int x = getX(screenWidth, hudWidth, config);
        int y = getY(screenHeight, hudHeight, config);

        // Background
        int bgColor = ((int) (config.hudOpacity * 255) << 24);
        graphics.fill(x, y, x + hudWidth, y + hudHeight, bgColor);

        // Flash
        renderFlash(graphics, x, y, hudWidth, hudHeight);

        // Text
        int color = session.getNetBalance() >= 0 ? 0xFF55FF55 : 0xFFFF5555;
        if (session.getTradeCount() == 0) color = 0xFFFFFFFF;
        graphics.text(font, text, x + PADDING, y + PADDING, color, true);
    }

    private static void renderFull(GuiGraphicsExtractor graphics, Minecraft client,
                                    TradeSession session, TradeTrackerConfig config,
                                    int screenWidth, int screenHeight) {
        Font font = client.font;
        int lineHeight = font.lineHeight + 2;
        int hudWidth = 220;

        // Calculate content lines
        int lines = 2; // title + session trades
        if (session.getTradeCount() > 0) {
            lines += 4; // balance + best + worst + trades today
        }
        int hudHeight = PADDING * 2 + lines * lineHeight;

        int x = getX(screenWidth, hudWidth, config);
        int y = getY(screenHeight, hudHeight, config);

        // Background
        int bgColor = ((int) (config.hudOpacity * 255) << 24);
        graphics.fill(x, y, x + hudWidth, y + hudHeight, bgColor);

        // Flash
        renderFlash(graphics, x, y, hudWidth, hudHeight);

        int currentY = y + PADDING;

        // Title - bold white
        String title = I18n.get("tradetracker.hud.title")
                + (session.isPaused() ? " " + (char) 0x23F8 : ""); // pause glyph while tracking is paused
        int titleX = x + (hudWidth - font.width(title)) / 2;
        graphics.text(font, title, titleX, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        // Session trades count
        String tradesText = I18n.get("tradetracker.hud.session_trades", session.getTradeCount());
        graphics.text(font, tradesText, x + PADDING, currentY, 0xFFFFFFFF, true);
        currentY += lineHeight;

        if (session.getTradeCount() > 0) {
            // Emerald balance
            int net = session.getNetBalance();
            String balanceText;
            int balanceColor;
            if (net >= 0) {
                balanceText = I18n.get("tradetracker.hud.balance_profit", net);
                balanceColor = 0xFF55FF55;
            } else {
                balanceText = I18n.get("tradetracker.hud.balance_loss", net);
                balanceColor = 0xFFFF5555;
            }
            graphics.text(font, balanceText, x + PADDING, currentY, balanceColor, true);
            currentY += lineHeight;

            // Best trade
            TradeEntry best = session.getBestTrade();
            if (best != null && best.getEmeraldBalance() > 0) {
                String bestText = I18n.get("tradetracker.hud.best_trade",
                        best.getOutputDescription(), best.getEmeraldBalance());
                // Truncate if too long
                if (font.width(bestText) > hudWidth - PADDING * 2) {
                    bestText = truncateToFit(font, bestText, hudWidth - PADDING * 2);
                }
                graphics.text(font, bestText, x + PADDING, currentY, 0xFFFFD700, true);
            }
            currentY += lineHeight;

            // Worst trade
            TradeEntry worst = session.getWorstTrade();
            if (worst != null && worst.getEmeraldBalance() < 0) {
                String worstText = I18n.get("tradetracker.hud.worst_trade",
                        worst.getOutputDescription(), worst.getEmeraldBalance());
                if (font.width(worstText) > hudWidth - PADDING * 2) {
                    worstText = truncateToFit(font, worstText, hudWidth - PADDING * 2);
                }
                graphics.text(font, worstText, x + PADDING, currentY, 0xFFFF5555, true);
            }
            currentY += lineHeight;

            // Trades today
            if (client.level != null) {
                int currentDay = (int) (client.level.getOverworldClockTime() / 24000L) + 1;
                int todayTrades = session.getTradesForDay(currentDay);
                String todayText = I18n.get("tradetracker.hud.trades_today", todayTrades);
                graphics.text(font, todayText, x + PADDING, currentY, 0xFFFFFFFF, true);
            }
        }
    }

    private static void renderFlash(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (flashTicksRemaining > 0) {
            float alpha = (float) flashTicksRemaining / FLASH_DURATION;
            int flashAlpha = (int) (alpha * 80);
            int color = (flashAlpha << 24) | flashColor;
            graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, color);
        }
    }

    private static int getX(int screenWidth, int hudWidth, TradeTrackerConfig config) {
        return switch (config.getHudPosition()) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - hudWidth - MARGIN;
        };
    }

    private static int getY(int screenHeight, int hudHeight, TradeTrackerConfig config) {
        return switch (config.getHudPosition()) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - hudHeight - MARGIN;
        };
    }

    private static String truncateToFit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        while (text.length() > 1 && font.width(text) + ellipsisWidth > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
}
