package de.dennisthegamer.tradetracker.fabric;

import de.dennisthegamer.tradetracker.TradeTrackerClient;
import de.dennisthegamer.tradetracker.render.TrackingArrowHud;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import de.dennisthegamer.hudlib.fabric.HudLibFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/** Fabric client entrypoint — wires loader hooks to the shared client logic. */
public final class TradeTrackerFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TradeTrackerClient.init();

        // Register keybinds
        KeyMappingHelper.registerKeyMapping(TradeTrackerClient.COMPACT_KEY);
        KeyMappingHelper.registerKeyMapping(TradeTrackerClient.SESSION_TOGGLE_KEY);
        KeyMappingHelper.registerKeyMapping(TradeTrackerClient.VILLAGERS_KEY);

        // Register HUD renderer via HudLib
        HudLibFabric.register(
                Identifier.fromNamespaceAndPath(TradeTrackerClient.MOD_ID, "hud"),
                TradeTrackerHud::render
        );

        // TradeMemory: direction arrows for villagers marked for tracking
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath(TradeTrackerClient.MOD_ID, "tracking_arrows"),
                TrackingArrowHud::render
        );

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(TradeTrackerClient::onEndClientTick);
    }
}
