package de.dennisthegamer.tradetracker.hud;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import de.dennisthegamer.hudlib.ui.HudBoxProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adaptiert das TradeTracker-HUD (Größe/Skalierung/Vorschau-Zeichnung) an die hudlib-ui-Schnittstelle. */
public class TradeTrackerHudBox implements HudBoxProvider {

    @Override
    public int width() {
        return TradeTrackerHud.measureBox()[0];
    }

    @Override
    public int height() {
        return TradeTrackerHud.measureBox()[1];
    }

    @Override
    public float scale() {
        float s = TradeTrackerConfig.getInstance().hudScale;
        return s <= 0 ? 1f : s;
    }

    @Override
    public void drawSample(GuiGraphicsExtractor graphics, int x, int y, float scale) {
        TradeTrackerHud.drawPreview(graphics, x, y, scale);
    }
}
