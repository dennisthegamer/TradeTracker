package de.dennisthegamer.tradetracker.platform;

import java.util.ServiceLoader;

/**
 * Entry point to the {@link Platform} implementation of the running loader.
 */
public final class Platforms {

    private static final Platform INSTANCE = ServiceLoader.load(Platform.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "No TradeTracker Platform implementation found on the classpath"));

    private Platforms() {
    }

    public static Platform get() {
        return INSTANCE;
    }
}
