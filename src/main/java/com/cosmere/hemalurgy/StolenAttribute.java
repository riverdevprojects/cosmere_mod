package com.cosmere.hemalurgy;

import java.util.Optional;

import com.cosmere.metal.Metal;
import com.cosmere.metal.MetalCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The charge on a Hemalurgic spike: one attribute ripped out of a victim's spiritweb.
 *
 * <p>{@link #metal} is the power's own metal where that makes sense -- a steel spike that
 * stole Physical Allomancy records <em>which</em> of the four physical metals the victim
 * could burn -- and is empty for kinds that steal something metal-agnostic like raw strength.
 *
 * @param kind      what was taken
 * @param metal     the specific power taken, for kinds that grant one
 * @param magnitude strength of the theft; Hemalurgy always loses some of what it steals
 */
public record StolenAttribute(Kind kind, Optional<Metal> metal, float magnitude) {
    public enum Kind implements StringRepresentable {
        /** Grants the ability to burn {@link #metal} Allomantically. */
        ALLOMANTIC_POWER("allomantic_power"),
        /** Grants the ability to store into {@link #metal} Feruchemically. */
        FERUCHEMIC_POWER("feruchemic_power"),
        /** Raw bodily strength: a permanent Strength buff. */
        PHYSICAL_STRENGTH("physical_strength"),
        /** Sharper human senses: permanent night vision tint and reach. */
        SENSES("senses"),
        /** Resistance to Soothing and Rioting. */
        EMOTIONAL_FORTITUDE("emotional_fortitude"),
        /** Resistance to being controlled through spikes already in the body. */
        MENTAL_FORTITUDE("mental_fortitude"),
        /** Fortune: passive Fortune/Looting. */
        FORTUNE("fortune"),
        /** Raw Investiture, which can carry powers from other magic systems. */
        INVESTITURE("investiture"),
        /** Spiritual Connection and Identity. */
        CONNECTION("connection"),
        /** Aluminum: strips the recipient instead of granting anything. */
        VOID("void"),
        /** Atium and lerasium: everything the victim had. */
        EVERYTHING("everything");

        public static final StringRepresentable.EnumCodec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public String translationKey() {
            return "cosmere.hemalurgy.kind." + this.name;
        }
    }

    public static final Codec<StolenAttribute> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Kind.CODEC.fieldOf("kind").forGetter(StolenAttribute::kind),
            Metal.CODEC.optionalFieldOf("metal").forGetter(StolenAttribute::metal),
            Codec.FLOAT.optionalFieldOf("magnitude", 1.0F).forGetter(StolenAttribute::magnitude)
    ).apply(inst, StolenAttribute::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StolenAttribute> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), StolenAttribute::kind,
            ByteBufCodecs.optional(ByteBufCodecs.idMapper(i -> Metal.values()[i], Metal::ordinal)), StolenAttribute::metal,
            ByteBufCodecs.FLOAT, StolenAttribute::magnitude,
            StolenAttribute::new);

    /**
     * What a spike of {@code spikeMetal} steals, per the Hemalurgic table. The victim decides
     * which specific power comes along; see {@link HemalurgyTransfer}.
     */
    public static Kind kindStolenBy(Metal spikeMetal) {
        return switch (spikeMetal) {
            case IRON -> Kind.PHYSICAL_STRENGTH;
            case STEEL, BRONZE, CADMIUM, ELECTRUM -> Kind.ALLOMANTIC_POWER;
            case TIN -> Kind.SENSES;
            case PEWTER, BRASS, BENDALLOY, GOLD -> Kind.FERUCHEMIC_POWER;
            case ZINC -> Kind.EMOTIONAL_FORTITUDE;
            case COPPER -> Kind.MENTAL_FORTITUDE;
            case CHROMIUM -> Kind.FORTUNE;
            case NICROSIL -> Kind.INVESTITURE;
            case ALUMINUM -> Kind.VOID;
            case DURALUMIN -> Kind.CONNECTION;
            case ATIUM, LERASIUM, MALATIUM, HARMONIUM, TRELLIUM -> Kind.EVERYTHING;
        };
    }

    /**
     * For spikes that steal a power, which quadrant of that art they reach. Returns null when
     * the spike's kind does not target a specific quadrant.
     */
    public static MetalCategory quadrantStolenBy(Metal spikeMetal) {
        return switch (spikeMetal) {
            case STEEL -> MetalCategory.PHYSICAL;       // Physical Allomancy
            case BRONZE -> MetalCategory.MENTAL;        // Mental Allomancy
            case CADMIUM -> MetalCategory.TEMPORAL;     // Temporal Allomancy
            case ELECTRUM -> MetalCategory.ENHANCEMENT; // Enhancement Allomancy
            case PEWTER -> MetalCategory.PHYSICAL;      // Physical Feruchemy
            case BRASS -> MetalCategory.MENTAL;         // Mental Feruchemy
            case BENDALLOY -> MetalCategory.SPIRITUAL;  // Spiritual Feruchemy
            case GOLD -> MetalCategory.HYBRID;          // Hybrid Feruchemy
            default -> null;
        };
    }
}
