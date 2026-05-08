package com.example.tradetracker.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TradeTrackerConfigScreen {

    public static Screen create(Screen parent) {
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        TradeTrackerConfig defaults = new TradeTrackerConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.tradetracker.title"))
                .category(createGeneralCategory(config, defaults))
                .category(createHudCategory(config, defaults))
                .category(createTrackingCategory(config, defaults))
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
                .option(Option.<TradeTrackerConfig.HudPosition>createBuilder()
                        .name(Component.translatable("config.tradetracker.hud_position"))
                        .description(OptionDescription.of(Component.translatable("config.tradetracker.hud_position.tooltip")))
                        .binding(defaults.getHudPosition(), config::getHudPosition, v -> config.hudPosition = v.name())
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(TradeTrackerConfig.HudPosition.class))
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
}
