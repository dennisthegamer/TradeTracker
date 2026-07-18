package de.dennisthegamer.tradetracker.config;

import de.dennisthegamer.tradetracker.hud.TradeTrackerHudBox;
import de.dennisthegamer.tradetracker.hud.TradeTrackerSlotStore;
import de.dennisthegamer.hudlib.ui.HudEditorScreen;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;

public class TradeTrackerConfigScreen {

    public static Screen create(Screen parent) {
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        TradeTrackerConfig defaults = new TradeTrackerConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.tradetracker.title"))
                .category(createGeneralCategory(config, defaults))
                .category(createHudCategory(config, defaults))
                .category(createTrackingCategory(config, defaults))
                .category(createTradeMemoryCategory(config, defaults))
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory createGeneralCategory(TradeTrackerConfig config, TradeTrackerConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.tradetracker.category.general"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.enabled"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.enabled.tooltip")))
                        .binding(defaults.enabled, () -> config.enabled, v -> config.enabled = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.show_session_summary"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.show_session_summary.tooltip")))
                        .binding(defaults.showSessionSummary, () -> config.showSessionSummary, v -> config.showSessionSummary = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.persist_sessions"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.persist_sessions.tooltip")))
                        .binding(defaults.persistSessions, () -> config.persistSessions, v -> config.persistSessions = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .build();
    }

    private static ConfigCategory createHudCategory(TradeTrackerConfig config, TradeTrackerConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.tradetracker.category.hud"))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.tradetracker.hud_edit"))
                        .description(OptionDescription.of(
                                Component.translatable("config.tradetracker.hud_edit.tooltip")))
                        .action((yaclScreen, opt) -> {
                            TradeTrackerConfig cfg = TradeTrackerConfig.getInstance();
                            Minecraft.getInstance().gui.setScreen(new HudEditorScreen(
                                    yaclScreen, new TradeTrackerHudBox(), new TradeTrackerSlotStore(),
                                    cfg::getHudPlacement, p -> { cfg.hudPlacement = p; cfg.save(); }));
                        })
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.hud_visible_always"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.hud_visible_always.tooltip")))
                        .binding(defaults.hudVisibleAlways, () -> config.hudVisibleAlways, v -> config.hudVisibleAlways = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.tradetracker.hud_opacity"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.hud_opacity.tooltip")))
                        .binding((int) (defaults.hudOpacity * 100),
                                () -> (int) (config.hudOpacity * 100),
                                v -> config.hudOpacity = v / 100f)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100)
                                .step(5))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.tradetracker.hud_scale"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.hud_scale.tooltip")))
                        .binding((int) (defaults.hudScale * 100),
                                () -> (int) (config.hudScale * 100),
                                v -> config.hudScale = v / 100f)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(50, 150)
                                .step(10))
                        .build())
                .build();
    }

    private static ConfigCategory createTrackingCategory(TradeTrackerConfig config, TradeTrackerConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.tradetracker.category.tracking"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.track_wandering_trader"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.track_wandering_trader.tooltip")))
                        .binding(defaults.trackWanderingTrader, () -> config.trackWanderingTrader, v -> config.trackWanderingTrader = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.show_villager_nameplate"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.show_villager_nameplate.tooltip")))
                        .binding(defaults.showVillagerNameplate, () -> config.showVillagerNameplate, v -> config.showVillagerNameplate = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .build();
    }

    private static ConfigCategory createTradeMemoryCategory(TradeTrackerConfig config, TradeTrackerConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("config.tradetracker.category.trade_memory"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.glow_marked_villagers"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.glow_marked_villagers.tooltip")))
                        .binding(defaults.glowMarkedVillagers, () -> config.glowMarkedVillagers, v -> config.glowMarkedVillagers = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Color>createBuilder()
                        .name(Component.translatable("config.tradetracker.glow_color"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.glow_color.tooltip")))
                        .binding(new Color(defaults.glowColor),
                                () -> new Color(config.glowColor),
                                v -> config.glowColor = v.getRGB() & 0xFFFFFF)
                        .controller(ColorControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.tradetracker.show_direction_arrow"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.show_direction_arrow.tooltip")))
                        .binding(defaults.showDirectionArrow, () -> config.showDirectionArrow, v -> config.showDirectionArrow = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<TradeTrackerConfig.ArrowPosition>createBuilder()
                        .name(Component.translatable("config.tradetracker.arrow_position"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.arrow_position.tooltip")))
                        .binding(defaults.getArrowPosition(), config::getArrowPosition, v -> config.arrowPosition = v.name())
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(TradeTrackerConfig.ArrowPosition.class)
                                .formatValue(TradeTrackerConfigScreen::positionName))
                        .build())
                .build();
    }

    /** Localized display name of a screen position enum value (shared by all position options). */
    private static Component positionName(Enum<?> value) {
        return Component.translatable("config.tradetracker.position." + value.name().toLowerCase(Locale.ROOT));
    }
}
