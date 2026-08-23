package com.cosmere.entity;

import java.util.UUID;

import com.cosmere.metal.Metal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A mistwraith woken by two Hemalurgic spikes: intelligent, obedient to Contract, and able to
 * wear the bones of whatever it has digested.
 *
 * <p>The metal of the spikes that made it decides its {@link Blessing}, and the Blessing is
 * the whole of what a kandra is good for. Kandra serve whoever holds their Contract -- here,
 * the player who drove in the second spike.
 */
public class KandraEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> BLESSING =
            SynchedEntityData.defineId(KandraEntity.class, EntityDataSerializers.INT);

    /**
     * The four Blessings, each granted by two spikes of one metal.
     *
     * <p>A kandra has exactly one, and it shapes everything about how the creature behaves.
     */
    public enum Blessing {
        /** Iron. Massive physical strength. */
        POTENCY("potency", Metal.IRON, 12.0D, 0.28D, 40.0D),
        /** Tin. Dexterity and speed. */
        AGILITY("agility", Metal.TIN, 5.0D, 0.42D, 24.0D),
        /** Copper. Intelligence: the Blessing that makes a kandra a spy. */
        PRESENCE("presence", Metal.COPPER, 4.0D, 0.32D, 30.0D),
        /** Zinc. Emotional intelligence. Reads and steadies those around it. */
        AWARENESS("awareness", Metal.ZINC, 4.0D, 0.32D, 30.0D);

        private final String id;
        private final Metal metal;
        private final double attackDamage;
        private final double speed;
        private final double health;

        Blessing(String id, Metal metal, double attackDamage, double speed, double health) {
            this.id = id;
            this.metal = metal;
            this.attackDamage = attackDamage;
            this.speed = speed;
            this.health = health;
        }

        public String id() {
            return this.id;
        }

        public Metal metal() {
            return this.metal;
        }

        public String translationKey() {
            return "cosmere.blessing." + this.id;
        }

        @Nullable
        public static Blessing forMetal(Metal metal) {
            for (Blessing blessing : values()) {
                if (blessing.metal == metal) {
                    return blessing;
                }
            }
            return null;
        }

        public static Blessing byOrdinal(int ordinal) {
            Blessing[] values = values();
            return values[Math.floorMod(ordinal, values.length)];
        }
    }

    public KandraEntity(EntityType<? extends KandraEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLESSING, Blessing.POTENCY.ordinal());
    }

    public Blessing blessing() {
        return Blessing.byOrdinal(this.entityData.get(BLESSING));
    }

    /** Applies the Blessing's stat profile. Called once, when the kandra is created. */
    public void setBlessing(Blessing blessing) {
        this.entityData.set(BLESSING, blessing.ordinal());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(blessing.health);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(blessing.speed);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(blessing.attackDamage);
        this.setHealth(this.getMaxHealth());
    }

    /** Replaces a spiked mistwraith with the kandra it has just become. */
    public static KandraEntity createFrom(ServerLevel level, MistwraithEntity mistwraith, Blessing blessing, Player contractHolder) {
        KandraEntity kandra = com.cosmere.registry.ModEntities.KANDRA.get().create(level);
        if (kandra == null) {
            return null;
        }
        kandra.moveTo(mistwraith.getX(), mistwraith.getY(), mistwraith.getZ(), mistwraith.getYRot(), mistwraith.getXRot());
        kandra.setBlessing(blessing);
        kandra.tame(contractHolder);
        kandra.setCustomName(Component.translatable("entity.cosmere.kandra.named", Component.translatable(blessing.translationKey())));
        level.addFreshEntity(kandra);
        mistwraith.discard();
        level.playSound(null, kandra.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        return kandra;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Blessing", this.entityData.get(BLESSING));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Blessing")) {
            this.entityData.set(BLESSING, tag.getInt("Blessing"));
        }
    }

    @Nullable
    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.BONE);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    /** Bound by Contract: a kandra will not raise a hand against the holder of its Contract. */
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        UUID owner = this.getOwnerUUID();
        if (owner != null && target.getUUID().equals(owner)) {
            return false;
        }
        return super.canAttack(target);
    }
}
