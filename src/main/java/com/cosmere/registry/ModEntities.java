package com.cosmere.registry;

import java.util.function.Supplier;

import com.cosmere.Cosmere;
import com.cosmere.entity.CoinProjectileEntity;
import com.cosmere.entity.KandraEntity;
import com.cosmere.entity.KolossEntity;
import com.cosmere.entity.MistwraithEntity;
import com.cosmere.entity.WolfhoundEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types. Attribute suppliers and spawn placements are wired up in {@code ModEvents}. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Cosmere.MODID);

    public static final Supplier<EntityType<MistwraithEntity>> MISTWRAITH = ENTITY_TYPES.register("mistwraith",
            () -> EntityType.Builder.of(MistwraithEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.6F)
                    .clientTrackingRange(10)
                    .build("mistwraith"));

    public static final Supplier<EntityType<KandraEntity>> KANDRA = ENTITY_TYPES.register("kandra",
            () -> EntityType.Builder.of(KandraEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("kandra"));

    public static final Supplier<EntityType<KolossEntity>> KOLOSS = ENTITY_TYPES.register("koloss",
            () -> EntityType.Builder.of(KolossEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.4F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("koloss"));

    public static final Supplier<EntityType<WolfhoundEntity>> WOLFHOUND = ENTITY_TYPES.register("wolfhound",
            () -> EntityType.Builder.of(WolfhoundEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.0F)
                    .clientTrackingRange(10)
                    .build("wolfhound"));

    public static final Supplier<EntityType<CoinProjectileEntity>> COIN_PROJECTILE = ENTITY_TYPES.register("coin_projectile",
            () -> EntityType.Builder.<CoinProjectileEntity>of(CoinProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(1)
                    .build("coin_projectile"));

    private ModEntities() {
    }
}
