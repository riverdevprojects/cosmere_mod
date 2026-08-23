package com.cosmere.item;

import java.util.Optional;
import java.util.UUID;

import com.cosmere.metal.Metal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The contents of a metalmind: how much attribute is stored, and whose Identity is
 * imprinted on it.
 *
 * <p>A metalmind can only be tapped by the Feruchemist whose Identity it carries. An
 * <em>unkeyed</em> metalmind -- one filled while storing aluminum, which strips Identity --
 * has no owner and can be tapped by anyone.
 *
 * @param charge   stored attribute, in charge-seconds
 * @param capacity maximum charge this metalmind can hold
 * @param owner    Identity imprint, empty for an unkeyed metalmind
 */
public record MetalmindData(float charge, float capacity, Optional<UUID> owner) {
    public static final Codec<MetalmindData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("charge").forGetter(MetalmindData::charge),
            Codec.FLOAT.fieldOf("capacity").forGetter(MetalmindData::capacity),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(MetalmindData::owner)
    ).apply(inst, MetalmindData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MetalmindData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, MetalmindData::charge,
            ByteBufCodecs.FLOAT, MetalmindData::capacity,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), MetalmindData::owner,
            MetalmindData::new);

    public static MetalmindData empty(float capacity) {
        return new MetalmindData(0.0F, capacity, Optional.empty());
    }

    public boolean isUnkeyed() {
        return this.owner.isEmpty();
    }

    /** A Feruchemist may tap this metalmind if it is unkeyed or carries their own Identity. */
    public boolean canBeTappedBy(UUID player) {
        return this.owner.isEmpty() || this.owner.get().equals(player);
    }

    public boolean isFull() {
        return this.charge >= this.capacity;
    }

    public boolean isEmpty() {
        return this.charge <= 0.0F;
    }

    public float fillFraction() {
        return this.capacity <= 0.0F ? 0.0F : Math.min(1.0F, this.charge / this.capacity);
    }

    public MetalmindData withCharge(float newCharge) {
        return new MetalmindData(Math.max(0.0F, Math.min(this.capacity, newCharge)), this.capacity, this.owner);
    }

    public MetalmindData withOwner(Optional<UUID> newOwner) {
        return new MetalmindData(this.charge, this.capacity, newOwner);
    }

    /**
     * Stores {@code amount} for {@code storer}. Storing into an empty metalmind imprints the
     * storer's Identity; storing while Identity is suppressed leaves it unkeyed.
     */
    public MetalmindData store(float amount, UUID storer, boolean identitySuppressed) {
        Optional<UUID> newOwner = this.isEmpty() ? (identitySuppressed ? Optional.empty() : Optional.of(storer)) : this.owner;
        float newCharge = Math.min(this.capacity, this.charge + amount);
        return new MetalmindData(newCharge, this.capacity, newOwner);
    }

    /** Empties the metalmind's Identity once it is fully drained, so it can be re-keyed. */
    public MetalmindData tap(float amount) {
        float newCharge = Math.max(0.0F, this.charge - amount);
        return new MetalmindData(newCharge, this.capacity, newCharge <= 0.0F ? Optional.empty() : this.owner);
    }

    /** Unused by the record itself, but keeps callers from needing to know the metal is on the item. */
    public static float defaultCapacityFor(Metal metal, float base) {
        return metal.isGodMetal() ? base * 4.0F : base;
    }
}
