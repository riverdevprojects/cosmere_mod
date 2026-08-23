package com.cosmere.item;

import java.util.Optional;

import com.cosmere.hemalurgy.StolenAttribute;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a Hemalurgic spike is carrying.
 *
 * <p>A blank spike is inert metal. Once it has been driven through a victim on a Hemalurgic
 * Table it holds a {@link StolenAttribute} -- the charge decays if the spike is left out of a
 * body, which is why the Steel Ministry stored theirs in jars of blood.
 *
 * @param charge  what the spike stole, empty for a blank spike
 * @param decay   ticks of exposure since the spike last sat in blood or in a body
 */
public record SpikeData(Optional<StolenAttribute> charge, int decay) {
    public static final SpikeData BLANK = new SpikeData(Optional.empty(), 0);

    public static final Codec<SpikeData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StolenAttribute.CODEC.optionalFieldOf("charge").forGetter(SpikeData::charge),
            Codec.INT.optionalFieldOf("decay", 0).forGetter(SpikeData::decay)
    ).apply(inst, SpikeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpikeData> STREAM_CODEC = StreamCodec.composite(
            StolenAttribute.STREAM_CODEC.apply(ByteBufCodecs::optional), SpikeData::charge,
            ByteBufCodecs.VAR_INT, SpikeData::decay,
            SpikeData::new);

    public boolean isCharged() {
        return this.charge.isPresent();
    }
}
