package de.dennisthegamer.tradetracker.platform;

import java.nio.file.Path;

/**
 * Loader abstraction for the few loader-API calls the mod needs.
 * Implementations are provided per loader and discovered via {@link java.util.ServiceLoader}.
 */
public interface Platform {

    /**
     * @return the loader's config directory (e.g. {@code .minecraft/config})
     */
    Path getConfigDir();
}
