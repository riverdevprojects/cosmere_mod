package com.cosmere.entity;

import com.cosmere.item.SpikeItem;
import com.cosmere.metal.Metal;
import com.cosmere.registry.ModEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A boneless heap of muscle and stolen skeletons that slugs around in the mists.
 *
 * <p>Mistwraiths are harmless. They eat the dead, wear their bones, and shamble. Left alone
 * they are a nuisance; driven through with two Hemalurgic spikes, one in each shoulder, they
 * wake up -- and a kandra is born.
 *
 * <p>The two spikes must be charged and must be of the same metal, which decides the
 * {@link KandraEntity.Blessing} the new kandra receives.
 */
public class MistwraithEntity extends Animal {
    private static final EntityDataAccessor<Integer> SPIKE_COUNT =
            SynchedEntityData.defineId(MistwraithEntity.class, EntityDataSerializers.INT);

    /** The metal of the first spike driven in; the second must match. */
    @Nullable
    private Metal pendingBlessingMetal;

    public MistwraithEntity(EntityType<? extends MistwraithEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.11D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIKE_COUNT, 0);
    }

    public int spikeCount() {
        return this.entityData.get(SPIKE_COUNT);
    }

    /**
     * Drives a spike into the mistwraith's shoulder. The first takes; the second, of the same
     * metal, finishes the job and replaces this entity with a kandra.
     */
    public InteractionResult receiveSpike(Player player, ItemStack stack, InteractionHand hand) {
        if (!(stack.getItem() instanceof SpikeItem spike)) {
            return InteractionResult.PASS;
        }
        if (!SpikeItem.dataOf(stack).isCharged()) {
            // A blank spike is just metal. It does nothing but annoy the creature.
            return InteractionResult.PASS;
        }
        KandraEntity.Blessing blessing = KandraEntity.Blessing.forMetal(spike.metal());
        if (blessing == null) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (this.pendingBlessingMetal != null && this.pendingBlessingMetal != spike.metal()) {
            // Mismatched metals tear the creature apart instead of waking it.
            this.hurt(this.damageSources().generic(), 12.0F);
            this.pendingBlessingMetal = null;
            this.entityData.set(SPIKE_COUNT, 0);
            return InteractionResult.CONSUME;
        }

        this.pendingBlessingMetal = spike.metal();
        this.entityData.set(SPIKE_COUNT, this.spikeCount() + 1);
        stack.shrink(1);
        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 0.6F);

        if (this.spikeCount() >= 2 && this.level() instanceof ServerLevel serverLevel) {
            KandraEntity.createFrom(serverLevel, this, blessing, player);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpikeCount", this.spikeCount());
        if (this.pendingBlessingMetal != null) {
            tag.putString("PendingBlessing", this.pendingBlessingMetal.id());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SPIKE_COUNT, tag.getInt("SpikeCount"));
        this.pendingBlessingMetal = tag.contains("PendingBlessing")
                ? Metal.byId(tag.getString("PendingBlessing")).orElse(null)
                : null;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        // Mistwraiths do not breed; they are grown from corpses, not born.
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.ROTTEN_FLESH);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    /** Mistwraiths only come out where the mists are, so daylight burns them off. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.level().isDay() && this.level().canSeeSky(this.blockPosition())
                && this.tickCount % 40 == 0) {
            this.hurt(this.damageSources().onFire(), 1.0F);
        }
    }

    public static EntityType<MistwraithEntity> type() {
        return ModEntities.MISTWRAITH.get();
    }
}
