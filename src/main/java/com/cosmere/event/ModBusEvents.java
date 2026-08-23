package com.cosmere.event;

import com.cosmere.Cosmere;
import com.cosmere.entity.KandraEntity;
import com.cosmere.entity.KolossEntity;
import com.cosmere.entity.MistwraithEntity;
import com.cosmere.entity.WolfhoundEntity;
import com.cosmere.registry.ModEntities;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Mod-bus wiring that has to happen once, at load: entity attributes and spawn rules. */
public final class ModBusEvents {
    /** Called from the mod constructor; these are mod-bus events, not game-bus ones. */
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(ModBusEvents::registerAttributes);
        modEventBus.addListener(ModBusEvents::registerSpawnPlacements);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MISTWRAITH.get(), MistwraithEntity.createAttributes().build());
        event.put(ModEntities.KANDRA.get(), KandraEntity.createAttributes().build());
        event.put(ModEntities.KOLOSS.get(), KolossEntity.createAttributes().build());
        event.put(ModEntities.WOLFHOUND.get(), WolfhoundEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Mistwraiths are produced by the mists themselves rather than the ordinary spawner,
        // but a placement rule keeps spawn eggs and structure spawns honest.
        event.register(ModEntities.MISTWRAITH.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.KOLOSS.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WOLFHOUND.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModBusEvents() {
    }
}
