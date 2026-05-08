package com.example.tradetracker.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.phys.Vec3;

public class VillagerNameplateRenderer {

    public static void renderLabel(EntityRenderState state, AbstractVillager villager,
                                   PoseStack poseStack, SubmitNodeCollector collector,
                                   CameraRenderState camera, int count) {
        Minecraft mc = Minecraft.getInstance();

        Component text = Component.literal(I18n.get("tradetracker.nameplate.trades", count));

        int textColor = 0xFFFFFFFF;
        int backgroundColor = 0x40000000;

        Vec3 attachment = state.nameTagAttachment != null
                ? state.nameTagAttachment.add(0, 0.0, 0)
                : new Vec3(0, villager.getBbHeight() + 0.5, 0);

        int yOffset = state.nameTag != null ? 10 : 0;

        poseStack.pushPose();
        poseStack.translate(attachment.x, attachment.y + 0.1, attachment.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);

        float x = -mc.font.width(text) / 2.0F;

        collector.submitText(
                poseStack,
                x,
                (float) yOffset,
                text.getVisualOrderText(),
                false,
                Font.DisplayMode.NORMAL,
                state.lightCoords,
                textColor,
                backgroundColor,
                0
        );

        poseStack.popPose();
    }
}
