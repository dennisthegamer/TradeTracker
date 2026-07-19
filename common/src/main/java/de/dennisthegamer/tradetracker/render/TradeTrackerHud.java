package de.dennisthegamer.tradetracker.render;

import de.dennisthegamer.hudlib.effect.HudEffects;
import de.dennisthegamer.hudlib.ui.HudPanel;
import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.event.TradeEventHandler;
import de.dennisthegamer.tradetracker.tracker.TradeEntry;
import de.dennisthegamer.tradetracker.tracker.TradeSession;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

public class TradeTrackerHud {

    private static boolean compactMode = false;

    private static final int PADDING = 6;
    private static final int FULL_WIDTH = 220;
    private static final int FLASH_DURATION = 15; // 0.75 Sekunden

    private static final HudEffects EFFECTS = new HudEffects(FLASH_DURATION);

    public static void toggleCompactMode() {
        compactMode = !compactMode;
    }

    public static void triggerProfitFlash() {
        EFFECTS.triggerFlash(0xFFD700); // gold
    }

    public static void triggerLossFlash() {
        EFFECTS.triggerFlash(0xFF5555); // red
    }

    public static void tick() {
        EFFECTS.tick();
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) return;

        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled) return;

        // Visibility check: only show when merchant screen is open (unless always visible)
        if (!config.hudVisibleAlways && TradeEventHandler.isMerchantScreenClosed()) return;

        TradeSession session = TradeSession.getInstance();
        if (session.getTradeCount() == 0 && TradeEventHandler.isMerchantScreenClosed()) return;

        Font font = client.font;
        int[] box = measure(font, session);
        HudPanel.draw(graphics, config.getHudPlacement(), box[0], box[1],
                config.hudScale, config.hudOpacity,
                (g, x, y) -> drawContent(g, x, y, box[0], box[1], client, session, font));
    }

    /** Breite/Höhe der aktuellen Box (für den HudBoxProvider des Editors). */
    public static int[] measureBox() {
        return measure(Minecraft.getInstance().font, TradeSession.getInstance());
    }

    /** Editor-Vorschau an expliziten (skalierten) Koordinaten. */
    public static void drawPreview(GuiGraphicsExtractor graphics, int x, int y, float scale) {
        Minecraft client = Minecraft.getInstance();
        TradeSession session = TradeSession.getInstance();
        Font font = client.font;
        int[] box = measure(font, session);
        HudPanel.drawAt(graphics, x, y, box[0], box[1], scale,
                TradeTrackerConfig.getInstance().hudOpacity,
                (g, bx, by) -> drawContent(g, bx, by, box[0], box[1], client, session, font));
    }

    /** {breite, hoehe} des jeweils aktiven Modus — identische Formeln wie der alte Renderer. */
    private static int[] measure(Font font, TradeSession session) {
        if (compactMode) {
            String text = session.getTradeCount() == 0
                    ? I18n.get("tradetracker.hud.no_trades")
                    : I18n.get("tradetracker.hud.compact", session.getNetBalance(), session.getTradeCount());
            return new int[] { font.width(text) + PADDING * 2, font.lineHeight + PADDING * 2 };
        }
        int lineHeight = font.lineHeight + 2;
        int lines = 2;
        if (session.getTradeCount() > 0) {
            lines += 4;
        }
        return new int[] { FULL_WIDTH, PADDING * 2 + lines * lineHeight };
    }

    private static void drawContent(GuiGraphicsExtractor graphics, int x, int y,
                                    int hudWidth, int hudHeight, Minecraft client,
                                    TradeSession session, Font font) {
        renderFlash(graphics, x, y, hudWidth, hudHeight);
        if (compactMode) {
            renderCompact(graphics, x, y, session, font);
        } else {
            renderFull(graphics, x, y, hudWidth, client, session, font);
        }
    }

    private static void renderFlash(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (EFFECTS.isFlashing()) {
            int flashAlpha = (int) (EFFECTS.flashAlpha() * 80);
            int color = (flashAlpha << 24) | EFFECTS.flashColor();
            graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, color);
        }
    }

    private static void renderCompact(GuiGraphicsExtractor graphics, int x, int y,
                                       TradeSession session, Font font) {
        String text;
        if (session.getTradeCount() == 0) {
            text = I18n.get("tradetracker.hud.no_trades");
        } else {
            text = I18n.get("tradetracker.hud.compact", session.getNetBalance(), session.getTradeCount());
        }
        if (session.isPaused()) text = text + " " + (char) 0x23F8; // pause glyph (no time - TradeTracker is always-on)

        int color = session.getNetBalance() >= 0 ? 0xFF55FF55 : 0xFFFF5555;
        if (session.getTradeCount() == 0) color = 0xFFFFFFFF;
        graphics.text(font, text, x + PADDING, y + PADDING, color, true);
    }

    private static void renderFull(GuiGraphicsExtractor graphics, int x, int y, int hudWidth,
                                   Minecraft client, TradeSession session, Font font) {
        int lineHeight = font.lineHeight + 2;
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
