package de.dennisthegamer.tradetracker.mixin;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Colors the glow outline of tracked villagers with the configured glow color. */
@SuppressWarnings("unused")
@Mixin(Entity.class)
public abstract class EntityTeamColorMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void tradetracker$markedVillagerGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof AbstractVillager)) return;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled || !config.glowMarkedVillagers) return;
        if (TradeMemoryStore.getInstance().isMarked(self.getUUID())) {
            cir.setReturnValue(config.glowColor & 0xFFFFFF);
        }
    }
}
