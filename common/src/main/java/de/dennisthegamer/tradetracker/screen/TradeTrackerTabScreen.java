package de.dennisthegamer.tradetracker.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Base for the unified TradeTracker window: draws a navigation sidebar on the left
 * through which the user switches between the tabs (villager memory, trade history)
 * without separate keybinds. Remembers the last open tab for the running session.
 */
public abstract class TradeTrackerTabScreen extends Screen {

    public enum Tab { VILLAGERS, HISTORY }

    protected static final int SIDEBAR_WIDTH = 90;
    protected static final int HEADER_HEIGHT = 30;
    private static final int PADDING = 6;
    private static final int ENTRY_STEP = 8;

    private static Tab lastTab = Tab.VILLAGERS;

    private final Tab tab;

    protected TradeTrackerTabScreen(Component title, Tab tab) {
        super(title);
        this.tab = tab;
        lastTab = tab;
    }

    /** Screen for the tab that was open most recently (default: villager list). */
    public static Screen openLastTab() {
        return createScreen(lastTab);
    }

    private static Screen createScreen(Tab tab) {
        return tab == Tab.HISTORY ? new TradeHistoryScreen() : new VillagerListScreen();
    }

    /** Left edge of the tab content; everything right of the sidebar belongs to the tab. */
    protected int contentX() {
        return SIDEBAR_WIDTH + 1;
    }

    protected void renderSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, 0x33000000);
        graphics.fill(SIDEBAR_WIDTH, HEADER_HEIGHT, SIDEBAR_WIDTH + 1, height, 0x44FFFFFF);

        int y = HEADER_HEIGHT + PADDING;
        for (Tab t : Tab.values()) {
            int entryTop = y - 2;
            int entryBottom = y + font.lineHeight + 2;
            boolean active = t == tab;
            boolean hovered = mouseX >= 0 && mouseX < SIDEBAR_WIDTH
                    && mouseY >= entryTop && mouseY < entryBottom;
            if (active) {
                graphics.fill(0, entryTop, SIDEBAR_WIDTH, entryBottom, 0x44FFFFFF);
            } else if (hovered) {
                graphics.fill(0, entryTop, SIDEBAR_WIDTH, entryBottom, 0x22FFFFFF);
            }
            graphics.text(font, label(t), PADDING, y, active ? 0xFFFFD700 : 0xFFFFFFFF, true);
            y += font.lineHeight + ENTRY_STEP;
        }
    }

    /**
     * Sidebar click handling; subclasses call this FIRST in {@code mouseClicked} so
     * clicks left of the content are never interpreted as list clicks.
     */
    protected boolean handleSidebarClick(MouseButtonEvent event) {
        if (event.x() >= SIDEBAR_WIDTH || event.y() < HEADER_HEIGHT) return false;
        if (event.button() != 0) return true;

        int y = HEADER_HEIGHT + PADDING;
        for (Tab t : Tab.values()) {
            if (event.y() >= y - 2 && event.y() < y + font.lineHeight + 2) {
                if (t != tab) {
                    Minecraft.getInstance().setScreen(createScreen(t));
                }
                return true;
            }
            y += font.lineHeight + ENTRY_STEP;
        }
        return true;
    }

    private String label(Tab t) {
        return I18n.get(t == Tab.HISTORY
                ? "tradetracker.tab.history" : "tradetracker.tab.villagers");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
