package com.cosmere.client.render;

import java.util.List;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.allomancy.MetalTarget;
import com.cosmere.client.AllomanticLineCache;
import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.metal.Metal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The blue lines.
 *
 * <p>Burning iron or steel draws a line from the Allomancer's chest to every piece of metal in
 * range, thick and bright for the heavy anchors and thin for a coin on the floor. This is the
 * single most recognisable thing about Allomancy and it doubles as the aiming interface --
 * {@code ClientTargeting} picks from the same list the lines are drawn from.
 */
@EventBusSubscriber(modid = Cosmere.MODID, value = Dist.CLIENT)
public final class AllomanticLineRenderer {
    /** Colour of a line to something that will move. */
    private static final float[] LOOSE = {0.35F, 0.65F, 1.0F};
    /** Colour of a line to something that will move you instead. */
    private static final float[] ANCHOR = {0.15F, 0.45F, 0.95F};

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        InvestitureData data = ClientInvestitureCache.local();
        if (!data.isBurning(Metal.IRON) && !data.isBurning(Metal.STEEL)) {
            return;
        }
        List<MetalTarget> targets = AllomanticLineCache.targets();
        if (targets.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        Vec3 chest = minecraft.player.getEyePosition().subtract(0.0D, 0.35D, 0.0D);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        Matrix4f matrix = pose.last().pose();

        for (MetalTarget target : targets) {
            if (target.isEntity() && isSelf(target.entity(), minecraft)) {
                continue;
            }
            float[] colour = target.anchored() ? ANCHOR : LOOSE;
            // Near lines are drawn brighter; far ones fade so a wall of iron is not a wall of light.
            float alpha = (float) Math.max(0.15D, 1.0D - chest.distanceTo(target.position()) / 24.0D);
            line(consumer, pose, matrix, chest, target.position(), colour, alpha);
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static boolean isSelf(Entity entity, Minecraft minecraft) {
        return entity == minecraft.player;
    }

    private static void line(VertexConsumer consumer, PoseStack pose, Matrix4f matrix,
                             Vec3 from, Vec3 to, float[] colour, float alpha) {
        Vec3 direction = to.subtract(from).normalize();
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(colour[0], colour[1], colour[2], alpha)
                .setNormal(pose.last(), (float) direction.x, (float) direction.y, (float) direction.z);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(colour[0], colour[1], colour[2], alpha)
                .setNormal(pose.last(), (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private AllomanticLineRenderer() {
    }
}
