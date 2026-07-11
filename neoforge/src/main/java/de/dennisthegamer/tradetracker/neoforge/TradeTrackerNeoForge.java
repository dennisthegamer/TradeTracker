package de.dennisthegamer.tradetracker.neoforge;

import de.dennisthegamer.tradetracker.TradeTrackerClient;
import de.dennisthegamer.tradetracker.config.TradeTrackerConfigScreen;
import de.dennisthegamer.tradetracker.render.TrackingArrowHud;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client entrypoint — wires loader hooks to the shared client logic.
 * Client-only mod, hence {@code dist = Dist.CLIENT} (no {@code @OnlyIn} in shared code).
 */
@Mod(value = TradeTrackerClient.MOD_ID, dist = Dist.CLIENT)
public final class TradeTrackerNeoForge {

    public TradeTrackerNeoForge(ModContainer container, IEventBus modBus) {
        TradeTrackerClient.init();

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onEndClientTick);

        // Config screen in the NeoForge mod list (ModMenu equivalent)
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> TradeTrackerConfigScreen.create(parent));
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TradeTrackerClient.COMPACT_KEY);
        event.register(TradeTrackerClient.SESSION_TOGGLE_KEY);
        event.register(TradeTrackerClient.VILLAGERS_KEY);
    }

    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.BOSS_OVERLAY,
                Identifier.fromNamespaceAndPath(TradeTrackerClient.MOD_ID, "hud"),
                TradeTrackerHud::render
        );
        event.registerAbove(
                VanillaGuiLayers.BOSS_OVERLAY,
                Identifier.fromNamespaceAndPath(TradeTrackerClient.MOD_ID, "tracking_arrows"),
                TrackingArrowHud::render
        );
    }

    private void onEndClientTick(ClientTickEvent.Post event) {
        TradeTrackerClient.onEndClientTick(Minecraft.getInstance());
    }
}
