package com.cosmere.registry;

import java.util.List;
import java.util.function.Supplier;

import com.cosmere.Cosmere;
import com.cosmere.item.MetalmindData;
import com.cosmere.item.SpikeData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item-attached data: what a metalmind holds and what a spike is charged with. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, Cosmere.MODID);

    public static final Supplier<DataComponentType<MetalmindData>> METALMIND =
            DATA_COMPONENTS.registerComponentType("metalmind", builder -> builder
                    .persistent(MetalmindData.CODEC)
                    .networkSynchronized(MetalmindData.STREAM_CODEC));

    public static final Supplier<DataComponentType<SpikeData>> SPIKE =
            DATA_COMPONENTS.registerComponentType("spike", builder -> builder
                    .persistent(SpikeData.CODEC)
                    .networkSynchronized(SpikeData.STREAM_CODEC));

    /** Spikes held inside a Jar of Spikes, in blood, which is what keeps their charge from bleeding off. */
    public static final Supplier<DataComponentType<List<ItemStack>>> SPIKE_JAR_CONTENTS =
            DATA_COMPONENTS.registerComponentType("spike_jar_contents", builder -> builder
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list())));

    private ModDataComponents() {
    }
}
