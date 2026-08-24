package com.cosmere.client;

import java.util.List;
import java.util.Optional;

import com.cosmere.Config;
import com.cosmere.allomancy.MetalScanner;
import com.cosmere.allomancy.MetalTarget;
import com.cosmere.util.ModTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Picking what to Push on.
 *
 * <p>Whatever is directly under the crosshair wins. Failing that -- and an Allomancer aiming at
 * a coin twenty blocks away rarely has it exactly under the crosshair -- the nearest metal to
 * the line of sight within {@link #CONE_COSINE} is chosen, which is what makes the blue lines
 * feel like aiming aids rather than decoration.
 */
public final class ClientTargeting {
    /** Cosine of the half-angle of the assist cone. 0.985 is a touch under ten degrees. */
    public static final double CONE_COSINE = 0.985D;

    /** What the client believes the player is aiming at. */
    public record Pick(int entityId, Optional<BlockPos> blockPos) {
        public static final Pick NOTHING = new Pick(-1, Optional.empty());

        public boolean isEmpty() {
            return this.entityId < 0 && this.blockPos.isEmpty();
        }
    }

    public static Pick pick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return Pick.NOTHING;
        }

        HitResult crosshair = minecraft.hitResult;
        if (crosshair instanceof EntityHitResult entityHit && entityHit.getEntity() != player
                && MetalScanner.carriesMetal(entityHit.getEntity())) {
            return new Pick(entityHit.getEntity().getId(), Optional.empty());
        }
        if (crosshair instanceof BlockHitResult blockHit && crosshair.getType() == HitResult.Type.BLOCK
                && isAllomanticBlock(minecraft.level, blockHit.getBlockPos())) {
            return new Pick(-1, Optional.of(blockHit.getBlockPos()));
        }

        MetalTarget nearest = nearestInCone(minecraft);
        if (nearest == null) {
            return Pick.NOTHING;
        }
        return nearest.isEntity()
                ? new Pick(nearest.entity().getId(), Optional.empty())
                : new Pick(-1, Optional.ofNullable(nearest.blockPos()));
    }

    /** Whether a directly-aimed-at block is actually metal, rather than any wall in front of you. */
    private static boolean isAllomanticBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModTags.Blocks.ALLOMANTIC_ANCHOR) || state.is(ModTags.Blocks.ALLOMANTICALLY_MOVABLE);
    }

    @Nullable
    private static MetalTarget nearestInCone(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<MetalTarget> targets = AllomanticLineCache.targets();

        MetalTarget best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MetalTarget target : targets) {
            Vec3 toTarget = target.position().subtract(eye);
            double distance = toTarget.length();
            if (distance < 0.1D || distance > Config.BLUE_LINE_RANGE.getAsInt()) {
                continue;
            }
            if (toTarget.scale(1.0D / distance).dot(look) < CONE_COSINE) {
                continue;
            }
            if (distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    private ClientTargeting() {
    }
}
