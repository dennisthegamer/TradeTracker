package de.dennisthegamer.tradetracker.hud;

import de.dennisthegamer.hudlib.ui.HudBoxProvider;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Adaptiert das TradeTracker-HUD (Größe/Vorschau/Skalierung) an die hudlib-ui-Schnittstelle. */
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
        float scale = TradeTrackerConfig.getInstance().hudScale;
        return scale <= 0 ? 1f : scale;
    }

    @Override
    public void drawSample(GuiGraphicsExtractor graphics, int x, int y, float scale) {
        TradeTrackerHud.drawPreview(graphics, x, y, scale);
    }
}
