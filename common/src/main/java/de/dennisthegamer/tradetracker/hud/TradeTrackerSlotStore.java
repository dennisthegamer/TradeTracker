package de.dennisthegamer.tradetracker.hud;

import de.dennisthegamer.hudlib.position.HudPreset;
import de.dennisthegamer.hudlib.ui.HudSlotStore;
import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;

import java.util.List;

/** Bildet die HUD-Preset-Slots von {@link TradeTrackerConfig} auf die hudlib-ui-Schnittstelle ab. */
public class TradeTrackerSlotStore implements HudSlotStore {

    @Override
    public List<HudPreset> slots() {
        return TradeTrackerConfig.getInstance().hudSlots;
    }

    @Override
    public void save() {
        TradeTrackerConfig.getInstance().save();
    }
}
