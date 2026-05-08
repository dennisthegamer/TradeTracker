package com.example.tradetracker.mixin;

import com.example.tradetracker.config.TradeTrackerConfig;
import com.example.tradetracker.render.VillagerNameplateRenderer;
import com.example.tradetracker.tracker.VillagerTradeStore;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("unused")
@Mixin(EntityRenderer.class)
public abstract class VillagerNametagMixin {

    @Inject(method = "submit", at = @At("TAIL"))
    private void tradetracker$onSubmit(EntityRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector collector, CameraRenderState camera,
                                        CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!TradeTrackerConfig.getInstance().enabled) return;
        if (!TradeTrackerConfig.getInstance().showVillagerNameplate) return;

        // Find nearby AbstractVillager matching this render state position
        double sx = state.x, sy = state.y, sz = state.z;
        AABB searchBox = new AABB(sx - 1.5, sy - 1.5, sz - 1.5, sx + 1.5, sy + 1.5, sz + 1.5);
        List<AbstractVillager> nearby = mc.level.getEntitiesOfClass(AbstractVillager.class, searchBox);
        if (nearby.isEmpty()) return;

        Vec3 statePos = new Vec3(sx, sy, sz);
        AbstractVillager closest = null;
        double closestDist = Double.MAX_VALUE;
        for (AbstractVillager v : nearby) {
            double d = v.position().distanceToSqr(statePos);
            if (d < closestDist) { closestDist = d; closest = v; }
        }
        if (closest == null) return;

        // Only render for the villager the player is looking at
        if (closest != mc.crosshairPickEntity) return;

        int count = VillagerTradeStore.getInstance().getCount(closest.getUUID());
        if (count == 0) return;

        VillagerNameplateRenderer.renderLabel(state, closest, poseStack, collector, camera, count);
    }
}
