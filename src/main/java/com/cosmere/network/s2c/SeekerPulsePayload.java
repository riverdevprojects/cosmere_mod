package com.cosmere.network.s2c;

import java.util.List;

import com.cosmere.Cosmere;
import com.cosmere.metal.Metal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a Seeker hears.
 *
 * <p>Burning bronze picks up the rhythmic thumping of nearby Allomancy. The server does the
 * detection -- it is the only side that knows who is burning what -- and sends the client one
 * pulse per burning entity so the HUD can draw the Steel Alphabet glyph above them.
 *
 * @param entityId the entity being heard
 * @param metals   what they are burning
 * @param muffled  true when the target is burning copper: something is there, but blurred
 */
public record SeekerPulsePayload(int entityId, List<Metal> metals, boolean muffled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SeekerPulsePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "seeker_pulse"));

    private static final StreamCodec<ByteBuf, Metal> METAL_CODEC =
            ByteBufCodecs.idMapper(i -> Metal.values()[i], Metal::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, SeekerPulsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SeekerPulsePayload::entityId,
                    METAL_CODEC.apply(ByteBufCodecs.list()), SeekerPulsePayload::metals,
                    ByteBufCodecs.BOOL, SeekerPulsePayload::muffled,
                    SeekerPulsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
