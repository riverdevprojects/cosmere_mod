package com.cosmere.allomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Something an Allomancer can Push on or Pull at.
 *
 * <p>Blue lines point at these, and every Push resolves against one. The distinction that
 * matters is {@link #anchored}: a bolted-down iron door moves the Allomancer, a loose coin
 * moves itself.
 */
public record MetalTarget(@Nullable BlockPos blockPos, @Nullable Entity entity, Vec3 position, boolean anchored, float weight) {
    public static MetalTarget ofBlock(BlockPos pos, boolean anchored, float weight) {
        return new MetalTarget(pos, null, Vec3.atCenterOf(pos), anchored, weight);
    }

    public static MetalTarget ofEntity(Entity entity, boolean anchored, float weight) {
        return new MetalTarget(null, entity, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), anchored, weight);
    }

    public boolean isBlock() {
        return this.blockPos != null;
    }

    public boolean isEntity() {
        return this.entity != null;
    }
}
