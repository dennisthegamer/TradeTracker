package de.dennisthegamer.tradetracker.platform;

import java.nio.file.Path;

/**
 * Loader abstraction. Implemented once per mod loader and discovered via
 * {@link java.util.ServiceLoader} (see {@link Platforms}).
 */
public interface Platform {

    /**
     * @return the loader's config directory (e.g. {@code .minecraft/config})
     */
    Path getConfigDir();
}
