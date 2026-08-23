package com.cosmere.entity;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Nine feet of blue skin stretched over a body that never stopped growing.
 *
 * <p>A koloss is made, not born: four iron spikes through the ribs and a koloss skin laid over
 * the body on a Hemalurgic Table. Left to itself it attacks anything it can reach. Soothed --
 * by an Allomancer burning brass, or by the player who made it -- it settles into a guard,
 * behaving much like an iron golem and defending whoever holds its leash.
 *
 * <p>The rage rises again on its own; a Soothing is a lease, not a cure.
 */
public class KolossEntity extends Monster {
    private static final EntityDataAccessor<Integer> SOOTHED_TICKS =
            SynchedEntityData.defineId(KolossEntity.class, EntityDataSerializers.INT);

    /** Ticks of calm a single Soothing buys. */
    public static final int SOOTHE_DURATION = 20 * 60;

    @Nullable
    private UUID controller;

    public KolossEntity(EntityType<? extends KolossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D)
                .add(Attributes.SCALE, 1.9D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Only hunts on its own while the rage has it.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !this.isSoothed()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, false, false,
                target -> this.isSoothed() && target instanceof Monster && !(target instanceof KolossEntity)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOOTHED_TICKS, 0);
    }

    public boolean isSoothed() {
        return this.entityData.get(SOOTHED_TICKS) > 0;
    }

    /** Calms the koloss for {@link #SOOTHE_DURATION}, and marks who it now answers to. */
    public void soothe(@Nullable Player by) {
        this.entityData.set(SOOTHED_TICKS, SOOTHE_DURATION);
        this.setTarget(null);
        if (by != null) {
            this.controller = by.getUUID();
        }
    }

    /** Rioting a koloss shortens whatever calm it had and sends it at the nearest target. */
    public void riot() {
        this.entityData.set(SOOTHED_TICKS, 0);
    }

    @Nullable
    public UUID controller() {
        return this.controller;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            int soothed = this.entityData.get(SOOTHED_TICKS);
            if (soothed > 0) {
                this.entityData.set(SOOTHED_TICKS, soothed - 1);
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.isSoothed() && this.controller != null && target.getUUID().equals(this.controller)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SoothedTicks", this.entityData.get(SOOTHED_TICKS));
        if (this.controller != null) {
            tag.putUUID("Controller", this.controller);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SOOTHED_TICKS, tag.getInt("SoothedTicks"));
        this.controller = tag.hasUUID("Controller") ? tag.getUUID("Controller") : null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
