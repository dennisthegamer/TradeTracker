package de.dennisthegamer.tradetracker.tracker;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.phys.AABB;

/**
 * Refreshes lastSeen + position of already known villagers while the player is near them.
 * Runs once per second from the client tick; only villagers already registered in the
 * TradeMemoryStore are updated.
 */
public class VillagerSightingUpdater {

    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final double SCAN_RADIUS = 64.0;

    private static int tickCounter = 0;

    public static void tick(Minecraft mc) {
        if (++tickCounter % UPDATE_INTERVAL_TICKS != 0) return;
        if (mc.player == null || mc.level == null) return;
        if (!TradeTrackerConfig.getInstance().enabled) return;

        TradeMemoryStore store = TradeMemoryStore.getInstance();
        String dimension = mc.level.dimension().identifier().toString();
        AABB searchBox = mc.player.getBoundingBox().inflate(SCAN_RADIUS);
        for (AbstractVillager villager : mc.level.getEntitiesOfClass(AbstractVillager.class, searchBox)) {
            BlockPos pos = villager.blockPosition();
            store.updateSighting(villager.getUUID(), pos.getX(), pos.getY(), pos.getZ(), dimension);
        }
    }
}
