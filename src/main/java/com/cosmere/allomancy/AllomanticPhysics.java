package com.cosmere.allomancy;

import com.cosmere.Config;
import com.cosmere.entity.CoinProjectileEntity;
import com.cosmere.item.CoinItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What actually happens when an Allomancer Pushes or Pulls.
 *
 * <p>The rule that makes Steelpushing feel like Steelpushing is that <em>something</em> always
 * moves. If what you Push on is heavier than you are -- a wall of iron, a bolted door -- you
 * are the one that moves. If it is lighter, it flies.
 *
 * <p>The interesting case is an ingot lying on the ground. Push along it at a shallow angle
 * and it skids away; push down into it at a steep one and the ground behind it takes the force
 * and throws you into the air. {@link #SLIDE_ANGLE_DEGREES} is where that line sits.
 */
public final class AllomanticPhysics {
    /**
     * Below this angle between the Allomancer's line of sight and the ground plane, a resting
     * ingot slides instead of anchoring. Deliberately shallow: getting height off a coin should
     * be the easy case and skidding one across a floor the deliberate one.
     */
    public static final double SLIDE_ANGLE_DEGREES = 20.0D;

    /** Impulse a single tick of an unflared Push delivers, in blocks per tick. */
    public static final double BASE_IMPULSE = 0.16D;

    /** Anything heavier than this relative to the Allomancer anchors instead of moving. */
    public static final float ANCHOR_WEIGHT = 20.0F;

    /**
     * Resolves one tick of a Push (or Pull) between {@code player} and {@code target}.
     *
     * @param strength the burn strength from {@link com.cosmere.InvestitureData#burnStrength}
     * @param pull     true for an Ironpull, false for a Steelpush
     */
    public static void apply(Player player, MetalTarget target, float strength, boolean pull) {
        Vec3 eye = player.getEyePosition();
        Vec3 toTarget = target.position().subtract(eye);
        double distance = toTarget.length();
        if (distance < 0.01D) {
            return;
        }
        Vec3 direction = toTarget.scale(1.0D / distance);

        // Force falls off with distance, but never to nothing inside the burn's range.
        double falloff = 1.0D / (1.0D + distance * 0.08D);
        double impulse = BASE_IMPULSE * strength * falloff * Config.PUSH_STRENGTH.get();

        boolean anchors = target.anchored() || target.weight() >= ANCHOR_WEIGHT;
        if (!anchors && target.isEntity()) {
            anchors = restingIngotAnchors(player, target, direction);
        }

        if (anchors) {
            // The metal wins. The Allomancer is thrown the other way.
            Vec3 push = pull ? direction : direction.reverse();
            player.push(push.x * impulse, push.y * impulse, push.z * impulse);
            player.hurtMarked = true;
            player.resetFallDistance();
            if (target.isBlock()) {
                nudgePressurePlate(player.level(), target.blockPos());
            }
        } else if (target.isEntity()) {
            Entity entity = target.entity();
            double scaled = impulse * (6.0D / Math.max(1.0F, target.weight()));
            Vec3 push = pull ? direction.reverse() : direction;
            entity.push(push.x * scaled, push.y * scaled, push.z * scaled);
            entity.hurtMarked = true;
            if (entity instanceof ItemEntity item) {
                item.setExtendedLifetime();
            }
        }

        if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 6 == 0) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.2F, pull ? 0.8F : 1.4F);
        }
    }

    /**
     * An ingot resting on the ground only slides if the Allomancer is looking along it. Steeper
     * than {@link #SLIDE_ANGLE_DEGREES} and the ground takes the force through the ingot, which
     * anchors it and launches the Allomancer instead.
     */
    private static boolean restingIngotAnchors(Player player, MetalTarget target, Vec3 direction) {
        Entity entity = target.entity();
        if (!(entity instanceof ItemEntity item) || !item.onGround()) {
            return false;
        }
        double angle = Math.toDegrees(Math.asin(Math.abs(direction.y)));
        return angle > SLIDE_ANGLE_DEGREES;
    }

    /**
     * A Steelpush on an iron pressure plate presses it, which is the whole trick behind
     * opening a door you are not standing next to.
     */
    private static void nudgePressurePlate(Level level, BlockPos pos) {
        if (pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WeightedPressurePlateBlock plate
                && state.hasProperty(WeightedPressurePlateBlock.POWER)
                && state.getValue(WeightedPressurePlateBlock.POWER) == 0) {
            level.setBlock(pos, state.setValue(WeightedPressurePlateBlock.POWER, 15), net.minecraft.world.level.block.Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, plate);
            level.updateNeighborsAt(pos.below(), plate);
            level.scheduleTick(pos, plate, 20);
            level.playSound(null, pos, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.8F);
        }
    }

    /**
     * Flicks a piece of metal out of the hand and Pushes it. This is the Coinshot's attack: the
     * coin leaves at a speed set by how hard the Push is burning.
     */
    public static boolean launchHeldMetal(Player player, ItemStack stack, float strength) {
        if (stack.isEmpty() || !MetalScanner.isMovableMetal(stack)) {
            return false;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return true;
        }
        CoinProjectileEntity coin = new CoinProjectileEntity(level, player, stack);
        float velocity = (1.6F + strength * 0.9F) * Config.PUSH_STRENGTH.get().floatValue();
        coin.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 0.4F);
        level.addFreshEntity(coin);
        stack.shrink(1);
        level.playSound(null, player.blockPosition(),
                stack.getItem() instanceof CoinItem ? SoundEvents.ITEM_PICKUP : SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 0.7F, 1.6F);
        return true;
    }

    private AllomanticPhysics() {
    }
}
