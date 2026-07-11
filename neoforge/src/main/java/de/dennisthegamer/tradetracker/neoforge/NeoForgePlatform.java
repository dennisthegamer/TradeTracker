package de.dennisthegamer.tradetracker.neoforge;

import de.dennisthegamer.tradetracker.platform.Platform;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/** NeoForge implementation of the loader abstraction (ServiceLoader). */
public final class NeoForgePlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
