package com.cosmere.client.render;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.metal.Metal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The shades the sighted metals show you.
 *
 * <p>Four metals all draw people who are not there, and each means something different:
 *
 * <ul>
 *   <li><b>Atium</b> -- every hostile leads a shadow of what it is about to do. This is what
 *       makes an atium burner untouchable: you are fighting the shadow, not the person.</li>
 *   <li><b>Electrum</b> -- your own futures, one shade per direction you might go.</li>
 *   <li><b>Gold</b> -- the person you might have been, standing still, wearing what they wore.</li>
 *   <li><b>Malatium</b> -- somebody else's past, shown over them.</li>
 * </ul>
 *
 * <p>Drawn as wireframe silhouettes rather than copies of the player model: a shade should read
 * as a shape at the edge of vision, not as a second entity you might mistake for real.
 */
@EventBusSubscriber(modid = Cosmere.MODID, value = Dist.CLIENT)
public final class ShadeRenderer {
    /** How far ahead of a mob its atium shadow runs, in seconds of its current motion. */
    private static final double ATIUM_LEAD = 0.9D;
    /** How far the electrum futures wander from the Allomancer. */
    private static final double ELECTRUM_SPREAD = 2.4D;
    private static final int ELECTRUM_SHADES = 5;
    private static final double SHADE_RANGE = 32.0D;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        InvestitureData data = ClientInvestitureCache.local();
        boolean atium = data.isBurning(Metal.ATIUM);
        boolean electrum = data.isBurning(Metal.ELECTRUM);
        boolean gold = data.isBurning(Metal.GOLD);
        boolean malatium = data.isBurning(Metal.MALATIUM);
        if (!atium && !electrum && !gold && !malatium) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        if (atium || malatium) {
            AABB area = player.getBoundingBox().inflate(SHADE_RANGE);
            for (Entity entity : minecraft.level.getEntities(player, area, e -> e instanceof LivingEntity)) {
                if (atium) {
                    // The shadow runs ahead along whatever the body is already doing.
                    Vec3 lead = entity.position().add(entity.getDeltaMovement().scale(20.0D * ATIUM_LEAD));
                    silhouette(consumer, pose, lead, entity.getBbWidth(), entity.getBbHeight(),
                            0.75F, 0.72F, 0.85F, 0.55F);
                }
                if (malatium) {
                    // Malatium shows who they used to be, standing where they stand.
                    silhouette(consumer, pose, entity.position().add(0.4D, 0.0D, 0.4D),
                            entity.getBbWidth(), entity.getBbHeight(), 0.6F, 0.55F, 0.45F, 0.45F);
                }
            }
        }

        if (gold) {
            silhouette(consumer, pose, player.position().add(player.getLookAngle().scale(2.0D)),
                    player.getBbWidth(), player.getBbHeight(), 1.0F, 0.85F, 0.3F, 0.6F);
        }

        if (electrum) {
            for (int i = 0; i < ELECTRUM_SHADES; i++) {
                double angle = (Math.PI * 2.0D / ELECTRUM_SHADES) * i + player.tickCount * 0.01D;
                Vec3 at = player.position().add(Math.cos(angle) * ELECTRUM_SPREAD, 0.0D, Math.sin(angle) * ELECTRUM_SPREAD);
                silhouette(consumer, pose, at, player.getBbWidth(), player.getBbHeight(),
                        0.4F, 0.7F, 1.0F, 0.45F);
            }
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    /** A crude standing figure: torso box, head box, and a line for each limb. */
    private static void silhouette(VertexConsumer consumer, PoseStack pose, Vec3 feet,
                                   float width, float height, float r, float g, float b, float a) {
        double halfWidth = width * 0.5D;
        double shoulder = feet.y + height * 0.72D;
        double hip = feet.y + height * 0.45D;

        box(consumer, pose, new AABB(
                feet.x - halfWidth, hip, feet.z - halfWidth,
                feet.x + halfWidth, shoulder, feet.z + halfWidth), r, g, b, a);
        box(consumer, pose, new AABB(
                feet.x - halfWidth * 0.6D, shoulder, feet.z - halfWidth * 0.6D,
                feet.x + halfWidth * 0.6D, feet.y + height, feet.z + halfWidth * 0.6D), r, g, b, a);
        segment(consumer, pose, new Vec3(feet.x - halfWidth * 0.6D, hip, feet.z),
                new Vec3(feet.x - halfWidth * 0.6D, feet.y, feet.z), r, g, b, a);
        segment(consumer, pose, new Vec3(feet.x + halfWidth * 0.6D, hip, feet.z),
                new Vec3(feet.x + halfWidth * 0.6D, feet.y, feet.z), r, g, b, a);
    }

    /** Twelve edges of a box, drawn as line segments. */
    private static void box(VertexConsumer consumer, PoseStack pose, AABB box, float r, float g, float b, float a) {
        Vec3[] corners = {
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ), new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ), new Vec3(box.minX, box.maxY, box.maxZ)
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            segment(consumer, pose, corners[edge[0]], corners[edge[1]], r, g, b, a);
        }
    }

    private static void segment(VertexConsumer consumer, PoseStack pose, Vec3 from, Vec3 to,
                                float r, float g, float b, float a) {
        Matrix4f matrix = pose.last().pose();
        Vec3 direction = to.subtract(from).normalize();
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal(pose.last(), (float) direction.x, (float) direction.y, (float) direction.z);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal(pose.last(), (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private ShadeRenderer() {
    }
}
