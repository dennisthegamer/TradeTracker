package com.example.tradetracker;

import com.example.tradetracker.config.TradeTrackerConfig;
import com.example.tradetracker.event.TradeEventHandler;
import com.example.tradetracker.render.TradeTrackerHud;
import com.example.tradetracker.screen.TradeHistoryScreen;
import com.example.tradetracker.tracker.TradeEntry;
import com.example.tradetracker.tracker.TradeSession;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TradeTrackerClient implements ClientModInitializer {

    public static final String MOD_ID = "tradetracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));

    private static KeyMapping compactKey;
    private static KeyMapping historyKey;
    private static KeyMapping sessionToggleKey;

    private boolean wasInWorld = false;
    private boolean wasMerchantScreenOpen = false;
    private boolean wasPaused = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("TradeTracker loaded!");

        // Load config
        TradeTrackerConfig.getInstance();

        // Register keybinds
        compactKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradetracker.compact",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                CATEGORY
        ));

        historyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradetracker.history",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        sessionToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradetracker.session_toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        // Register HUD renderer
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                (graphics, deltaTracker) -> TradeTrackerHud.render(graphics, deltaTracker)
        );

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.player == null) {
            handleWorldLeave(client);
            return;
        }

        boolean inWorld = client.level != null;

        // Session lifecycle: detect world join/leave
        if (inWorld && !wasInWorld) {
            // Just joined a world - try to restore saved session
            TradeSession session = TradeSession.getInstance();
            session.reset();
            TradeTrackerConfig config = TradeTrackerConfig.getInstance();
            String keyName = sessionToggleKey.getTranslatedKeyMessage().getString();

            if (config.persistSessions && session.loadFromDisk()) {
                client.player.sendSystemMessage(
                        Component.translatable("tradetracker.session.restored", keyName)
                                .withStyle(style -> style.withColor(0x55FF55))
                );
                LOGGER.info("World joined -- restored saved session");
            } else {
                client.player.sendSystemMessage(
                        Component.translatable("tradetracker.session.press_to_start", keyName)
                                .withStyle(style -> style.withColor(0xFFD700))
                );
                LOGGER.info("World joined -- trade session ready (paused)");
            }
        } else if (!inWorld && wasInWorld) {
            handleWorldLeave(client);
        }

        wasInWorld = inWorld;

        if (!inWorld) return;

        // Pause session when ESC/Pause screen is open
        boolean pauseScreenOpen = client.screen instanceof PauseScreen;
        if (pauseScreenOpen && !wasPaused) {
            TradeSession session = TradeSession.getInstance();
            if (!session.isPaused()) {
                session.pause();
                LOGGER.info("Session paused (ESC menu)");
            }
        }
        wasPaused = pauseScreenOpen;

        // HUD effects tick
        TradeTrackerHud.tick();

        // Track MerchantScreen open/close
        boolean merchantOpen = client.screen instanceof MerchantScreen;
        if (merchantOpen && !wasMerchantScreenOpen) {
            TradeEventHandler.onMerchantScreenOpen((MerchantScreen) client.screen);
        } else if (!merchantOpen && wasMerchantScreenOpen) {
            TradeEventHandler.onMerchantScreenClose();
        }
        wasMerchantScreenOpen = merchantOpen;

        // Process keybinds
        while (compactKey.consumeClick()) {
            TradeTrackerHud.toggleCompactMode();
        }

        while (historyKey.consumeClick()) {
            client.setScreen(new TradeHistoryScreen());
        }

        while (sessionToggleKey.consumeClick()) {
            TradeSession session = TradeSession.getInstance();
            session.togglePause();
            if (session.isPaused()) {
                client.player.sendSystemMessage(
                        Component.translatable("tradetracker.session.paused")
                                .withStyle(style -> style.withColor(0xFFAA00))
                );
            } else {
                client.player.sendSystemMessage(
                        Component.translatable("tradetracker.session.started")
                                .withStyle(style -> style.withColor(0x55FF55))
                );
            }
        }
    }

    private void handleWorldLeave(Minecraft client) {
        if (!wasInWorld) return;

        TradeSession session = TradeSession.getInstance();
        session.pause();
        if (session.getTradeCount() > 0) {
            if (client.player != null) {
                sendSessionSummary(client);
            }
            if (TradeTrackerConfig.getInstance().persistSessions) {
                session.saveToDisk();
                LOGGER.info("Session saved to disk");
            }
        }
        session.reset();
        wasMerchantScreenOpen = false;
        wasInWorld = false;
        LOGGER.info("Session ended");
    }

    private void sendSessionSummary(Minecraft client) {
        if (client.player == null) return;

        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.showSessionSummary) return;

        TradeSession session = TradeSession.getInstance();

        // Header
        client.player.sendSystemMessage(Component.literal(""));
        client.player.sendSystemMessage(
                Component.translatable("tradetracker.session.summary_header")
                        .withStyle(style -> style.withColor(0xFFD700).withBold(true))
        );

        // Duration
        client.player.sendSystemMessage(
                Component.translatable("tradetracker.session.duration", session.getFormattedDuration())
                        .withStyle(style -> style.withColor(0xFFFFFF))
        );

        // Total trades
        client.player.sendSystemMessage(
                Component.translatable("tradetracker.session.total_trades", session.getTradeCount())
                        .withStyle(style -> style.withColor(0xFFFFFF))
        );

        // Emerald balance
        client.player.sendSystemMessage(
                Component.translatable("tradetracker.session.balance",
                        session.getTotalProfit(), session.getTotalLoss())
                        .withStyle(style -> style.withColor(
                                session.getNetBalance() >= 0 ? 0x55FF55 : 0xFF5555))
        );

        // Best trade
        TradeEntry best = session.getBestTrade();
        if (best != null && best.getEmeraldBalance() > 0) {
            client.player.sendSystemMessage(
                    Component.translatable("tradetracker.session.best_trade",
                            best.getOutputDescription(), best.getEmeraldBalance())
                            .withStyle(style -> style.withColor(0xFFD700))
            );
        }

        // Most traded profession
        Map.Entry<String, Integer> mostTraded = session.getMostTradedProfession();
        if (mostTraded != null) {
            client.player.sendSystemMessage(
                    Component.translatable("tradetracker.session.most_traded",
                            mostTraded.getKey(), mostTraded.getValue())
                            .withStyle(style -> style.withColor(0xAAAAAA))
            );
        }
    }
}
