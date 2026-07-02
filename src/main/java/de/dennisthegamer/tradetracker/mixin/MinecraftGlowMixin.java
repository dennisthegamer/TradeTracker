package de.dennisthegamer.tradetracker.mixin;

import de.dennisthegamer.tradetracker.config.TradeTrackerConfig;
import de.dennisthegamer.tradetracker.tracker.TradeMemoryStore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Renders the vanilla glow outline for villagers marked for tracking in TradeMemory. */
@SuppressWarnings("unused")
@Mixin(Minecraft.class)
public class MinecraftGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void tradetracker$glowMarkedVillagers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof AbstractVillager)) return;
        TradeTrackerConfig config = TradeTrackerConfig.getInstance();
        if (!config.enabled || !config.glowMarkedVillagers) return;
        if (TradeMemoryStore.getInstance().isMarked(entity.getUUID())) {
            cir.setReturnValue(true);
        }
    }
}
