package de.dennisthegamer.tradetracker.platform;

import java.util.ServiceLoader;

/**
 * Holder for the loader-specific {@link Platform} implementation,
 * resolved once via {@link ServiceLoader}.
 */
public final class Platforms {

    private static final Platform INSTANCE = ServiceLoader.load(Platform.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No TradeTracker Platform implementation on classpath"));

    private Platforms() {
    }

    public static Platform get() {
        return INSTANCE;
    }
}
