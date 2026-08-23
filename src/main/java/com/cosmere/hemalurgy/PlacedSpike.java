package com.cosmere.hemalurgy;

import java.util.Optional;

import com.cosmere.metal.Metal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A spike that is currently in someone's body: which metal, where it sits, and what it is
 * feeding them.
 */
public record PlacedSpike(Metal metal, SpikeSlot slot, Optional<StolenAttribute> charge) {
    public static final Codec<PlacedSpike> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Metal.CODEC.fieldOf("metal").forGetter(PlacedSpike::metal),
            SpikeSlot.CODEC.fieldOf("slot").forGetter(PlacedSpike::slot),
            StolenAttribute.CODEC.optionalFieldOf("charge").forGetter(PlacedSpike::charge)
    ).apply(inst, PlacedSpike::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedSpike> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Metal.values()[i], Metal::ordinal), PlacedSpike::metal,
            ByteBufCodecs.idMapper(i -> SpikeSlot.values()[i], SpikeSlot::ordinal), PlacedSpike::slot,
            ByteBufCodecs.optional(StolenAttribute.STREAM_CODEC), PlacedSpike::charge,
            PlacedSpike::new);
}
