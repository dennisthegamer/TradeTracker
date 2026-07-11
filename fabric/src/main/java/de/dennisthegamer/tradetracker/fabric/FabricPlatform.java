package de.dennisthegamer.tradetracker.fabric;

import de.dennisthegamer.tradetracker.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric implementation of the loader abstraction (ServiceLoader). */
public final class FabricPlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
