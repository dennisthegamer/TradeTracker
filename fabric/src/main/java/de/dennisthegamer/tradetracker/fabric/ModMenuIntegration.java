package de.dennisthegamer.tradetracker.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.dennisthegamer.tradetracker.config.TradeTrackerConfigScreen;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TradeTrackerConfigScreen::create;
    }
}
