package de.dennisthegamer.tradetracker;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.event.TradeEventHandler;
import de.dennisthegamer.tradetracker.render.TradeTrackerHud;
import de.dennisthegamer.tradetracker.screen.TradeTrackerTabScreen;
import de.dennisthegamer.tradetracker.tracker.TradeEntry;
import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import de.dennisthegamer.tradetracker.tracker.TradeSession;
import de.dennisthegamer.tradetracker.tracker.VillagerSightingUpdater;
import de.dennisthegamer.tradetracker.tracker.VillagerTradeStore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Shared (loader-independent) client logic. The per-loader entrypoints in the
 * {@code fabric}/{@code neoforge} subprojects call {@link #init()}, register the
 * key mappings and HUD elements, and forward the end-of-client-tick event to
 * {@link #onEndClientTick(Minecraft)}.
 */
public final class TradeTrackerClient {

    public static final String MOD_ID = "tradetracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));

    public static final KeyMapping COMPACT_KEY = new KeyMapping(
            "key.tradetracker.compact",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            CATEGORY
    );

    public static final KeyMapping SESSION_TOGGLE_KEY = new KeyMapping(
            "key.tradetracker.session_toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping VILLAGERS_KEY = new KeyMapping(
            "key.tradetracker.villagers",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private static boolean wasInWorld = false;
    private static boolean wasMerchantScreenOpen = false;
    private static boolean wasPaused = false;

    private TradeTrackerClient() {
    }

    /** Common client init — called once by each loader's entrypoint. */
    public static void init() {
        LOGGER.info("TradeTracker loaded!");

        // Load config
        TradeTrackerConfig.getInstance();
    }

    public static void onEndClientTick(Minecraft client) {
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
            VillagerTradeStore.getInstance().loadFromDisk();
            TradeMemoryStore.getInstance().loadFromDisk();
            // Must run after loadFromDisk: it rebuilds the marked cache for this world only
            TradeMemoryStore.getInstance().setWorld(worldKey(client));
            TradeTrackerConfig config = TradeTrackerConfig.getInstance();
            String keyName = SESSION_TOGGLE_KEY.getTranslatedKeyMessage().getString();

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

        // TradeMemory: refresh lastSeen/position of known villagers nearby
        VillagerSightingUpdater.tick(client);

        // Track MerchantScreen open/close
        boolean merchantOpen = client.screen instanceof MerchantScreen;
        if (merchantOpen && !wasMerchantScreenOpen) {
            TradeEventHandler.onMerchantScreenOpen((MerchantScreen) client.screen);
        } else if (!merchantOpen && wasMerchantScreenOpen) {
            TradeEventHandler.onMerchantScreenClose();
        }
        if (merchantOpen) {
            // Offers arrive from the server after the screen opens — capture them per tick
            TradeEventHandler.onMerchantScreenTick((MerchantScreen) client.screen);
        }
        wasMerchantScreenOpen = merchantOpen;

        // Process keybinds
        while (COMPACT_KEY.consumeClick()) {
            TradeTrackerHud.toggleCompactMode();
        }

        // Unified TradeTracker window (villager memory + trade history, sidebar navigation)
        while (VILLAGERS_KEY.consumeClick()) {
            client.setScreen(TradeTrackerTabScreen.openLastTab());
        }

        while (SESSION_TOGGLE_KEY.consumeClick()) {
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

    /**
     * Identity of the joined world: the save directory in singleplayer, the server address
     * in multiplayer. Empty if neither is known - records stamped with an empty id never
     * match any world, so nothing is shown rather than something wrong.
     */
    private static String worldKey(Minecraft client) {
        IntegratedServer server = client.getSingleplayerServer();
        if (server != null) {
            return "local:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }
        ServerData data = client.getCurrentServer();
        if (data != null && data.ip != null && !data.ip.isEmpty()) {
            return "server:" + data.ip;
        }
        return "";
    }

    private static void handleWorldLeave(Minecraft client) {
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
        TradeMemoryStore.getInstance().saveIfDirty();
        TradeMemoryStore.getInstance().clearWorld();
        wasMerchantScreenOpen = false;
        wasInWorld = false;
        LOGGER.info("Session ended");
    }

    private static void sendSessionSummary(Minecraft client) {
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
