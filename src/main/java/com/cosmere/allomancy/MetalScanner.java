package com.cosmere.allomancy;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.util.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the metal around an Allomancer.
 *
 * <p>This is what the blue lines are drawn from and what a Push or Pull resolves against.
 * Scanning a cube of blocks is not cheap, so callers cache the result and rescan on an
 * interval rather than per frame -- see {@code AllomanticLineRenderer}.
 */
public final class MetalScanner {
    /** Hard cap on results, so standing in an iron farm does not lock the game up. */
    public static final int MAX_TARGETS = 256;

    /**
     * Every piece of metal within {@code range} of {@code center}.
     *
     * @param includeEntities whether loose metal -- dropped coins, minecarts, armoured mobs --
     *                        is included alongside fixed blocks
     */
    public static List<MetalTarget> scan(Level level, Vec3 center, double range, boolean includeEntities) {
        List<MetalTarget> targets = new ArrayList<>();
        BlockPos origin = BlockPos.containing(center);
        int r = (int) Math.ceil(range);
        double rangeSq = range * range;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (targets.size() >= MAX_TARGETS) {
                        return targets;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (center.distanceToSqr(Vec3.atCenterOf(cursor)) > rangeSq) {
                        continue;
                    }
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.is(ModTags.Blocks.ALLOMANTIC_ANCHOR)) {
                        targets.add(MetalTarget.ofBlock(cursor.immutable(), true, 100.0F));
                    } else if (state.is(ModTags.Blocks.ALLOMANTICALLY_MOVABLE)) {
                        targets.add(MetalTarget.ofBlock(cursor.immutable(), true, 40.0F));
                    }
                }
            }
        }

        if (includeEntities) {
            AABB box = new AABB(center, center).inflate(range);
            for (Entity entity : level.getEntities((Entity) null, box, MetalScanner::carriesMetal)) {
                if (targets.size() >= MAX_TARGETS) {
                    break;
                }
                targets.add(MetalTarget.ofEntity(entity, false, entityMetalWeight(entity)));
            }
        }

        return targets;
    }

    /** Whether an entity has enough metal on or in it to Push against. */
    public static boolean carriesMetal(Entity entity) {
        if (entity instanceof ItemEntity item) {
            return isMovableMetal(item.getItem());
        }
        if (entity instanceof AbstractMinecart) {
            return true;
        }
        if (entity instanceof LivingEntity living) {
            for (ItemStack armour : living.getArmorSlots()) {
                if (isMovableMetal(armour)) {
                    return true;
                }
            }
            return isMovableMetal(living.getMainHandItem()) || isMovableMetal(living.getOffhandItem());
        }
        return false;
    }

    public static boolean isMovableMetal(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModTags.Items.ALLOMANTICALLY_INERT)) {
            return false;
        }
        return stack.is(ModTags.Items.ALLOMANTICALLY_MOVABLE);
    }

    /**
     * Roughly how hard something is to shift. A dropped coin flies; a minecart barely budges;
     * an armoured player throws the Allomancer instead.
     */
    public static float entityMetalWeight(Entity entity) {
        if (entity instanceof ItemEntity item) {
            return 0.4F + item.getItem().getCount() * 0.08F;
        }
        if (entity instanceof AbstractMinecart) {
            return 12.0F;
        }
        if (entity instanceof LivingEntity living) {
            float weight = 4.0F;
            for (ItemStack armour : living.getArmorSlots()) {
                if (isMovableMetal(armour)) {
                    weight += 4.0F;
                }
            }
            return weight;
        }
        return 1.0F;
    }

    private MetalScanner() {
    }
}
