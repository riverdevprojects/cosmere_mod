package com.cosmere.entity;

import com.cosmere.registry.ModEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The wolfhounds the great houses keep: waist-high, heavy-jawed and expensive.
 *
 * <p>Mechanically a wolf, but scaled up and hitting far harder, which makes a tamed pair a
 * serious escort. They breed true.
 */
public class WolfhoundEntity extends Wolf {
    public WolfhoundEntity(EntityType<? extends WolfhoundEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.SCALE, 1.8D);
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        WolfhoundEntity pup = ModEntities.WOLFHOUND.get().create(level);
        if (pup != null && other instanceof Wolf mate) {
            pup.setOwnerUUID(this.getOwnerUUID() != null ? this.getOwnerUUID() : mate.getOwnerUUID());
            pup.setTame(pup.getOwnerUUID() != null, true);
        }
        return pup;
    }
}
